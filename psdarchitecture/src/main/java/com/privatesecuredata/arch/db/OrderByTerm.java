package com.privatesecuredata.arch.db;

/**
 * Created by kenan on 8/14/15.
 */
public class OrderByTerm {
    private String _dbFieldName;
    private boolean _ascending;

    public OrderByTerm(String objectFieldName, boolean asc)
    {
        _dbFieldName = DbNameHelper.getSimpleFieldName(objectFieldName);
        _ascending = asc;
    }

    public OrderByTerm(SqlDataField dbField, boolean asc)
    {
        _dbFieldName = dbField.getName();
        _ascending = asc;
    }

    @Override
    public String toString() {
        return String.format(" %s %s", _dbFieldName, _ascending ? "ASC" : "DESC");
    }
}
