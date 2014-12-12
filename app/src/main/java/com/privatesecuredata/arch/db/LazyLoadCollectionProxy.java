package com.privatesecuredata.arch.db;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

import android.database.Cursor;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;

public class LazyLoadCollectionProxy<T extends Collection<V>, 
		V extends IPersistable<V>> extends AbstractList<V>
		implements ILoadCollection<V>
{
	private IPersister<V> persister;
	private LazyCollectionInvocationHandler<T, V> handler;
	private PersistanceManager pm;
	
	public LazyLoadCollectionProxy(LazyCollectionInvocationHandler<T, V>  handler, PersistanceManager pm, IPersister<V> persister)
	{
		if ( (null == handler) || (null == persister) )
			throw new ArgumentException("Parameters handler and/or persister must not be null!");
		
		this.handler = handler;
		this.pm = pm;
		this.persister = persister;
	}
	
	@Override
	public V get(int location) {
		try {
			return persister.rowToObject(location, handler.getCursor());
		} catch (Exception e) {
			throw new DBException(String.format("Error converting cursor-content to object using persister of type=%s at position=%d", persister.getClass().getName(), location), e);
		}
	}

	@Override
	public int size() {
		return handler.size();
	}
	
	@Override
	public V set(int location, V object) {
		List<V> collection = handler.loadCollection();
		return collection.set(location, object);
	};
	
	@Override
	public void add(int location, V object) {
		List<V> collection = handler.loadCollection();
		collection.add(location, object);
	};
	
	@Override
	public V remove(int location) {
		List<V> collection = handler.loadCollection();
		return collection.remove(location);
	}
	
	@Override
	public Cursor getLoadAllCursor() {
		return handler.getCursor();
	}

	@Override
	public Collection<V> loadAll() throws DBException {
		return handler.loadCollection();
	}
}