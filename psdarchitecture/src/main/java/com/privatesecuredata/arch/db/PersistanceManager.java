package com.privatesecuredata.arch.db;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.privatesecuredata.arch.db.annotations.DbPartialClass;
import com.privatesecuredata.arch.db.annotations.Persister;
import com.privatesecuredata.arch.db.vmGlue.DBViewModelCommitListener;
import com.privatesecuredata.arch.db.vmGlue.DbListViewModelFactory;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.IListViewModelFactory;

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
	private Hashtable<Class<?>, IPersister<? extends IPersistable>> persisterMap = new Hashtable<Class<?>, IPersister<? extends IPersistable>>();
    private Hashtable<String, Class<?>> classNameMap = new Hashtable<String, Class<?>>();
	private Hashtable<Pair<Class<?>, Class<?>>, ICursorLoader> cursorLoaderMap = new Hashtable<Pair<Class<?>, Class<?>>, ICursorLoader>();
	private SQLiteDatabase db;
	private IDbDescription dbDesc;
	private boolean initialized = false;
	
	public PersistanceManager(IDbDescription dbDesc) 
	{
		this.dbDesc = dbDesc;
	}
	
	/**
	 * This method initializes the database. 
	 * This means:
	 * - create the database and all tables if needed.
	 * - update the database if needed
	 * - 
	 * 
	 * 
	 * @param ctx
	 * @param dbDesc
	 * @throws DBException
	 */
	public void initialize(Context ctx, IDbDescription dbDesc) throws DBException
	{
		try {
			if (null == this.db)
			{
				ContextWrapper ctxWrapper = new ContextWrapper(ctx);
				File dbFile = ctxWrapper.getDatabasePath(dbDesc.getName());
				File dbDir = new File(dbFile.getParent());
				if (!dbDir.exists())
					dbDir.mkdir();
	
				boolean createDB = false;
	
				if (!dbFile.exists()) { 
					createDB=true;
				}
	
				db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
	
				if (createDB)
					onCreate(getDb());
				else
				{
					int currentVersion = getDb().getVersion();
	
					if (currentVersion < dbDesc.getVersion())
						onUpgrade(getDb(), currentVersion, dbDesc.getVersion());
					else if (currentVersion > dbDesc.getVersion())
					{
						throw new DBException(
								String.format("Current DB version \"V %d\" is new than that of the App \"V %d\"", 
										currentVersion, dbDesc.getVersion()));
					}
				}
	
				if (!this.initialized)
				{
					for(IPersister<? extends IPersistable> persister : persisterMap.values())
					{
						try {
							persister.init(this);
						}
						catch (Exception ex)
						{
	 						throw new DBException(
									String.format("Error initializing persister \"%s\"" , 
											persister.getClass().getName()), ex);
						}
					}
				}
	
				this.initialized = true;
			}
		}
		catch (Exception ex)
		{
			throw new DBException("Error initializing Persistance-Manager", ex);
		}
	}
	
	public boolean isInitialized() { return this.initialized; }
	
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
        classNameMap.put(persistentType.getName(), persistentType);

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
			throw new DBException("Error adding common Persister!", ex);			
		}
	}

    public Class<?> getPersistentType(String className)
    {
        return classNameMap.get(className);
    }

    public IPersister getIPersister(Class classObj)
    {
        return (IPersister)persisterMap.get(classObj);
    }

	public <T extends IPersistable> IPersister<T> getPersister(Class<T> classObj)
	{
		return (IPersister<T>)persisterMap.get(classObj);
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
                if (null != createSQL)
				    db.execSQL(createSQL);
			}
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onCreate(db);
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
        try {

            if (!persistables.isEmpty()) {
                T persistable = persistables.iterator().next();
                type = (Class<T>)persistable.getClass();
            }

            for (T persistable : persistables) {

                DbId<?> dbId = persistable.getDbId();
                if ((null == dbId) || (dbId.getDirty())) {
                    __saveNoTransaction(persister, persistable);
                    __updateForeignKeyNoTransaction(persister, persistable, foreignKey);
                }
            }

            /**
             * The size of the collection-proxy is updated from the Cursors-Listener in the DBListeViewModelFactory...
             */
        }
        finally {
            return type;
        }
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
                    ( (null!=type) ? type.getName() : "unknown")));
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

    public  <T extends IPersistable> void save(T persistable) throws DBException
    {
        try {
            db.beginTransaction();
            Class classObj = (Class) persistable.getClass();
            IPersister persister = getPersister(classObj);

            __saveNoTransaction(persister, persistable);
            db.setTransactionSuccessful();
        }
        finally
        {
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
//								persistable.getClass().getName()));
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
//								persistable.getClass().getName(),
//								dbId.getId()));
//			}
//			finally 
//			{
//				db.endTransaction();
//			}
//		}
//	}
	
	public <T extends IPersistable> void delete(T persistable) throws DBException
	{
		DbId<T> dbId = persistable.getDbId();
		
		if (null == dbId)
			return;
					
		try {
			Class<T> classObj = (Class<T>) persistable.getClass();
			IPersister<T> persister = (IPersister<T>) getPersister(classObj);
			persister.delete(persistable);
		}
		catch (Exception ex)
		{
			throw new DBException(
					String.format("Error deleting an object of type=%s to database", 
							persistable.getClass().getName()));
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

    public Cursor getCursor(DbId<?> referencingObjectId, Class<?> referencingType, Class<?> referencedType) throws DBException
    {
        ICursorLoader loader = getLoader(referencingType, referencedType);
        return (loader != null) ? loader.getCursor(referencingObjectId) : null;
    }
	
	public Cursor getCursor(IPersistable referencingObject, Class<?> referencedType) throws DBException
	{
		ICursorLoader loader = getLoader(referencingObject.getClass(), referencedType);
		return (loader != null) ? loader.getCursor(referencingObject.getDbId()) : null;
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
	
	public <T extends IPersistable> Collection<T> loadAll(Class<T> classObj) throws DBException
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

    private <T extends IPersistable> void __updateForeignKeyNoTransaction(IPersister<T> persister, T persistable, DbId<?> foreignKey) throws DBException
    {
        persister.updateForeignKey(persistable, foreignKey);
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
			DbId<T> dbId = new DbId<T>(id);
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
		this.db.close();
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
        MVVM mvvm = MVVM.createMVVM(this);
        IListViewModelFactory factory = new DbListViewModelFactory(this);
        mvvm.setListViewModelFactory(factory);
        mvvm.addGlobalCommitListener(new DBViewModelCommitListener());
        return mvvm;
    }

    /**
     * Update the size of the collection which is saved in each "parent" object.
     *
     * @param persistable The parent which contains the list
     * @param field       The field within the parent which holds the list
     * @param childItems  The new / updated list of child items
     */
    public void updateCollectionProxySize(IPersistable persistable, Field field, Collection childItems) {
        Class type = persistable.getClass();
        IPersister<?> persister = getPersister(type);

        persister.updateCollectionProxySize(persistable, field, childItems);
    }
}
 