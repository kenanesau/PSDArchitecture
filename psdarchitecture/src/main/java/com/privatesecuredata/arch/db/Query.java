package com.privatesecuredata.arch.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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

    /**
     * @param pm
     * @param sql
     */
    public void prepare(PersistanceManager pm, String sql) {
        this.db = pm.getDb();
        this.sql = sql;
    }

    public Cursor run() {
        String[] args = new String[params.size()];
        int i=0;
        for(QueryParameter para : params.values())
        {
            args[i++] = para.toString();
        }
        return this.db.rawQuery(this.sql, args);
    }


}
