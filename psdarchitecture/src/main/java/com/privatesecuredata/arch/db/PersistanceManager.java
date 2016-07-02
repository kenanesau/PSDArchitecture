package com.privatesecuredata.arch.db;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import com.google.common.io.Files;
import com.privatesecuredata.arch.db.annotations.DbExtends;
import com.privatesecuredata.arch.db.annotations.DbPartialClass;
import com.privatesecuredata.arch.db.annotations.Persister;
import com.privatesecuredata.arch.db.query.Query;
import com.privatesecuredata.arch.db.query.QueryBuilder;
import com.privatesecuredata.arch.db.query.QueryCondition;
import com.privatesecuredata.arch.db.vmGlue.DBViewModelCommitListener;
import com.privatesecuredata.arch.db.vmGlue.DbListViewModelFactory;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.vm.IListViewModelFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import rx.subjects.ReplaySubject;

/**
 * 
 * @author Kenan Esau
 * 
 * The PersistanceManager is the main interface for the database-user. It provides
 * all needed functionality to load and save entity-classes which implement the
 * interface IPersistable.
 * 
 * The PersistanceManager also provides some helper-functions which makes it easier
 * to implement a Persister (e.g. assignDbId()).
 *
 */
public class PersistanceManager {

    public enum Status {
        UNINITIALIZED(1),
        INITIALIZINGPM(2),
        INITIALIZEDPM(3),
        CREATINGDB(4),
        CREATEDDB(5),
        UPGRADINGDB(6),
        OPERATIONAL(7),
        INFO(254),
        ERROR(255);

        private int value;

        private Status(int value) {
            this.value = value;
        }
    }

    public enum ActionType {
        SAVE(1),
        LOAD(2),
        DELETE(3);

        private int value;

        private ActionType(int value) {
            this.value = value;
        }

        int valueOf() {
            return value;
        }
    }

    /**
     * Interface for manipulating data before saving or after loading
     *
     * @param <T> A type which implements IPersistable
     */
    public interface Action<T extends IPersistable> {
        /**
         * Type SAVE: Called BEFORE actually saving the data
         * Type LOAD: Called AFTER loading the data from DB
         * Type DELETE: Called BEFORE actually deleting the data
         *
         * @param pm Reference to the PersistanceManager
         * @param data The data to be saved
         */
        void execute(PersistanceManager pm, T data);
        ActionType getType();
    }

    private IDbDescription dbDesc;
    private Hashtable<Class<?>, IPersister<? extends IPersistable>> persisterMap = new Hashtable<Class<?>, IPersister<? extends IPersistable>>();
    private Hashtable<String, Class<?>> classNameMap = new Hashtable<String, Class<?>>();
    private Hashtable<Pair<Class<?>, Class<?>>, ICursorLoader> cursorLoaderMap = new Hashtable<Pair<Class<?>, Class<?>>, ICursorLoader>();
    private SQLiteDatabase db;
	private boolean initializedDb = false;
	private MVVM mvvm = null;
	private Context ctx;
    private ArrayList<ICursorLoaderFactory> cursorLoaderFactories = new ArrayList<ICursorLoaderFactory>();
    private HashMap<String, QueryBuilder> queries = new HashMap<>();

    private ReplaySubject<StatusMessage> statusRelay;

    public PersistanceManager(IDbDescription dbDesc) {
        this(dbDesc, null);
    }

    public PersistanceManager(IDbDescription dbDesc, ReplaySubject<StatusMessage> statusRelay) {
        this.statusRelay = statusRelay;
        publishStatus(new StatusMessage(PersistanceManager.Status.INITIALIZINGPM));
        init(dbDesc);
    }

    private void init(IDbDescription dbDesc)
    {
        this.dbDesc = dbDesc;

        for(Class<?> classObj : dbDesc.getPersisterTypes())
            this.addPersister(classObj);

        for(Class<?> classObj : dbDesc.getPersistentTypes())
            this.addPersistentType(classObj);

        /** Count the number of references to types (needed for updates) **/
        for(Class<?> classObj : dbDesc.getPersistentTypes()) {
            IPersister persister = this.getIPersister(classObj);

            Collection<ObjectRelation> rels = persister.getDescription().getOneToOneRelations();
            for (ObjectRelation rel : rels) {
                IPersister referencedPersister = this.getIPersister(rel.getField().getType());
                referencedPersister.getDescription().increaseRefCount();
            }

            rels = persister.getDescription().getOneToManyRelations();
            for (ObjectRelation rel : rels) {
                IPersister referencedPersister = this.getIPersister(rel.getReferencedListType());
                referencedPersister.getDescription().increaseRefCount();
            }
        }

        /**
         * Work on the extends-relationships
         */
        for(Class<?> persistentType : dbDesc.getPersistentTypes()) {
            DbExtends anno = persistentType.getAnnotation(DbExtends.class);

            if (null != anno) {
                AutomaticPersister persister = (AutomaticPersister)this.getIPersister(persistentType);
                if (null == persister)
                    throw new DBException(String.format("Could not find persister for type \"%s\"", persistentType.getName()));
                AutomaticPersister parentPersister = (AutomaticPersister)this.getIPersister(anno.extendedType());
                if (null == parentPersister)
                    throw new DBException(String.format("Could not find persister for parent of extends-relationship of type \"%s\"!", anno.extendedType().getName()));
                persister.extendsPersister(parentPersister);
            }
        }

        /**
         * Register the query-Builders
         */
        for(Class<?> queryBuilderType : dbDesc.getQueryBuilderTypes()) {
            try {
                Constructor constructor = queryBuilderType.getConstructor();
                QueryBuilder queryBuilder = (QueryBuilder)constructor.newInstance();

                this.registerQuery(queryBuilder);

            } catch (Exception e) {
                String msg = String.format("unable to create or register Querybuilder of type \"%s\"",
                        queryBuilderType.getName());
                if (statusRelay == null)
                    throw new ArgumentException(msg, e);
                else {
                    Exception ex = new ArgumentException(msg, e);

                    if (statusRelay != null)
                        this.publishStatus(new StatusMessage(PersistanceManager.Status.ERROR, msg, ex));
                }
            }
        }

        publishStatus(new StatusMessage(Status.UNINITIALIZED, "PersistanceManager created"));
    }

