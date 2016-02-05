package com.privatesecuredata.arch.db;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kenan on 1/22/16.
 */
public class QueryBuilder<T> {

    private Class type;
    private String queryId;
    /**
     * Condition-ID -> Condition
     */
    private HashMap<String, QueryCondition> conditions = new HashMap<>();

    public QueryBuilder(Class type, String queryId) {
        this.type = type;
        this.queryId = queryId;
    }

    public void addCondition(QueryCondition cond) {
        conditions.put(cond.Id(), cond);
    }

    public void addCondition(String fldName) {
        addCondition(new QueryCondition(fldName));
    }

    public void addCondition(String condId, String fldName) {
        addCondition(new QueryCondition(condId, fldName));
    }

    public void addTypeCondition(String condId, String paraId, String fldName) {
        QueryCondition cond = new QueryCondition(condId, paraId, fldName);
        cond.setTypeCondition();
        addCondition(cond);
    }

    public void addTypeCondition(String condId, String fldName) {
        addTypeCondition(condId, fldName, fldName);
    }

    public void addTypeCondition(String fldNameAndId) {
        addTypeCondition(fldNameAndId, fldNameAndId, fldNameAndId);
    }

    public String id() { return queryId; }

    public Query createQuery(PersistanceManager pm) {
        Query query = new Query(id());
        AutomaticPersister persister = (AutomaticPersister)pm.getIPersister(type);
        Map<String, SqlDataField> fields = persister.getFieldMap();

        StringBuilder sb = new StringBuilder(persister.getSelectAllStatement());

        sb.append(" WHERE ");

        for (String condId : conditions.keySet())
        {
            QueryCondition cond = conditions.get(condId);
            sb = cond.append(fields, sb);

            query.addCondition(cond);
        }

        query.prepare(pm, persister, fields, sb.toString());
        /** maybe prohibit future changes...
        query.seal() **/

        return query;
    }

}
