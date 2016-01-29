package com.privatesecuredata.arch.db;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kenan on 1/22/16.
 */
public class QueryBuilder<T> {

    private Class type;
    private String queryId;
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

    public void addCondition(String queryId, String fldName) {
        addCondition(new QueryCondition(queryId, fldName));
    }

    public void addCondition(String fldName, Class type) {
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

        query.prepare(pm, sb.toString());
        /** maybe prohbit future changes...
        query.seal() **/

        //AbstractPersister.appendWhereClause(sb, )

        return query;
    }

}
