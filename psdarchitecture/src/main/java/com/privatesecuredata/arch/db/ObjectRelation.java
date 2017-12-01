package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.db.annotations.DbThisToOne;
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
    private boolean _isMandatory;
    private boolean _isComposition;
    private String _queryId;
    private Query _cachedQuery;

    /**
     * Constructor used for referencing lists of child-objects
     *
     * @param fld
     * @param referencedListType
     * @param referencingType
     * @param deleteChildren
     * @param isMandatory
     * @param queryId
     */
    public ObjectRelation(Field fld, Class<?> referencedListType, Class<?> referencingType, boolean deleteChildren, boolean isMandatory, boolean isComposition, String queryId)
    {
        _fld = fld;
        _referencedListType = referencedListType;
        _referencingType = referencingType;
        _deleteChildren = deleteChildren;
        _isMandatory = isMandatory;
        _isComposition = isComposition;
        if ( (null != queryId) && (queryId.compareTo("") == 0) ) {
            _queryId = null;
        }
        else {
            _queryId = queryId;
        }
    }

    public ObjectRelation(Field fld, Class<?> referencedListType, Class<?> referencingType, DbThisToOne thisToOneAnno, String queryId) {
        this(fld, referencedListType, referencingType, thisToOneAnno.deleteChildren(), thisToOneAnno.isMandatory(), thisToOneAnno.isComposition(), queryId);
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
		this(fld, null, referencedType, deleteChildren, true, false, null);
	}

    public ObjectRelation(Field fld, Class<?> referencedType, DbThisToOne thisToOne)
    {
        this(fld, null, referencedType, thisToOne, null);
    }

    public boolean isComposition() { return _isComposition; }
	public Field getField() { return _fld; }
	public Class getReferencingType() { return _referencingType; }

    /**
     * This is only set if the object relation relates to a list of objects
     * @return
     */
    public Class getReferencedListType() { return _referencedListType; }
	public IPersister getPersister(PersistanceManager pm)
	{
		if (null==_persister)
			_persister = pm.getPersister(getReferencingType());
		
		return _persister;
	}
    public boolean deleteChildren() { return _deleteChildren; }
    public boolean isMandatory() { return  _isMandatory; }
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
