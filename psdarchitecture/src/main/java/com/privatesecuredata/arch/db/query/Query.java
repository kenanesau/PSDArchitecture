package com.privatesecuredata.arch.db.query;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.privatesecuredata.arch.db.AutomaticPersister;
import com.privatesecuredata.arch.db.DbNameHelper;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.SqlDataField;
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
    private Map<String, IQueryCondition> conditions = new LinkedHashMap<>();
    private Map<String, QueryParameter> params = new LinkedHashMap<>();

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

    public void setParameter(String paraId, Object value)
    {
        params.get(paraId).setValue(value);
    }

    public void setParameter(String paraId, Class value)
    {
        params.get(paraId).setValue(DbNameHelper.getDbTypeName(value));
    }

    /**
     * @param pm
     * @param sql
     */
    public void prepare(PersistanceManager pm, AutomaticPersister persister, String sql) {
        this.db = pm.getDb();
        this.sql = sql;

        Map<String, SqlDataField> fields = persister.getFieldMap();
        /**
         * Set default values
         */
        for(IQueryCondition cond : conditions.values()) {
            cond.setDefaultValues(fields);
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
