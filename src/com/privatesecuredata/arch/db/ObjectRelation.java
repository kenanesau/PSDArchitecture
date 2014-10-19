package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;

public class ObjectRelation {
	private Field _fld;
	private Class _type;
	private IPersister _persister;
	
	public ObjectRelation(Field fld, Class<?> type)
	{
		_fld = fld;
		_type = type;
	}
	
	public Field getField() { return _fld; }
	public Class<?> getType() { return _type; }
	public IPersister getPersister(PersistanceManager pm)
	{
		if (null==_persister)
			_persister = pm.getPersister(_type);
		
		return _persister;
	}
}
