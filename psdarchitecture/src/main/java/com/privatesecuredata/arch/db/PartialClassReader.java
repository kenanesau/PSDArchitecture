package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.db.annotations.DbField;
import com.privatesecuredata.arch.exceptions.DBException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kenan on 12/16/14.
 */
public class PartialClassReader<T extends IPersistable> extends AutomaticPersister<T> {
    AutomaticPersister<?> _fullPersister;
    String _selectAllSqlString;
    String _selectSingleSqlString;

    public PartialClassReader(PersistanceManager pm, Class<T> persistentType, AutomaticPersister<?> fullPersister) throws Exception {
        _fullPersister = fullPersister;
        setPM(pm);

        setPersistentType(persistentType);
        setConstructor(persistentType.getConstructor((Class<?>[])null));
        Field[] fields = persistentType.getDeclaredFields();
        HashMap<String, SqlDataField> fieldMap = new HashMap<String, SqlDataField>();
        for(Field field : fields)
        {
            DbField dbAnno = field.getAnnotation(DbField.class);
            if (null == dbAnno)
                continue;

            SqlDataField sqldf = new SqlDataField();
            sqldf.setId(dbAnno.id());
            sqldf.setField(field);

            fieldMap.put(sqldf.getId(), sqldf);
        }

        for(SqlDataField fullPersisterField : _fullPersister.getSqlFields())
        {
            SqlDataField partialPersisterField = fieldMap.get(fullPersisterField.getId());
            if (null == partialPersisterField)
                continue;

            partialPersisterField.setName(fullPersisterField.getSqlName());
            partialPersisterField.setSqlType(fullPersisterField.getSqlType());
            partialPersisterField.setTableName(fullPersisterField.getTableName());
        }
        setTableFieldsInternal(new ArrayList(fieldMap.values()));

        for(ObjectRelation objRel : _fullPersister.getOneToManyRelations())
        {
            ICursorLoader loader = new IdCursorLoader(getPM(),
                    _fullPersister.getPersistentType(),
                    objRel.getReferencedType(),
                    getTableFieldsInternal());

            /**
             * Loader gets registered for the persistentType of the partial view-class
             */
            getPM().registerCursorLoader(persistentType, objRel.getReferencedType(), loader);
        }

        createSelectAllSqlString();
        createSelectSingleSqlString();
        this.init(_fullPersister.getPM());
    }

    private void createSelectAllSqlString()
    {
        StringBuilder sql = new StringBuilder("SELECT _id");

        int fieldCount = 0;
        for(SqlDataField fld : getTableFieldsInternal() )
        {
            sql.append(", ")
              .append(fld.getSqlName());

            fieldCount++;
        }

        sql.append(" FROM ").append(getTableName());

        _selectAllSqlString = sql.toString();
    }

    private void createSelectSingleSqlString()
    {
        StringBuilder sql = new StringBuilder("SELECT _id");

        int fieldCount = 0;
        for(SqlDataField fld : getTableFieldsInternal() )
        {
            sql.append(", ")
                    .append(fld.getSqlName());

            fieldCount++;
        }

        sql.append(" FROM ")
            .append(getTableName())
            .append(" WHERE _id=?");

        _selectSingleSqlString = sql.toString();
    }

    @Override
    public long insert(T persistable) throws DBException
    {
        return -1;
    }

    @Override
    public long update(T persistable) throws DBException
    {
        return -1;
    }

    @Override
    public void updateForeignKey(DbId<T> persistableId, DbId<?> foreignId) throws DBException
    {}

    @Override
    public String getTableName()
    {
       return _fullPersister.getTableName();
    }

    @Override
    protected String getSelectAllStatement() { return _selectAllSqlString; }

    @Override
    protected String getSelectSingleSqlString() { return _selectSingleSqlString; }

    @Override
    protected String getUpdateStatement() { return null; }

    @Override
    protected String getInsertStatement() { return null; }

    public void unregisterCursorLoaders() {
        for(ObjectRelation objRel : _fullPersister.getOneToManyRelations())
        {
            getPM().unregisterCursorLoader(getPersistentType(), objRel.getReferencedType());
        }
    }
}
