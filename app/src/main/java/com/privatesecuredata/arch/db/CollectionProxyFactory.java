package com.privatesecuredata.arch.db;

import java.util.Collection;
import java.util.List;
import java.lang.reflect.*;

public class CollectionProxyFactory {
	@SuppressWarnings("unchecked")
	public static<T extends Collection<V>, V extends IPersistable<V>> T getCollectionProxy(PersistanceManager pm, Class<V> clazz, IPersistable<?> parent, int size, ICursorLoader loader)
	{
		return (T) Proxy.newProxyInstance( clazz.getClassLoader(),
					new Class[] { List.class }, 
					new LazyCollectionInvocationHandler<T, V>(pm, clazz, parent, size, loader) );
	}
}
