package com.privatesecuredata.arch.db;

/**
 * Created by kenan on 8/14/15.
 */
public class OrderByTerm {
    private Class _type;
    private String _dbTableName;
    private String _sqlFieldStr;
    private String _sqlOrderStr;

    public OrderByTerm(Class type, String objectFieldName, boolean asc)
    {
        _type = type;
         setSqlOrderStr(DbNameHelper.getTableName(type), DbNameHelper.getSimpleFieldName(objectFieldName), asc);
    }

    private void setSqlOrderStr(String tableName, String simpleFieldName, boolean asc) {
        _dbTableName = tableName;
        _sqlFieldStr = null == tableName ?
                simpleFieldName :
                String.format(" %s.%s", _dbTableName, simpleFieldName);
        _sqlOrderStr = null == tableName ?
                String.format(" %s %s", _sqlFieldStr, asc ? "ASC" : "DESC") :
                String.format(" %s.%s %s", tableName, simpleFieldName, asc ? "ASC" : "DESC");
    }

    public OrderByTerm(String objectFieldName, boolean asc)
    {
        _sqlFieldStr = DbNameHelper.getSimpleFieldName(objectFieldName);
        _sqlOrderStr = String.format(" %s %s", _sqlFieldStr, asc ? "ASC" : "DESC");
    }

    public OrderByTerm(SqlDataField dbField, boolean asc)
    {
        _sqlFieldStr = dbField.getSqlName();
        _sqlOrderStr = String.format(" %s %s", _sqlFieldStr, asc ? "ASC" : "DESC");
    }

    @Override
    public String toString() {
        return _sqlOrderStr;
    }

    public String getSqlFieldName() { return _sqlFieldStr; }
    public String getSqlTableName() { return _dbTableName; }
    public Class getType() { return _type; }
}
