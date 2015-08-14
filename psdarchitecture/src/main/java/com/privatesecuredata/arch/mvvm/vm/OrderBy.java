package com.privatesecuredata.arch.mvvm.vm;

/**
 * Created by kenan on 8/14/15.
 */
public class OrderBy {
    private String _fieldName;
    private boolean _ascending;

    public OrderBy(String objectFieldName, boolean asc)
    {
        _fieldName = objectFieldName;
        _ascending = asc;
    }

    public String getFieldName() {
        return _fieldName;
    }

    public boolean isAscending() {
        return _ascending;
    }
}
