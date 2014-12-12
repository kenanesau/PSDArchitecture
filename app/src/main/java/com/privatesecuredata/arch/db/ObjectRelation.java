package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;

public class ObjectRelation {
	private Field _fld;
	private IPersister _persister;
	
	public ObjectRelation(Field fld)
	{
		_fld = fld;
	}
	
	public Field getField() { return _fld; }
	public Class getType() { return _fld.getType(); }
	public IPersister getPersister(PersistanceManager pm)
	{
		if (null==_persister)
			_persister = pm.getPersister(getType());
		
		return _persister;
	}
}
