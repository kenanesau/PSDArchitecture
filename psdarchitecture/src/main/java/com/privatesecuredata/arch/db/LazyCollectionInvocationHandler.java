package com.privatesecuredata.arch.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.database.Cursor;

import com.privatesecuredata.arch.exceptions.DBException;

public class LazyCollectionInvocationHandler<T extends Collection<V>, V extends IPersistable<V>>
	implements InvocationHandler, ICursorChangedListener
{
	IPersistable<?> foreignKey;
	IPersister<V> persister;
	PersistanceManager pm;
	Cursor cursor = null;
	ICursorLoader loader;
	int size = 0;
	List<V> proxyList = null;
    boolean loaded = false;
			

	public Cursor getCursor() {
		if (null == cursor) {
			try {
				cursor = loader.getCursor(foreignKey.getDbId());
			} catch (Exception e) {
				throw new DBException("Error getting data via cursor", e);
			}
		}
		
		return cursor;
	}
	
	public int size() {
        if (loaded)
            return proxyList.size();
        else if (cursor != null)
            return cursor.getCount();
        else
            return this.size;
    }
	
	public List<V> loadCollection() 
	{
		try {
            if (!loaded) {
                Cursor cursor = getCursor();
                proxyList = new ArrayList<V>(cursor.getCount());
                for (int pos = 0; pos < cursor.getCount(); pos++)
                    proxyList.add(pm.load(persister, cursor, pos));
                loaded=true;
            }
		} catch (Exception ex) {
			throw new DBException("Error loading collection: ", ex);
		}
		return proxyList;
	}
	
	/**
	 * Invocation-Handler to load collections of IPersistable objects from an ICursorloader
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
		proxyList = new LazyLoadCollectionProxy<T, V>(this, pm, persister);
		this.size = size;
	}

    /**
     * Constructor of LazyCollectionInvocationHandler to directly give a Cursor to it
     *
     * @param pm          The persistance Manager
     * @param type        The type of the IPersistable objects to load
     * @param foreignKey  The foreign key (The referencing object)
     * @param size        The cached size for the proxy
     * @param csr         The cursor which is used for the proxied list of objects
     */
    public LazyCollectionInvocationHandler(PersistanceManager pm, Class<V> type, IPersistable<?> foreignKey, int size, Cursor csr) {
        this.pm = pm;
        this.foreignKey = foreignKey;
        this.cursor = csr;
        this.persister = (IPersister<V>)pm.getPersister(type);
        proxyList = new LazyLoadCollectionProxy<T, V>(this, pm, persister);
        this.size = size;
    }
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		return method.invoke(proxyList, args);
	}

    @Override
    public void notifyCursorChanged(Cursor csr) {
        this.cursor = csr;
        if (loaded) {
            loaded = false;
            loadCollection();
        }
    }

    public boolean isLoaded() { return this.loaded; }
}