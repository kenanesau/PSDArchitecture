package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersisterDescription;
import com.privatesecuredata.arch.db.SqlDataField;

import java.util.LinkedList;
import java.util.Map;

/**
 * A QueryConditionContainer is a container of other IQueryCondition-implementations.
 * It either concatenates its child-conditions via AND or OR.
 */
public class QueryConditionContainer implements IQueryCondition {

    public enum Operation {
        AND(1),
        OR(2);

        private int value;

        private Operation(int value) {
            this.value = value;
        }

    }

    private LinkedList<IQueryCondition> conditions = new LinkedList<>();
    protected LinkedList<QueryParameter> params = new LinkedList<>();
    private String condId;
    Operation op = Operation.AND;

    protected QueryConditionContainer() { }

    public QueryConditionContainer(String condId)
    {
        this.condId = condId;
    }

    /**
     * Create a query
     *
     * @param condId ID of the condition
     */
    public QueryConditionContainer(String condId, IQueryCondition... conditions) {
        this(condId);

        addCondition(conditions);
    }

    public String getCondId() { return condId; }

    public void addCondition(IQueryCondition... conditions)
    {
        for (IQueryCondition cond : conditions) {
            this.conditions.add(cond);

            for(QueryParameter param : cond.parameters()) {
                params.addLast(param);
            }
        }
    }

    public Operation op() {
        return op;
    }

    public void setOperation(Operation op)
    {
        this.op = op;
    }

    @Override
    public QueryParameter[] parameters() {
        QueryParameter[] paramsCpy = new QueryParameter[this.params.size()];
        return this.params.toArray(paramsCpy);
    }

    @Override
    public String id() {
        return condId;
    }

    @Override
    public StringBuilder append(PersistanceManager pm, PersisterDescription desc, StringBuilder sb) {
        String operation = "";

        switch (op)
        {
            case AND:
                operation = "AND ";
                break;
            case OR:
                operation = "OR ";
                break;
        }

        sb.append("( ");

        for(int i = 0; i < conditions.size(); i++) {
            sb = conditions.get(i).append(pm, desc, sb);

            if ( (conditions.size() > 1) && (i + 1 < conditions.size()) ) {
                sb.append(operation);
            }
        }

        sb.append(") ");

        return sb;
    }

    @Override
    public IQueryCondition clone() {
        QueryConditionContainer newContainer = new QueryConditionContainer();

        newContainer.condId = this.condId;
        newContainer.op = this.op;
        for(IQueryCondition cond : this.conditions) {
            newContainer.addCondition(cond.clone());
        }

        return newContainer;
    }

    @Override
    public void setDefaultValues(Map<String, SqlDataField> fields) {
        for(IQueryCondition cond : conditions)
            cond.setDefaultValues(fields);
    }

    public boolean isEmpty() {
        return conditions.size() == 0;
    }

}
