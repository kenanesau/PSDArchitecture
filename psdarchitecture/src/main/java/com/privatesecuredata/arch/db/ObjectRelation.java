package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;

public class ObjectRelation {
	private Field _fld;
	private IPersister _persister;
    private Class<?> _referencedType;
	
	public ObjectRelation(Field fld, Class<?> referencedType)
	{
		_fld = fld;
        _referencedType = referencedType;
	}
	
	public Field getField() { return _fld; }
	public Class getReferencedType() { return _referencedType; }
	public IPersister getPersister(PersistanceManager pm)
	{
		if (null==_persister)
			_persister = pm.getPersister(getReferencedType());
		
		return _persister;
	}
}
