package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.db.annotations.DbField;
import com.privatesecuredata.arch.db.annotations.DbForeignKeyField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kenan on 3/18/16.
 *
 * Contains a "description" of all fields in the DB of a persistent type
 */
public class PersisterDescription<T> {

    private final String _dbTypeName;
    private Map<String, SqlDataField> _tableFields = new LinkedHashMap<>();
    private List<SqlDataField> _proxyCntFields;

    /**
     * ID-Fields in the table of the current persister which point to rows in other tables
     * (Counter-part of an OneToMany- or an OneToOne-Relation of another Persister
     */
    private Hashtable<Class<?>, SqlForeignKeyField> _foreignKeyFields;

    /**
     * List of Fields to be filled in an persistable and the corresponding types
     */
    private Map<String, ObjectRelation> _thisToOneRelations;
    private Map<String, ObjectRelation> _oneToManyRelations;
    private Class<T> _persistentType;

    public PersisterDescription(Class<T> type) {
        _persistentType = type;
        _dbTypeName = DbNameHelper.getDbTypeName(_persistentType);
        _thisToOneRelations = new Hashtable<>();
        _oneToManyRelations = new Hashtable<>();
        _foreignKeyFields = new Hashtable<Class<?>, SqlForeignKeyField>();
    }

    public String getTableName() {
        return DbNameHelper.getTableName(_persistentType);
    }

    public void addForeignKeyField(DbForeignKeyField dbAnno) {
        Class<?> foreignKeyType = dbAnno.foreignType();

        SqlForeignKeyField sqlForeignKeyField = new SqlForeignKeyField(getTableName(), foreignKeyType);
        if (dbAnno.isMandatory())
            sqlForeignKeyField.setMandatory();
        Class<?> key = foreignKeyType;
        addForeignKeyField(key, sqlForeignKeyField);
    }

    public void addForeignKeyField(Class foreignType, SqlForeignKeyField fld) {
        _foreignKeyFields.put(foreignType, fld);
    }

    public Collection<SqlForeignKeyField> getForeignKeyFields() {
        return _foreignKeyFields.values();
    }

    public boolean hasForeignKeyFields()
    {
        return _foreignKeyFields.size() > 0;
    }


    public void addSqlField(SqlDataField sqlField) {
        _tableFields.put(sqlField.getSqlName(), sqlField);
    }

    public void addSqlField(Field field, DbField anno) {
        SqlDataField sqlField = new SqlDataField(field);
        if (anno.isMandatory())
            sqlField.setMandatory();
        if (!anno.id().equals(""))
            sqlField.setId(anno.id());

        addSqlField(sqlField);
    }

    public SqlDataField getTableField(String sqlName) {
        return _tableFields.get(sqlName);
    }

    public void addOneToOneRelation(ObjectRelation rel)
    {
        _thisToOneRelations.put(rel.getField().getName(), rel);
    }

    public ObjectRelation getOneToOneRelation(String fieldName)
    {
        return _thisToOneRelations.get(fieldName);
    }

    public Collection<ObjectRelation> getOneToOneRelations() {
        return _thisToOneRelations.values();
    }

    public void addOneToManyRelation(ObjectRelation rel)
    {
        _oneToManyRelations.put(rel.getField().getName(), rel);
    }

    public Collection<ObjectRelation> getOneToManyRelations() {
        return _oneToManyRelations.values();
    }

    public ObjectRelation getOneToManyRelation(String fieldName)
    {
        return _oneToManyRelations.get(fieldName);
    }

    public Map<String, SqlDataField> getFieldMap() {
        return new LinkedHashMap<>(_tableFields);
    }

    public Collection<SqlDataField> getTableFields() {
        return _tableFields.values();
    }

    public void addProxyCntField(SqlDataField collectionProxySizeFld) {
        addSqlField(collectionProxySizeFld);

        if (null == _proxyCntFields)
            _proxyCntFields = new ArrayList<SqlDataField>();

        /**
         * save the SQLData-field to create an update-statement for the proxy-fields
         * later during init().
         */
        _proxyCntFields.add(collectionProxySizeFld);
    }

    public List<SqlDataField> getAndResetProxyCntFields()
    {
        List<SqlDataField> ret = _proxyCntFields;

        _proxyCntFields = null;

        return  ret;
    }

    SqlForeignKeyField getForeignKeyField(Class foreignType)
    {
        return _foreignKeyFields.get(foreignType);
    }

    void extend(PersisterDescription<?> other)
    {
        for (SqlDataField fld : other.getTableFields())
            _tableFields.put(fld.getSqlName(), fld);

        if (other._proxyCntFields != null) {
            for (SqlDataField fld : other._proxyCntFields)
                _proxyCntFields.add(fld);
        }

        _foreignKeyFields.putAll(other._foreignKeyFields);

        for (ObjectRelation rel : other._thisToOneRelations.values())
            _thisToOneRelations.put(rel.getField().getName(), rel);

        for (ObjectRelation rel : other._oneToManyRelations.values())
            _oneToManyRelations.put(rel.getField().getName(), rel);
    }

    public Class<T> getType() { return _persistentType;}

    public String getDbTypeName() { return _dbTypeName; }
}