    public IDbDescription getDbDescription() { return dbDesc; }

    public void publishStatus(StatusMessage msg) {
        if (null != statusRelay)
            statusRelay.onNext(msg);
    }

    public void publishStatus(String msg, Exception ex) {
        if ( (null != statusRelay) && (statusRelay.hasObservers())) {
            statusRelay.onNext(new StatusMessage(Status.ERROR, msg, ex));
        }
        else
            throw new DBException(msg, ex);
    }

    protected File getUpgradingDbFile() {
        String dbName = "upgrading_";
        dbName = dbName.concat(dbDesc.getName());
        File dbFile = ctx.getDatabasePath(dbName);
        return dbFile;
    }

    protected File getDbFile() {
        String dbName = dbDesc.getName();
        File dbFile = ctx.getDatabasePath(dbName);
        return dbFile;
    }

    /**
     * This method initializes the database.
     * This means:
     * - open the database
     * - create the database if it does not exist yet
     *
     * @param ctx
     * @throws DBException
     */
    public void initializeDb(Context ctx) throws DBException
    {
        initializeDb(ctx, false);
    }

    /**
	 * This method initializes the database. 
	 * This means:
	 * - open the database
	 * - create the database if it does not exist yet
	 *
	 * @param ctx
     * @param duringUpgrade if this parameter is set to true the DB-Name is manipulated to be prefixed
     *                      with "upgrading_" this prefix is removed later if the upgrade was successful
	 * @throws DBException
	 */
	public void initializeDb(Context ctx, boolean duringUpgrade) throws DBException
	{
        int version = -1;

		try {
			if (null == this.db)
			{
				this.ctx = ctx;
                File dbFile = duringUpgrade ? getUpgradingDbFile() : getDbFile();

                File dbDir = new File(dbFile.getParent());
				if (!dbDir.exists())
					dbDir.mkdir();
	
				boolean createDB = false;
	
				if (!dbFile.exists()) {
                    publishStatus(new StatusMessage(Status.CREATINGDB));
					createDB=true;
				}
	
				db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
                if (createDB) {
                    try {
                        onCreate(getDb());
                        publishStatus(new StatusMessage(Status.CREATEDDB));
                    }
                    catch (DBException ex) {
                        db.close();
                        dbFile.delete();
                        throw ex;
                    }
                }

                if (!this.initializedDb)
				{
					for(IPersister<? extends IPersistable> persister : persisterMap.values())
					{
						try {
							persister.init(this);
						}
						catch (Exception ex)
						{
	 						throw new DBException(
									String.format("Error initializing persister for type \"%s\"" ,
											persister.getDescription().getType().getName()), ex);
						}
					}
				}

                for(ICursorLoaderFactory factory : cursorLoaderFactories)
                {
                    registerCursorLoader(factory.getReferencingType(),
                            factory.getReferencedType(),
                            factory.create());
                }

                cursorLoaderFactories.clear();
				this.initializedDb = true;
			}
		}
		catch (Exception ex)
		{
            publishStatus("Error initializing Persistance-Manager", ex);
		}
	}
	
	public boolean hasInitializedDb() { return this.initializedDb; }
	
	/**
	 * When this method returns without exception, there is no dbfile existent anymore
	 * 
	 * @param ctx
	 * @param dbDesc
	 * @return true if the dbfile existed and could be deleted, false if the dbfile did no exist 
	 */
	public static boolean dropDb(Context ctx, IDbDescription dbDesc)
	{
		ContextWrapper ctxWrapper = new ContextWrapper(ctx);
		File dbFile = ctxWrapper.getDatabasePath(dbDesc.getName());
		if (dbFile.exists())
		{
			if (!dbFile.delete())
				throw new DBException(String.format("Error dropping database \"%s\"", dbFile.getAbsolutePath()));
			else
				return true;
		}
		else
			return false;
	}

    private void addPersisterToMap(Class<?> persistentType, IPersister<?> persisterObj)
    {
        persisterMap.put(persistentType, persisterObj);
        classNameMap.put(persisterObj.getDescription().getDbTypeName(), persistentType);
    }
	
