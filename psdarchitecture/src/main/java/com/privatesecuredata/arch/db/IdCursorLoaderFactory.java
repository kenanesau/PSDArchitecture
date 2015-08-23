package com.privatesecuredata.arch.db;

import java.util.List;

/**
 * Created by kenan on 8/23/15.
 */
public class IdCursorLoaderFactory implements ICursorLoaderFactory {
    private PersistanceManager _pm;
    private Class _referencingType;
    private Class _referencedType;
    private List<SqlDataField> _fields;

    public IdCursorLoaderFactory(PersistanceManager pm, Class referencingType, Class referencedType) {
        this(pm, referencingType, referencedType, null);
    }

    public IdCursorLoaderFactory(PersistanceManager pm, Class referencingType, Class referencedType, List<SqlDataField> fields) {
        _pm = pm;
        _referencingType = referencingType;
        _referencedType = referencedType;
        _fields = fields;
    }

    public ICursorLoader create()
    {
        if (null != _fields)
            return new IdCursorLoader(_pm, _referencingType, _referencedType, _fields);
        else
            return new IdCursorLoader(_pm, _referencingType, _referencedType);
    }

    @Override
    public Class getReferencingType() {
        return _referencingType;
    }

    @Override
    public Class getReferencedType() {
        return _referencedType;
    }
}