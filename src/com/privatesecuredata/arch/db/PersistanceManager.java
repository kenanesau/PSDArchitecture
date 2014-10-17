package com.privatesecuredata.arch.db;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.privatesecuredata.arch.db.annotations.DbField;
import com.privatesecuredata.arch.db.annotations.Persister;
import com.privatesecuredata.arch.exceptions.DBException;

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
	private Hashtable<Class<?>, IPersister<? extends IPersistable<?>>> persisterMap = new Hashtable<Class<?>, IPersister<? extends IPersistable<?>>>();
	private Hashtable<Pair<IPersister<? extends IPersistable<?>>, Class<?>>, ICursorLoader> cursorLoaderMap = new Hashtable<Pair<IPersister<? extends IPersistable<?>>, Class<?>>, ICursorLoader>();
	private SQLiteDatabase db;
	private IDbDescription dbDesc;
	private boolean initialized = false;
	
	public PersistanceManager(IDbDescription dbDesc) 
	{
		this.dbDesc = dbDesc;
	}
	
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
					for(IPersister<? extends IPersistable<?>> persister : persisterMap.values())
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
	
	public void addPersister(Class<?> persisterClass) throws DBException 
	{
		try {
			Persister persisterAnnotation = persisterClass.getAnnotation(Persister.class);
			if (null==persisterAnnotation)
				throw new Exception("No annotation of type Persister");
				
			Constructor<?> ctor = persisterClass.getConstructor();
			IPersister<? extends IPersistable<?>> persisterObj = (IPersister<? extends IPersistable<?>>) ctor.newInstance();
					
			persisterMap.put(persisterAnnotation.persists(), persisterObj);
		}
		catch (Exception ex)
		{
			throw new DBException("Error adding Persister!", ex);			
		}
	}
	
	public void addPersistentType(Class<?> persistentType) {
		try {
			IPersister<? extends IPersistable<?>> persisterObj = new AutomaticPersister(persistentType);
					
			persisterMap.put(persistentType, persisterObj);
		}
		catch (Exception ex)
		{
			throw new DBException("Error adding common Persister!", ex);			
		}
	}
	
	public <T extends IPersistable<T>> IPersister<T> getPersister(Class<T> classObj)
	{
		return (IPersister<T>)persisterMap.get(classObj);
	}
	
	public <T extends IPersistable<T>> IPersister<T> getPersister(IPersistable<T> persistable)
	{
		return getPersister(persistable.getClass());
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
	
	public <T extends IPersistable<T>> void save(Collection<T> coll) throws DBException
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
						dbId = new DbId<T>(id);
						dbId.setObj(persistable);
						persistable.setDbId(dbId);
						dbId.setClean();
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
	
	public <T extends IPersistable<T>> T load(DbId<?> fathersId, Class<T> classObj, long id) throws DBException
	{
		IPersistable<T> persistable = load(classObj, id);
		fathersId.addChild(persistable.getDbId());
		return (T) persistable;
	}
	
	public <T extends IPersistable<T>> T load(Class<T> classObj, long id) throws DBException
	{
		try {
			IPersister<T> persister = getPersister(classObj);
			DbId<T> dbId = new DbId<T>(id);
			IPersistable<T> persistable = persister.load(dbId);
			dbId.setObj(persistable);
			persistable.setDbId(dbId);
			dbId.setClean();
			return (T) persistable;
		} 
		catch (Exception ex)
		{
			throw new DBException(
					String.format("Error loading object of type=%s, id=%d", 
							classObj.getName(), id));
		}
	}
	
	public <T extends IPersistable<T>> void saveAndUpdateForeignKey(T persistable, DbId<?> foreignKey) throws DBException
	{
		try {
			DbId<T> dbId = persistable.getDbId(); 
			if ( (null == dbId) || (dbId.getDirty()) )
			{
				save(persistable);
				updateForeignKey(persistable, foreignKey);
			}
		}
		catch (Exception ex)
		{
			throw new DBException(
					String.format("Error updating foreign key for an object of type=%s, id=%s, foreignkey-id=%d", 
							persistable.getClass().getName(), 
							((persistable.getDbId()==null) ? "NULL" : new Long(persistable.getDbId().getId()).toString()),
							foreignKey.getId()));
					
		}
	}
	
	public <T extends IPersistable<T>> void save(T persistable) throws DBException
	{

		DbId<T> dbId = persistable.getDbId();
		if (null == dbId)
		{
			db.beginTransaction();
			try {
				Class<T> classObj = (Class<T>) persistable.getClass();
				IPersister<T> persister = getPersister(classObj);
				long id = persister.insert(persistable);
				if (id >= 0) {
					assignDbId(persistable, id);
					persistable.getDbId().setClean();
				}
				else
					throw new DBException("Error inserting new row in database");

				db.setTransactionSuccessful();
			}
			catch (Exception ex)
			{
				throw new DBException(
						String.format("Error inserting an object of type=%s to database", 
								persistable.getClass().getName()));
			}
			finally 
			{
				db.endTransaction();
			}
		}
		else
		{
			if (!dbId.getDirty())
				return;

			db.beginTransaction();
			try {
				Class<T> classObj = (Class<T>) persistable.getClass();
				IPersister<T> persister = getPersister(classObj);

				persister.update(persistable);
				dbId.setClean();
				db.setTransactionSuccessful();
			}
			catch (Exception ex)
			{
				throw new DBException(
						String.format("Error updating an object of type=%s, id=%d",
								persistable.getClass().getName(),
								dbId.getId()));
			}
			finally 
			{
				db.endTransaction();
			}
		}
	}
	
	public <T extends IPersistable<T>> void delete(T persistable) throws DBException
	{
		DbId<T> dbId = persistable.getDbId();
		
		if (null == dbId)
			return;
					
		db.beginTransaction();
		try {
			Class<T> classObj = (Class<T>) persistable.getClass();
			IPersister<T> persister = getPersister(classObj);
			persister.delete(persistable);
			db.setTransactionSuccessful();
		}
		catch (Exception ex)
		{
			throw new DBException(
					String.format("Error deleting an object of type=%s to database", 
							persistable.getClass().getName()));
		}
		finally 
		{
			db.endTransaction();
		}
	}
	
	public ICursorLoader getLoader(Class<?> classObj, Class<?> foreignKeyClass)
	{
		IPersister<?> persister = persisterMap.get(classObj);
		Pair<IPersister<? extends IPersistable<?>>, Class<?>> key = new Pair<IPersister<? extends IPersistable<?>>, Class<?>>(persister, foreignKeyClass);
		return cursorLoaderMap.get(key);
	}
	
	public <T extends IPersistable<T>> Cursor getCursor(Class<T> classObj, Class<?> foreignKeyClass) throws DBException
	{
		ICursorLoader loader = getLoader(classObj, foreignKeyClass);
		return loader.getCursor(null);
	}
	
	public <T extends IPersistable<T>> Cursor getCursor(Class<T> classObj, IPersistable<?> foreignKey) throws DBException
	{
		ICursorLoader loader = getLoader(classObj, foreignKey.getClass());
		return loader.getCursor(foreignKey);		
	}
	
	public void registerCursorLoader(Class<?> classObj, Class<?> foreignKeyType, ICursorLoader loader)
	{
		IPersister<?> persister = persisterMap.get(classObj);
		Pair<IPersister<? extends IPersistable<?>>, Class<?>> key = new Pair<IPersister<? extends IPersistable<?>>, Class<?>>(persister, foreignKeyType);
		
		cursorLoaderMap.put(key, loader);
	}
	
	public <T extends IPersistable<T>> Cursor getLoadAllCursor(Class<T> classObj) throws DBException
	{
		IPersister<T> persister = getPersister(classObj);
		return persister.getLoadAllCursor();
	}
	
	public <T extends IPersistable<T>> Collection<T> loadAll(Class<T> classObj) throws DBException
	{
		IPersister<T> persister = getPersister(classObj);
		Collection<T> ret = persister.loadAll();
		
		for (T persistable : ret) {
			persistable.getDbId().setClean();
		}
		return ret;
	}
	
	public <T extends IPersistable<T>> void updateForeignKey(T persistable, DbId<?> foreignKey) throws DBException
	{
		Class<T> classObj = (Class<T>) persistable.getClass();
		IPersister<T> persister = getPersister(classObj);
		
		persister.updateForeignKey(persistable, foreignKey);
	}
	
	public <T extends IPersistable<T>> DbId<T> assignDbId(T persistable, long id)
	{
		if (null == persistable.getDbId())
		{
			DbId<T> dbId = new DbId<T>(id);
			dbId.setObj(persistable);
			persistable.setDbId(dbId);
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

	public <T extends IPersistable<T>> ArrayList<T> loadCursor(Class<T> clazz, Cursor cursor) throws DBException {
		IPersister<T> persister = (IPersister<T>) getPersister(clazz);
		int cnt = cursor.getCount();
		ArrayList<T> lst = new ArrayList<T>(cnt);
		while(cursor.moveToNext())
		{
			lst.add(persister.rowToObject(cursor.getPosition(), cursor));
		}
		
		return lst;
	}
}
 