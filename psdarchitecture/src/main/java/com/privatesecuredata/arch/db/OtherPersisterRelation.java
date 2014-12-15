package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;

public class OtherPersisterRelation {
	private IPersistable<?> _other;
	private IPersister<?> _otherPersister;
	private PersistanceManager _pm;
	
	public OtherPersisterRelation(PersistanceManager pm, Field fld)
	{
		_pm = pm;
	}
	
	public <T extends IPersistable<T>> IPersister<T> getPersister(Class<?> persistable) {
		
		if (null == _otherPersister)
		{
			_otherPersister = _pm.getPersister(_other);
		}
		
		return (IPersister<T>) _otherPersister;
	}
}