	public void addPersister(Class<?> persisterClass) throws DBException 
	{
		try {
			Persister persisterAnnotation = persisterClass.getAnnotation(Persister.class);
			if (null==persisterAnnotation)
				throw new Exception("No annotation of type Persister");
				
			Constructor<?> ctor = persisterClass.getConstructor();
			IPersister<? extends IPersistable> persisterObj = (IPersister<? extends IPersistable>) ctor.newInstance();

            addPersisterToMap(persisterAnnotation.persists(), persisterObj);
		}
		catch (Exception ex)
		{
			throw new DBException("Error adding Persister!", ex);			
		}
	}
	
	/**
	 * Create a Persister for a persistable type and register it at the Persistance-Manager
	 * 
	 * @param persistentType
	 */
	public void addPersistentType(Class<?> persistentType) {
		try {
			IPersister<? extends IPersistable> persisterObj = new AutomaticPersister(this, persistentType);
					
			addPersisterToMap(persistentType, persisterObj);
		}
        catch (NoSuchMethodException ex)
        {
            throw new DBException(String.format("Error, missing contstructor in persister of type \"%s\"!", persistentType.getName()), ex);
        }
		catch (Exception ex)
		{
			throw new DBException(String.format("Error adding common Persister for type '%s'!", (null != persistentType) ? persistentType.getName() : "NULL"), ex);
		}
	}

    public Class<?> getPersistentType(String className)
    {
        return classNameMap.get(className);
    }

    public IPersister getIPersister(Class classObj)
    {
        IPersister persister = persisterMap.get(classObj);
        if(null == persister) {
            Class newType = getPersistentType(DbNameHelper.getDbTypeName(classObj));
            if (null != newType)
                persister = persisterMap.get(newType);
        }

        return persister;
    }

    public IPersister<?> getUnspecificPersister(Class<?> classObj)
    {
        IPersister<?> persister = persisterMap.get(classObj);
        if(null == persister) {
            Class newType = getPersistentType(DbNameHelper.getDbTypeName(classObj));
            if (null != newType)
                persister = persisterMap.get(newType);
        }

        return persister;
    }

	public <T extends IPersistable> IPersister<T> getPersister(Class<T> classObj)
	{
		IPersister<T> persister = (IPersister<T>)persisterMap.get(classObj);
        if(null == persister) {
            Class newType = getPersistentType(DbNameHelper.getDbTypeName(classObj));
            if (null != newType)
                persister = (IPersister<T>)persisterMap.get(newType);
        }

        return persister;
	}

    public <T extends IPersistable> IPersister<T> getPersister(IPersistable persistable)
	{
		return (IPersister<T>)getPersister(persistable.getClass());
	}
	
	protected static PersistanceManager instance = null;
	
	public void onCreate(SQLiteDatabase db) {
		db.setVersion(dbDesc.getVersion());
		db.beginTransaction();
		try {
			for (String createSQL : dbDesc.getCreateStatements())
			{
				db.execSQL(createSQL);
			}
			
			for (Class persistentType : dbDesc.getPersistentTypes())
			{
				AutomaticPersister<?> persister = (AutomaticPersister)this.getPersister(persistentType);
				String createSQL = persister.getCreateStatement();
                if (null != createSQL) {
                    db.execSQL(createSQL);
                }
			}
			db.setTransactionSuccessful();
		}
        catch (Exception ex) {

            throw new DBException("Error creating DB", ex);
        }
		finally
		{
			db.endTransaction();
		}
	}

	public void onUpgrade(Context ctx, int oldVersion, int newVersion, HashMap<Integer, HashMap<Integer, String>> dbNames) throws DBException {
        if (!hasInitializedDb())
            initializeDb(ctx, true);

        IDbHistoryDescription history = getDbDescription().getDbHistory();
        PersistanceManager oldPm = null;

        if (dbNames.containsKey(oldVersion))
        {
            IDbDescription oldDescription = null;

            for(int instance : dbNames.get(oldVersion).keySet()) {
                try {
                    oldDescription = history.getDbDescription(oldVersion, instance);
                    Map<Integer, IConversionDescription> conversionDescriptions = history.getDbConversions();
                    IConversionDescription conv = conversionDescriptions.get(newVersion);

                    String msg = String.format("Upgrading DB '%s' Version %d Instance %d",
                            oldDescription.getName(),
                            oldDescription.getVersion(),
                            oldDescription.getInstance());
                    publishStatus(new StatusMessage(Status.UPGRADINGDB, msg));

                    oldPm = new PersistanceManager(oldDescription, null);
                    oldPm.initializeDb(ctx);

                    ConversionManager convMan = new ConversionManager(oldPm, this, conv);
                    conv.convert(convMan);

                    db.close();
                    File newDbFile = getDbFile();
                    Files.move(getUpgradingDbFile(), newDbFile);
                    db = SQLiteDatabase.openOrCreateDatabase(newDbFile, null);
                }
                catch (Exception ex) {
                    String errMsg = String.format("Error converting '%s' to new Version '%d' Instance '%d'",
                            dbDesc.getBaseName(), newVersion, instance);
                    Log.e(getClass().getName(), errMsg);
                    publishStatus(errMsg, ex);
                    throw new DBException(ex);
                }
                finally {
                    if (null != oldPm)
                       oldPm.close();

                    try {
                        File filesDir = ctx.getExternalFilesDir(null);
                        File backupOldDbFile = new File(filesDir, oldDescription.getName());
                        File dir = backupOldDbFile.getParentFile();
                        if (!dir.exists())
                            dir.createNewFile();
                        File oldDbFile = ctx.getDatabasePath(oldDescription.getName());

                        Files.move(oldDbFile, backupOldDbFile);
                    }
                    catch (IOException ex) {
                        String errMsg = String.format("Error moving db-file '%s' to %s",
                                oldDescription.getName(), ctx.getExternalFilesDir(null).getName());
                        Log.e(getClass().getName(), errMsg);
                        publishStatus(errMsg, ex);
                        throw new DBException(errMsg, ex);
                    }
                }
            }
        }
	}
	
