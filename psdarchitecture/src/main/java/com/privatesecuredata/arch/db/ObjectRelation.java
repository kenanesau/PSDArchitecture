package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;

/**
 * Class to save meta-data for later db-handling (adding, deleting items from/to db)
 *
 * Objects of this type are heavily used by the AutomaticPersister
 *
 * @see AutomaticPersister
 */
public class ObjectRelation {
	private Field _fld;
	private IPersister _persister;
    private Class<?> _referencingType;
    private Class<?> _referencedListType;
    private boolean _deleteChildren;

    /**
     * Constructor used for referencing lists of child-objects
     *
     * @param fld
     * @param referencedListType
     * @param refereningType
     * @param deleteChildren
     */
    public ObjectRelation(Field fld, Class<?> referencedListType,  Class<?> refereningType, boolean deleteChildren)
    {
        _fld = fld;
        _referencedListType = referencedListType;
        _referencingType = refereningType;
        _deleteChildren = deleteChildren;
    }

    /**
     * Constructor for referencing single child-objects
     *
     * @param fld
     * @param referencedType
     * @param deleteChildren
     */
	public ObjectRelation(Field fld, Class<?> referencedType, boolean deleteChildren)
	{
		this(fld, null, referencedType, deleteChildren);
	}
	
	public Field getField() { return _fld; }
	public Class getReferencingType() { return _referencingType; }
    public Class getReferencedListType() { return _referencedListType; }
	public IPersister getPersister(PersistanceManager pm)
	{
		if (null==_persister)
			_persister = pm.getPersister(getReferencingType());
		
		return _persister;
	}
    public boolean deleteChildren() { return _deleteChildren; }
}
