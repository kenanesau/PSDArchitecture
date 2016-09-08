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
        params.get(paraId).setValue(value);
    }

    public void setParameter(String paraId, Class value)
    {
        params.get(paraId).setValue(DbNameHelper.getDbTypeName(value));
    }

    public void setForeignKeyParameter(String paraId, IPersistable value)
    {
        setForeignKeyParameter(paraId, value.getDbId());
    }

    public void setForeignKeyParameter(String paraId, DbId value)
    {
        setParameter(paraId, value.getId());
    }

    public void setForeignKeyParameter(IPersistable value) {
        setForeignKeyParameter(value.getDbId());
    }

    public void setForeignKeyParameter(DbId<?> value) {
        setForeignKeyParameter(DbNameHelper.getForeignKeyFieldName(value.getType()), value);
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
        String[] args = new String[params.size()];
        int i=0;
        StringBuilder sb = new StringBuilder(this.sql);

        sb = AbstractPersister.appendOrderByString(sb, order);

        for(QueryParameter para : params.values())
        {
            if (null == para.value())
                throw new ArgumentException(String.format(
                        "Running query \"%s\" Parameter with ID \"%s\" has no value. SQL: '%s'",
                        id(), para.id(), sb.toString()));

            args[i++] = para.getDbString();
        }
        return this.db.rawQuery(sb.toString(), args);
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