	public <T extends IPersistable> void save(Collection<T> coll) throws DBException
	{
		if ( (null == coll) || (coll.size() == 0) )
			return;
		
		db.beginTransaction();
		try {
			IPersister<T> persister = null;
			for (T persistable : coll)
			{
				if (null == persister)
					persister = getPersister(persistable);
				DbId<T> dbId = persistable.getDbId();
				if (null == dbId)
				{
					long id = persister.insert(persistable);
					if (id >= 0) {
                        assignDbId(persistable, id);
					}
					else
						throw new DBException("Error inserting new row in database");
				} 
				else
				{
					if (!dbId.getDirty())
						continue;
					
					persister.update(persistable);
					dbId.setClean();
				}
			}
			db.setTransactionSuccessful();
		}
		finally 
		{
			db.endTransaction();
		}
	}

    public <T extends IPersistable> T load(IPersister<T> persister, Cursor cursor, int pos) throws DBException
    {
        T persistable = persister.rowToObject(pos, cursor);

        DbId<T> dbId = persistable.getDbId();
        if (dbId != null)
            dbId.setClean();
        else
        {
            /**
             * Do this in case we did NOT load via AutomaticPersister...
             */
            int idx = cursor.getColumnIndex("_id");
            cursor.moveToPosition(pos);
            if (idx > -1) {
                int id = cursor.getInt(idx);
                assignDbId(persistable, id);
            }
        }

        return persistable;
    }

	public <T extends IPersistable> T load(DbId<?> fathersId, Class<T> classObj, long id) throws DBException
	{
		IPersistable persistable = load(classObj, id);
		fathersId.addChild(persistable.getDbId());
		return (T) persistable;
	}

    public <T extends IPersistable> T load(DbId id) throws DBException
    {
        return (T)load(id.getType(), id.getId());
    }
	
	public <T extends IPersistable> T load(Class<T> classObj, long id) throws DBException
	{
		try {
			IPersister<T> persister = getPersister(classObj);
			T persistable = persister.load(id);
            assignDbId(persistable, id);
			return (T) persistable;
		} 
		catch (Exception ex)
		{
			throw new DBException(
					String.format("Error loading object of type=%s, id=%d", 
							classObj.getName(), id), ex);
		}
	}

    private <T extends IPersistable> Class<T> __saveAndUpdateForeignKeyNoTransaction(IPersister<T> persister, Collection<T> persistables, DbId<?> foreignKey) throws Exception
    {
        Class<T> type = null;

        if (!persistables.isEmpty()) {
            T persistable = persistables.iterator().next();
            type = (Class<T>)persistable.getClass();
        }

        for (T persistable : persistables) {

            DbId<?> dbId = persistable.getDbId();
            if ((null == dbId) || (dbId.getDirty())) {
                __saveNoTransaction(persister, persistable);
            }
            if ((null == dbId) || (dbId.getDirtyForeignKey()))
                __updateForeignKeyNoTransaction(persistable, foreignKey);
        }

        /**
         * The size of the collection-proxy is updated from the Cursors-Listener in the DBListeViewModelFactory...
         */

        return type;
    }

    public <T extends IPersistable> void saveAndUpdateForeignKey(Collection<T> persistables, IPersistable foreignKey) throws DBException
    {
        Class<T> type=null;
        try {
            db.beginTransaction();
            DbId<?> foreignDbId = foreignKey.getDbId();
            if ( (null == foreignDbId) || (foreignDbId.getDirty()) ) {
                __saveNoTransaction(foreignKey);
                foreignDbId = foreignKey.getDbId();
            }

            if (!persistables.isEmpty()) {
                T persistable = persistables.iterator().next();
                type = (Class<T>)persistable.getClass();
            }
            IPersister<T> persister = getPersister(type);

            type = __saveAndUpdateForeignKeyNoTransaction(persister, persistables, foreignDbId);

            db.setTransactionSuccessful();
        }
        catch (Exception ex)
        {
            throw new DBException(String.format("Error saving and updating foreign persistable for an list of objects of type=%s",
                    ( (null!=type) ? type.getName() : "unknown")), ex);
        }
        finally {
            db.endTransaction();
        }
    }
	
	public <T extends IPersistable> void saveAndUpdateForeignKey(Collection<T> persistables, DbId<?> foreignKey) throws DBException
	{
		Class<T> type=null;
		try {
			db.beginTransaction();
            if (!persistables.isEmpty()) {
                T persistable = persistables.iterator().next();
                type = (Class<T>)persistable.getClass();
            }
            IPersister<T> persister = getPersister(type);

            type = __saveAndUpdateForeignKeyNoTransaction(persister, persistables, foreignKey);

			db.setTransactionSuccessful();
		}
		catch (Exception ex)
		{
			throw new DBException(String.format("Error saving and updating foreign keys for an list of objects of type=%s", 
					( (null!=type) ? type.getName() : "unknown")));
		}
		finally {
			db.endTransaction();
		}
	}

