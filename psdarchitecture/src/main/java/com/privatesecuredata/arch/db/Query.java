package com.privatesecuredata.arch.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.privatesecuredata.arch.exceptions.ArgumentException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kenan on 1/29/16.
 */
public class Query<T> {
    private String queryId;
    private String sql;
    private SQLiteDatabase db;
    private Map<String, QueryCondition> conditions = new LinkedHashMap<>();
    private Map<String, QueryParameter> params = new LinkedHashMap<>();

    public Query(String id) {
        this.queryId = id;
    }

    public String id() {
        return queryId;
    }

    public QueryCondition getCondition(String condId) {
        return conditions.get(condId);
    }

    public void addCondition(QueryCondition condition)
    {
        QueryCondition localCondition = condition.clone();
        conditions.put(localCondition.Id(), localCondition);
        for (QueryParameter param : localCondition.parameters())
        {
            params.put(param.id(), param);
        }
    }

    public void setParameter(String paraId, Object value)
    {
        params.get(paraId).setValue(value);
    }

    public void setParameter(String paraId, Class value)
    {
        params.get(paraId).setValue(value.getName());
    }

    /**
     * @param pm
     * @param sql
     */
    public void prepare(PersistanceManager pm, AutomaticPersister persister,
                        Map<String, SqlDataField> fields, String sql) {
        this.db = pm.getDb();
        this.sql = sql;

        /**
         * Set default values
         */
        for(QueryCondition cond : conditions.values()) {
            if (!cond.isTypeCondition())
                continue;

            for (QueryParameter para : cond.parameters()) {
                /**
                 * Set default if no value set yet
                 */
                if (null == para.value()) {
                    SqlDataField sqlField= fields.get(DbNameHelper.getFieldName(para.fieldName(), SqlDataField.SqlFieldType.OBJECT_NAME));
                    para.setValue(sqlField.getField().getType().getName());
                }
            }
        }
    }

    public Cursor run() {
        String[] args = new String[params.size()];
        int i=0;
        for(QueryParameter para : params.values())
        {
            if (null == para.value())
                throw new ArgumentException(String.format("Parameter with ID \"%s\" has no value", para.id()));

            args[i++] = para.value().toString();
        }
        return this.db.rawQuery(this.sql, args);
    }


}
