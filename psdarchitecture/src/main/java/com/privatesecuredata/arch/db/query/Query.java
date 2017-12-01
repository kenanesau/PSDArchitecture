package com.privatesecuredata.arch.db.query;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.privatesecuredata.arch.db.AbstractPersister;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.DbNameHelper;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.OrderByTerm;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersisterDescription;
import com.privatesecuredata.arch.db.SqlDataField;
import com.privatesecuredata.arch.exceptions.ArgumentException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by kenan on 1/29/16.
 */
public class Query<T> {
    public final String PARA_ID = "para_id";

    private String queryId;
    private String sql;
    private SQLiteDatabase db;
    private Map<String, IQueryCondition> conditions = new LinkedHashMap<>();
    private Map<String, QueryParameter> params = new LinkedHashMap<>();
    private OrderByTerm[] orderByTerms;
    private IPersistable foreignKeyPersistable;
    private String foreignKeyParaId;
    private ReentrantLock mtx = new ReentrantLock();

    public Query(String id) {
        this.queryId = id;
    }

    public String id() {
        return queryId;
    }

    public IQueryCondition getCondition(String condId) {
        return conditions.get(condId);
    }

    public void addCondition(IQueryCondition condition)
    {
        IQueryCondition localCondition = condition.clone();
        conditions.put(localCondition.id(), localCondition);
        for (QueryParameter param : localCondition.parameters())
        {
            params.put(param.id(), param);
        }
    }

    public void setOrderByTerms(OrderByTerm[] orderByTerms)
    {
        this.orderByTerms = orderByTerms;
    }

    public void setParameter(String paraId, Object value)
    {
        QueryParameter para = params.get(paraId);
        if (null == para)
            throw new ArgumentException(String.format("Query '%s' was not able to find parameter '%s'", id(), paraId));

        para.setValue(value);
    }

    public void setParameter(String paraId, Class value)
    {
        QueryParameter para = params.get(paraId);
        if (null == para)
            throw new ArgumentException(String.format("Query '%s' was not able to find parameter '%s'", id(), paraId));

        para.setValue(DbNameHelper.getDbTypeName(value));
    }

    public void setForeignKeyParameter(String paraId, IPersistable value)
    {
        setForeignKeyParameter(paraId, value.getDbId());
    }

    public void setForeignKeyParameter(String paraId, DbId value)
    {
        foreignKeyParaId = paraId;
        setParameter(paraId, value.getId());
    }

    public void setForeignKeyParameter(Class foreignKeyType, IPersistable val)
    {
        setForeignKeyParameter(DbNameHelper.getForeignKeyFieldName(foreignKeyType, val.getClass()), val);
    }

    public void setForeignKeyParameter(Class otherType, Class foreignKeyType, IPersistable val)
    {
        setForeignKeyParameter(DbNameHelper.getForeignKeyFieldName(otherType, foreignKeyType), val);
    }

    public void setForeignKeyParameter(IPersistable value) {
        try {
            mtx.lock();
            if (value == null)
                throw new ArgumentException("Parameter 'value' must not be null");

            if (value.getDbId() != null) {
                setForeignKeyParameter(value.getDbId());
                foreignKeyPersistable = null;
            } else {
                this.foreignKeyPersistable = value;
                if (foreignKeyParaId != null) {
                    params.get(foreignKeyParaId).setValue(-1);
                    foreignKeyParaId = null;
                }
            }
        } finally {
            mtx.unlock();
        }
    }

    public void setForeignKeyParameter(DbId<?> value) {
        setForeignKeyParameter(DbNameHelper.getForeignKeyFieldName(value.getType()), value);
    }

    /**
     * Sets a parameter which checks the corresponding condition which was added with
     * addMatchingObjRefCondition. The condition returns true if the referenced object is the same
     * (has the same DB-ID)
     *
     * @sa addMatchingObjRefCondition
     * @param fldName
     * @param obj
     */
    public void setMatchinObjRefParameter(String fldName, IPersistable obj) {
        setParameter(
                DbNameHelper.getFieldName(fldName, SqlDataField.SqlFieldType.OBJECT_REFERENCE),
                obj != null ? obj.getDbId().getId() : null);
        setParameter(
                DbNameHelper.getFieldName(fldName, SqlDataField.SqlFieldType.OBJECT_NAME),
                obj != null ? DbNameHelper.getDbTypeName(obj.getDbId().getType()) : null);
    }

    /**
     * Sets a parameter for the corresponding condition which was added with addEqualsObjCondition.
     * The condition checks for equality (uses the equals()-operator on...) the object which is
     * used as a parameter here (obj)
     *
     * TODO: Implementation not finished yet, Setting the parameter value does not work yet
     *
     * @sa addEqualsObjCondition
     * @param fldName Object field name
     * @param obj Paramter / Object to check for equality
     */
    public void setEqualsObjParameter(String fldName, IPersistable obj) {
        setParameter(fldName, obj);
    }

    /**
     * @param pm
     * @param sql
     */
    public void prepare(PersistanceManager pm, PersisterDescription description, String sql) {
        this.db = pm.getDb();
        this.sql = sql;

        Map<String, SqlDataField> fields = description.getFieldMap();

        /**
         * Set default values
         */
        for(IQueryCondition cond : conditions.values()) {
            cond.setDefaultValues(fields);
        }
    }

    /**
     * Run the query, overriding the order-by-terms set within the query
     *
     * @param order New order-by-terms
     * @return Cursor with results
     */
    public Cursor run(OrderByTerm[] order) {
        try {
            mtx.lock();

            String[] args = new String[params.size()];
            int i = 0;
            StringBuilder sb = new StringBuilder(this.sql);

            sb = AbstractPersister.appendOrderByString(sb, order);

            if (foreignKeyPersistable != null) {
            /* If the fk-persistable is not saved to the DB yet */
                if (foreignKeyPersistable.getDbId() == null)
                    return null;
            }

            for (QueryParameter para : params.values()) {
                if (null == para.value())
                    throw new ArgumentException(String.format(
                            "Running query \"%s\" Parameter with ID \"%s\" has no value. SQL: '%s'",
                            id(), para.id(), sb.toString()));

                args[i++] = para.getDbString();
            }

            return this.db.rawQuery(sb.toString(), args);
        }
        finally {
            mtx.unlock();
        }
    }

    /**
     * Run the query
     *
     * @return The cursur with results
     */
    public Cursor run() {
        return run(this.orderByTerms);
    }

    public String getSqlStatement() {
        String[] args = new String[params.size()];
        int i=0;
        StringBuilder sb = new StringBuilder(this.sql);

        for(QueryParameter para : params.values())
        {
            args[i++] = (para.value() != null ? para.getDbString() : "NULL");
        }

        return sb.toString();
    }

}