    private <T extends IPersistable> void __saveNoTransaction(T persistable) throws DBException
    {
        Class<T> type = (Class<T>)persistable.getClass();
        IPersister<T> persister = getPersister(type);
        __saveNoTransaction(persister, persistable);
    }

    private <T extends IPersistable> void __saveNoTransaction(IPersister<T> persister, T persistable) throws DBException
    {

        DbId<?> dbId = persistable.getDbId();
        if (null == dbId)
        {
            try {
                long id = persister.insert(persistable);
                if (id >= 0) {
                    assignDbId(persistable, id);
                }
                else
                    throw new DBException("Error inserting new row in database");

            }
            catch (Exception ex)
            {
                throw new DBException(
                        String.format("Error inserting an object of type=%s to database",
                                persistable.getClass().getName()), ex);
            }
        }
        else
        {
            if (!dbId.getDirty())
                return;

            try {
                long rowsAffected = persister.update(persistable);
                if (rowsAffected != 1)
                    throw new DBException(String.format("Update of \"%s\" was not successful", persistable.getClass().getName()));

                dbId.setClean();
            }
            catch (Exception ex)
            {
                throw new DBException(
                        String.format("Error updating an object of type=%s, id=%d",
                                persistable.getClass().getName(),
                                dbId.getId()), ex);
            }
        }
    }

    public <T extends IPersistable> void save(T persistable) throws DBException
    {
        try {
            db.beginTransaction();
            Class classObj = (Class) persistable.getClass();
            IPersister persister = getPersister(classObj);

            if (null == persister)
                throw new DBException(String.format("Error could not find persister for type=%s",
                        classObj.getName()));

            __saveNoTransaction(persister, persistable);
            db.setTransactionSuccessful();
        }
        catch (Exception ex) {
            throw new DBException(
                    String.format("Error saving an object of type=%s",
                            persistable.getClass().getName()), ex);
        }
        finally
        {
            db.endTransaction();
        }
    }

    /**
     * Move the persistable with the DbId "item" to the container (foreign-key) with the
     * DbId "containerDst".
     * @param containerDst DbId of the destination container
     * @param item         DbId of the item to move
     * @param <T>
     * @throws DBException
     */
    public <T extends IPersistable> void move(DbId<T> containerSrc, DbId<T> containerDst, Field field, int oldSize, DbId<?> item) throws DBException
    {
        try {
            db.beginTransaction();
            Class classObj = (Class) item.getType();
            IPersister persister = getPersister(classObj);
            persister.updateForeignKey(item, containerDst);
            persister.updateCollectionProxySize(containerSrc, field, oldSize-1);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }

    }

