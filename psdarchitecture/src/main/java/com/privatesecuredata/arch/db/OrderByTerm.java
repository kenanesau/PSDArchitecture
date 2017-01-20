package com.privatesecuredata.arch.db;

/**
 * Created by kenan on 8/14/15.
 */
public class OrderByTerm {
    private Class _type;
    private String _dbTableName;
    private String _sqlFieldStr;
    private String _sqlOrderStr;

    /**
     * Create an OderByTerm which is ascending by default
     * Use this constructor if you joined two types/tables and you want to
     * order by a field of the joined table.
     *
     * @param type Type/Table by was joined
     * @param objectFieldName field in the joined table
     */
    public OrderByTerm(Class type, String objectFieldName)
    {
        this(type, objectFieldName, true);
    }

    /**
     * Create an OderByTerm
     * Use this constructor if you joined two types/tables and you want to
     * order by a field of the joined table.
     * @param type Type/Table by was joined
     * @param objectFieldName field in the joined table
     * @param asc if true-> ascending, descending otherwise
     */
    public OrderByTerm(Class type, String objectFieldName, boolean asc)
    {
        _type = type;
         setSqlOrderStr(DbNameHelper.getTableName(type), DbNameHelper.getSimpleFieldName(objectFieldName), asc);
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

    private void setSqlOrderStr(String tableName, String simpleFieldName, boolean asc) {
        _dbTableName = tableName;
        _sqlFieldStr = null == tableName ?
                simpleFieldName :
                String.format(" %s.%s", _dbTableName, simpleFieldName);
        _sqlOrderStr = null == tableName ?
                String.format(" %s %s", _sqlFieldStr, asc ? "ASC" : "DESC") :
                String.format(" %s.%s %s", tableName, simpleFieldName, asc ? "ASC" : "DESC");
    }

    @Override
    public String toString() {
        return _sqlOrderStr;
    }

    public String getSqlFieldName() { return _sqlFieldStr; }
    public String getSqlTableName() { return _dbTableName; }
    public Class getType() { return _type; }
}
