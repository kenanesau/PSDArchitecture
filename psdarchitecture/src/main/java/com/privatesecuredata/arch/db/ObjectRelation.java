package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.db.query.Query;

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
    private String _queryId;
    private Query _cachedQuery;

    /**
     * Constructor used for referencing lists of child-objects
     *
     * @param fld
     * @param referencedListType
     * @param referencingType
     * @param deleteChildren
     * @param queryId
     */
    public ObjectRelation(Field fld, Class<?> referencedListType,  Class<?> referencingType, boolean deleteChildren, String queryId)
    {
        _fld = fld;
        _referencedListType = referencedListType;
        _referencingType = referencingType;
        _deleteChildren = deleteChildren;
        if ( (null != queryId) && (queryId.compareTo("") == 0) ) {
            _queryId = null;
        }
        else {
            _queryId = queryId;
        }
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
		this(fld, null, referencedType, deleteChildren, null);
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
    public String getQueryId() { return _queryId; }
    public Query getAndCacheQuery(PersistanceManager pm) {
        if (null != this._cachedQuery)
            return this._cachedQuery;

        if (null != this._queryId) {
            this._cachedQuery = pm.getQuery(_queryId);
        }

        return _cachedQuery;
    }
}