    /**
     * Move the persistable items to the container (foreign-key) with the
     * DbId "containerDst".
     * @param <T>
     * @param containerDst DbId of the destination container
     * @param itemIds      DbIds of the items to move
     * @throws DBException
     */
    public <T extends IPersistable> void move(DbId<T> containerSrc, DbId<IPersistable> containerDst, Field fld, int oldSrcSize, int oldDstSize, ArrayList<DbId> itemIds) throws DBException
    {
        try {
            db.beginTransaction();
            Class classObj = (Class) itemIds.get(0).getType();
            IPersister persister = getPersister(classObj);

            for(DbId<?> itemId : itemIds) {
                persister.updateForeignKey(itemId, containerDst);
            }

            classObj = containerDst.getType();
            persister = getPersister(classObj);
            persister.updateCollectionProxySize(containerSrc, fld, oldSrcSize - itemIds.size());
            persister.updateCollectionProxySize(containerDst, fld, oldDstSize + itemIds.size());
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

	public <T extends IPersistable> void saveAndUpdateForeignKey(T persistable, DbId<?> foreignKey) throws DBException {
        DbId<?> dbId = persistable.getDbId();
        if ((null == dbId) || (dbId.getDirty())) {
            db.beginTransaction();
            try {
                Class classObj = (Class) persistable.getClass();
                IPersister<T> persister = getPersister(classObj);

                __saveNoTransaction(persister, persistable);
                __updateForeignKeyNoTransaction(persistable, foreignKey);
                db.setTransactionSuccessful();
            } catch (Exception ex) {
                throw new DBException(
                    String.format("Error updating foreign key for an object of type=%s, id=%s, foreignkey-id=%d",
                        persistable.getClass().getName(),
                        ((persistable.getDbId() == null) ? "NULL" : Long.valueOf(persistable.getDbId().getId()).toString()),
                        foreignKey.getId()), ex);

            } finally {
                db.endTransaction();
            }
        }
    }


//	public <T extends IPersistable<T>> void save(T persistable) throws DBException
//	{
//
//		DbId<T> dbId = persistable.getDbId();
//		if (null == dbId)
//		{
//			db.beginTransaction();
//			try {
//				Class<T> classObj = (Class<T>) persistable.getClass();
//				IPersister<T> persister = getPersister(classObj);
//				long id = persister.insert(persistable);
//				if (id >= 0) {
//					assignDbId(persistable, id);
//					persistable.getDbId().setClean();
//				}
//				else
//					throw new DBException("Error inserting new row in database");
//
//				db.setTransactionSuccessful();
//			}
//			catch (Exception ex)
//			{
//				throw new DBException(
//						String.format("Error inserting an object of type=%s to database", 
//								persistable.getClass().getSqlName()));
//			}
//			finally 
//			{
//				db.endTransaction();
//			}
//		}
//		else
//		{
//			if (!dbId.getDirty())
//				return;
//
//			db.beginTransaction();
//			try {
//				Class<T> classObj = (Class<T>) persistable.getClass();
//				IPersister<T> persister = getPersister(classObj);
//
//				persister.update(persistable);
//				dbId.setClean();
//				db.setTransactionSuccessful();
//			}
//			catch (Exception ex)
//			{
//				throw new DBException(
//						String.format("Error updating an object of type=%s, id=%d",
//								persistable.getClass().getSqlName(),
//								dbId.getId()));
//			}
//			finally 
//			{
//				db.endTransaction();
//			}
//		}
//	}

    public <T extends IPersistable> void delete(Class<T> type) {
        AbstractPersister<T> persister = (AbstractPersister<T>) getPersister(type);
        try {
            db.beginTransaction();
            if (!persister.hasDeleteActions()) {
                db.execSQL(String.format("DELETE FROM %s", DbNameHelper.getTableName(type)));
            }
            else
            {
                Cursor csr = getLoadAllCursor(type);
                for (int row = 0; row < csr.getCount(); row++) {
                    csr.moveToPosition(row);
                    persister.disableActions();
                    T res = persister.rowToObject(row, csr);
                    persister.enableActions();
                    persister.delete(res);
                }
            }


            db.setTransactionSuccessful();
        }
        catch (Exception ex) {
            throw new DBException(
						String.format("Error deleting objects of type=%s",
								type.getName()), ex);
        }
        finally
		{
    		db.endTransaction();
            persister.enableActions();
		}
    }

	public <T extends IPersistable> void delete(T persistable) throws DBException
	{
		DbId<T> dbId = persistable.getDbId();
		
		if (null == dbId)
			return;
					
		try {
			Class<T> classObj = (Class<T>) persistable.getClass();
			IPersister<T> persister = (IPersister<T>) getPersister(classObj);
            db.beginTransaction();

			persister.delete(persistable);
            persistable.setDbId(null);

            db.setTransactionSuccessful();
		}
		catch (Exception ex)
		{
			throw new DBException(
					String.format("Error deleting an object of type=%s",
							persistable.getClass().getName()), ex);
		}
        finally {
            db.endTransaction();
        }
    }
	
	public ICursorLoader getLoader(Class<?> referencingType, Class<?> referencedType)
	{
		Pair<Class<?>, Class<?>> key = new Pair<Class<?>, Class<?>>(referencingType, referencedType);
		return cursorLoaderMap.get(key);
	}

    /**
     * Get a DB-Cursor with all DB-objects of type referencedType and the foreign-Key
     * referencingType
     *
     * @param referencingType foreign-key-relation in the corresponding table of referencedType
     * @param referencedType  the type which determines which db-table is searched
     * @return the cursor
     * @throws DBException
     */
	public Cursor getCursor(Class<?> referencingType, Class<?> referencedType) throws DBException
	{
        if (referencingType == null)
        {
            return getLoadAllCursor(referencedType);
        }

		ICursorLoader loader = getLoader(referencingType, referencedType);
		return (loader != null) ? loader.getCursor(null) : null;
	}

    public Cursor getCursor(Class<?> referencingType, Class<?> referencedType, OrderByTerm... terms) throws DBException
    {
        if (referencingType == null)
        {
            return getLoadAllCursor(referencedType, terms);
        }

        ICursorLoader loader = getLoader(referencingType, referencedType);
        return (loader != null) ? loader.getCursor(null, terms) : null;
    }

    public Cursor getCursor(DbId<?> referencingObjectId, Class<?> referencingType, Class<?> referencedType) throws DBException
    {
        ICursorLoader loader = getLoader(referencingType, referencedType);
        return (loader != null) ? loader.getCursor(referencingObjectId) : null;
    }

    public Cursor getCursor(DbId<?> referencingObjectId, Class<?> referencingType, Class<?> referencedType, OrderByTerm... terms) throws DBException
    {
        ICursorLoader loader = getLoader(referencingType, referencedType);
        return (loader != null) ? loader.getCursor(referencingObjectId, terms) : null;
    }
	
	public Cursor getCursor(IPersistable referencingObject, Class<?> referencedType) throws DBException
	{
		ICursorLoader loader = getLoader(referencingObject.getClass(), referencedType);
		return (loader != null) ? loader.getCursor(referencingObject.getDbId()) : null;
	}

    public Cursor getCursor(IPersistable referencingObject, Class<?> referencedType, OrderByTerm... terms) throws DBException
    {
        ICursorLoader loader = getLoader(referencingObject.getClass(), referencedType);
        return (loader != null) ? loader.getCursor(referencingObject.getDbId(), terms) : null;
    }

    public void registerCursorLoaderFactory(ICursorLoaderFactory factory)
    {
        cursorLoaderFactories.add(factory);
    }
	
	public void registerCursorLoader(Class<?> referencingType, Class<?> referencedType, ICursorLoader loader)
	{
		Pair<Class<?>, Class<?>> key = new Pair<Class<?>, Class<?>>(referencingType, referencedType);
		cursorLoaderMap.put(key, loader);
	}

    public void unregisterCursorLoader(Class<?> referencingType, Class<?> referencedType) {
        Pair<Class<?>, Class<?>> key = new Pair<Class<?>, Class<?>>(referencingType, referencedType);
        cursorLoaderMap.remove(key);
    }
	
	public <T extends IPersistable> Cursor getLoadAllCursor(Class<?> classObj) throws DBException
	{
		IPersister persister = getIPersister(classObj);
		return persister.getLoadAllCursor();
	}

    public <T extends IPersistable> Cursor getLoadAllCursor(Class<?> classObj, OrderByTerm... terms) throws DBException
    {
        IPersister persister = getIPersister(classObj);
        return null != terms ? persister.getLoadAllCursor(terms) : persister.getLoadAllCursor();
    }
	
	public <T extends IPersistable> Collection<T> loadAll(Class classObj) throws DBException
	{
		IPersister<T> persister = getPersister(classObj);
		Collection<T> ret = persister.loadAll();
		
		for (T persistable : ret) {
			DbId<T> dbId = persistable.getDbId();
			
			dbId.setClean();
			dbId.setObj(persistable);
		}
		return ret;
	}

    public <T extends IPersistable> T loadFirst(Class<T> classObj, Cursor csr) throws DBException
    {
        T persistable = null;
        IPersister persister = getIPersister(classObj);
        if (csr.getCount() > 0) {
            persistable = (T) persister.rowToObject(0, csr);
            DbId<T> dbId = persistable.getDbId();
            dbId.setClean();
            dbId.setObj(persistable);
        }

        return persistable;
    }

	public <T extends IPersistable> T loadFirst(Class<T> classObj) throws DBException
	{
        T persistable = null;
        Cursor csr = null;
        try {
            csr = getLoadAllCursor(classObj);
            persistable = loadFirst(classObj, csr);
        }
        finally {
            if (csr != null)
                csr.close();
        }

		return persistable;
	}

    /**
     * Loads the object from the DB which references referencedObj (resolve a the DB-backwards-reference
     * which is usually not visible in the POJO-object-tree).
     *
     * @param referencingType Type of object to load
     * @param referencedObj Object wich is referenced by a type of object
     * @param <T>
     * @return Returns the object which references referencedObj or null if none is found
     */
    public <T extends IPersistable> T loadReferencingObject(final Class<T> referencingType, final IPersistable referencedObj) {
        if (null == referencedObj)
            throw new ArgumentException("Parameter referencedObj must not be null!");

        QueryBuilder qb = new QueryBuilder(new QueryBuilder.IDescriptionGetter() {
            @Override
            public PersisterDescription getDescription(PersistanceManager pm) {
                PersisterDescription desc = new PersisterDescription(referencedObj.getClass());
                desc.addSqlField(new SqlDataField(DbNameHelper.getForeignKeyFieldName(referencingType), SqlDataField.SqlFieldType.LONG));
                return desc;
            }
        }, "PSDARCH_GETPARENT");

        qb.addCondition(new QueryCondition("_id"));
        Query q = qb.createQuery(this);

        q.setParameter("_id", referencedObj.getDbId().getId());
        Cursor csr = q.run();
        if (csr.getCount() == 0)
            return null;

        csr.moveToNext();
        long id = csr.getLong(1);
        csr.close();

        return load(referencingType, id);
    }

    private <T extends IPersistable> void __updateForeignKeyNoTransaction(IPersister<T> persister, T persistable, DbId<?> foreignKey) throws DBException
    {
        persister.updateForeignKey(persistable.<T>getDbId(), foreignKey);
        persistable.getDbId().setCleanForeignKey();
    }

    private <T extends IPersistable> void __updateForeignKeyNoTransaction(T persistable, DbId<?> foreignKey) throws DBException
    {
        Class<T> classObj = (Class<T>) persistable.getClass();
        IPersister<T> persister = getPersister(classObj);

        __updateForeignKeyNoTransaction(persister, persistable, foreignKey);
    }

	public <T extends IPersistable> void updateForeignKey(T persistable, DbId<?> foreignKey) throws DBException
	{
		try {
            db.beginTransaction();
            __updateForeignKeyNoTransaction(persistable, foreignKey);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
	
	public <T extends IPersistable> DbId<T> assignDbId(T persistable, long id)
	{
		return assignDbId(persistable, id, true);
	}
	
	public <T extends IPersistable> DbId<T> assignDbId(T persistable, long id, boolean isClean)
	{
		if (null == persistable.getDbId())
		{
			DbId<T> dbId = new DbId<T>(persistable.getClass(), id);
			dbId.setObj(persistable);
			persistable.setDbId(dbId);
			if (isClean)
				persistable.getDbId().setClean();
			else
				persistable.getDbId().setDirty();
		}
		
		return persistable.getDbId();
	}

	public SQLiteDatabase getDb() {
		return db;
	}
	
	public void close()
	{
		this.initializedDb = false;
        Log.d(getClass().getName(), "Closing PersistanceManager!");
        if (null != db) {
            this.db.close();
            this.db = null;
        }
	}

	public <T extends IPersistable> ArrayList<T> loadCursor(Class<T> clazz, Cursor cursor) throws DBException {
		IPersister<T> persister = (IPersister<T>) getPersister(clazz);
		int cnt = cursor.getCount();
		ArrayList<T> lst = new ArrayList<T>(cnt);
		while(cursor.moveToNext())
		{
            T persistable = this.load(persister, cursor, cursor.getPosition());
            lst.add(persistable);
        }
        cursor.close();
		
		return lst;
	}

    public <T extends IPersistable> void registerPartialPersister(Class<T> type) throws DBException {
        try
        {
            DbPartialClass anno = type.getAnnotation(DbPartialClass.class);
            if (null==anno)
                throw new ArgumentException(String.format("Type %s does not have an DbPartialClass-Annotation!", type.getName()));

            AutomaticPersister<?> fullPersister = (AutomaticPersister<?>)getPersister((Class)anno.type());
            if (null==fullPersister)
                throw new ArgumentException(String.format("Could not find a Persister for type %s", anno.type().getName()));

            PartialClassReader<T> partialPersister = new PartialClassReader<T>(this, type, fullPersister);
            persisterMap.put(type, partialPersister);
        }
        catch (Exception ex)
        {
            throw new DBException("Error registering new PartialClassReader", ex);
        }
   }

   public <T extends IPersistable> void unregisterPartialPersister(Class<T> type) {
       PartialClassReader<T> partialPersister = (PartialClassReader<T>)getPersister(type);
       if (null!=partialPersister)
           partialPersister.unregisterCursorLoaders();

      persisterMap.remove(type);
   }

    public MVVM createMVVM() {
		if (this.mvvm == null) {
			this.mvvm = MVVM.createMVVM(this);
			if (null != this.ctx) {
				this.mvvm.setResources(this.ctx.getResources());
				this.mvvm.setContext(this.ctx);
			}
			IListViewModelFactory factory = new DbListViewModelFactory(this);
			this.mvvm.setListViewModelFactory(factory);
			this.mvvm.addGlobalCommitListener(new DBViewModelCommitListener());

            Class providerType = null;
            Class<?> modelType = null;
            try {
                for (Class<?> mType : persisterMap.keySet()) {
                    modelType = mType;
                    ComplexVmMapping anno = (ComplexVmMapping) modelType.getAnnotation(ComplexVmMapping.class);
                    if (null != anno) {
                        providerType = anno.vmFactoryType();
                        this.mvvm.registerVmProvider(modelType, providerType);
                    }
                }
            }
            catch (Exception e) {
                throw new MVVMException(String.format("Error creating provider of type '%s' for type '%s'",
                        providerType.getName(), modelType.getName()), e);
            }
		}

        return mvvm;
    }

    public void updateCollectionProxySize(DbId persistableId, Field field, long newCollSize) {
        Class type = persistableId.getType();
        IPersister<?> persister = getPersister(type);

        persister.updateCollectionProxySize(persistableId, field, newCollSize);
    }
    /**
     * Update the size of the collection which is saved in each "parent" object.
     *
     * @param persistableId The DbId of the parent which contains the list
     * @param field       The field within the parent which holds the list
     * @param childItems  The new / updated list of child items
     */
    public void updateCollectionProxySize(DbId persistableId, Field field, Collection childItems) {
        updateCollectionProxySize(persistableId, field, childItems.size());
    }

    public OrderByTerm[] orderByDbField(String... objFieldNames) {
        OrderByTerm[] terms = new OrderByTerm[objFieldNames.length];

        int i=0;
        for(String fieldName : objFieldNames)
        {
            terms[i++] = new OrderByTerm(fieldName, true);
        }

        return terms;
    }

    public OrderByTerm orderByDbField(String objFieldName, boolean ascending)
    {
        return new OrderByTerm(objFieldName, ascending);
    }

    public void registerQuery(QueryBuilder qb) {
        this.queries.put(qb.id(), qb);
    }

    public Query getQuery(String queryId) {
        QueryBuilder qb = this.queries.get(queryId);
        if (null == qb)
            throw new ArgumentException(String.format("Could not find query with id '%s'", queryId));

        return qb.createQuery(this);
    }

    /**
     * Register an action which is executed each time either on Save, Load or Delete (Determined by
     * the ActionType.
     * @param type Persistable type
     * @param action Action to exeute
     * @param <T> Type parameter
     * @see Action
     * @see ActionType
     */
    public <T extends IPersistable> void registerAction(Class<T> type, Action<T> action) {
        IPersister<T> persister = getPersister(type);
        if (null != persister) {
            persister.registerAction(action);
        }
        else
            throw new ArgumentException(String.format("Error registering Action. No persister for type '%s' found", type.getName()));
    }

    /**
     * Unregister an action.
     *
     * @param type Persistable type
     * @param action Action to exeute
     * @param <T> Type parameter
     * @see Action
     * @see ActionType
     */
    public <T extends IPersistable> void unregisterAction(Class<T> type, Action<T> action) {
        IPersister<T> persister = getPersister(type);
        if (null != persister) {
            persister.unregisterAction(action);
        }
        else
            throw new ArgumentException(String.format("Error unregistering Action. No persister for type '%s' found", type.getName()));
    }

    public int version() { return this.dbDesc.getVersion(); }
}
 