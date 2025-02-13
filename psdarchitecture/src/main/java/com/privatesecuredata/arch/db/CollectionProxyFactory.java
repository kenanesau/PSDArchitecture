package com.privatesecuredata.arch.db;

import android.database.Cursor;

import java.util.Collection;
import java.util.List;
import java.lang.reflect.*;

public class CollectionProxyFactory {
	@SuppressWarnings("unchecked")
	public static<T extends Collection<V>, V extends IPersistable> T getCollectionProxy(PersistanceManager pm, Class<V> clazz, IPersistable parent, int size, ICursorLoader loader)
	{
		return (T) Proxy.newProxyInstance( clazz.getClassLoader(),
					new Class[] { List.class }, 
					new LazyCollectionInvocationHandler<T, V>(pm, clazz, parent, size, loader) );
	}

    public static<T extends Collection<V>, V extends IPersistable> T getCollectionProxy(PersistanceManager pm, Class<V> clazz, IPersistable parent, int size, Cursor csr)
    {
        return (T) Proxy.newProxyInstance( clazz.getClassLoader(),
                new Class[] { List.class },
                new LazyCollectionInvocationHandler<T, V>(pm, clazz, parent, size, csr) );
    }
}
