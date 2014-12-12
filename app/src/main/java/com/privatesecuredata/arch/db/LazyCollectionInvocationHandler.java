package com.privatesecuredata.arch.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.database.Cursor;

import com.privatesecuredata.arch.exceptions.DBException;

public class LazyCollectionInvocationHandler<T extends Collection<V>, V extends IPersistable<V>> 
	implements InvocationHandler
{
	IPersistable<?> foreignKey;
	IPersister<V> persister;
	PersistanceManager pm;
	Cursor cursor = null;
	ICursorLoader loader;
	int size = 0;
	List<V> coll;
			

	public Cursor getCursor() {
		if (null == cursor) {
			try {
				cursor = loader.getCursor(foreignKey);
			} catch (Exception e) {
				throw new DBException("Error getting data via cursor", e);
			}
		}
		
		return cursor;
	}
	
	public int size() { return this.size; }
	
	public List<V> loadCollection() 
	{
		List<V> loadedColl = null;
		try {
			Cursor cursor = loader.getCursor(foreignKey);
			loadedColl = new ArrayList<V>(cursor.getCount());
			for (int pos=0; pos<cursor.getCount(); pos++)
				loadedColl.add(persister.rowToObject(pos, cursor));
			
			this.coll = loadedColl;
		} catch (Exception ex) {
			throw new DBException("Error loading collection: ", ex);
		}
		return loadedColl;
	}
	
	/**
	 * Invocation-Handler to load collections of IPersistable object from an ICursorloader
	 * 
	 * @param pm          The persistance Manager
	 * @param type        The type of the IPersistable objects to load
	 * @param foreignKey  The foreign key (The referencing object) 
	 * @param size        The cached size for the proxy
	 * @param loader      The ICursorLoader for loading 
	 */
	public LazyCollectionInvocationHandler(PersistanceManager pm, Class<V> type, IPersistable<?> foreignKey, int size, ICursorLoader loader) {
		this.pm = pm;
		this.foreignKey = foreignKey;
		this.loader = loader;
		this.persister = (IPersister<V>)pm.getPersister(type);
		coll = new LazyLoadCollectionProxy<T, V>(this, pm, persister);
		this.size = size;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		return method.invoke(coll, args);
	}
}