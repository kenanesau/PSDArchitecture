package com.privatesecuredata.arch.mvvm.vm;

/**
 * Created by kenan on 8/14/15.
 */
public class OrderBy {
    private Class _type;
    private String _fieldName;
    private boolean _ascending;

    public OrderBy(Class type, String objectFieldName)
    {
        this(type, objectFieldName, true);
    }

    public OrderBy(Class type, String objectFieldName, boolean asc)
    {
        this(objectFieldName, asc);
        _type = type;
    }

    public OrderBy(String objectFieldName, boolean asc)
    {
        _fieldName = objectFieldName;
        _ascending = asc;
    }

    public OrderBy(String objectFieldName)
    {
        this(objectFieldName, true);
    }

    public Class getType() { return _type; }

    public String getFieldName() {
        return _fieldName;
    }

    public boolean isAscending() {
        return _ascending;
    }
}
