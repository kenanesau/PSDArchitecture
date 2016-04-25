package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.DbNameHelper;
import com.privatesecuredata.arch.db.SqlDataField;
import com.privatesecuredata.arch.exceptions.DBException;

import java.util.Map;

/**
 * An ordinary Query-condition. All QueryConditions of a Query result in the where-clauses
 * of the SQL-statement.
 *
 * A QueryCondition can either be a where-clause for an ordinary field/value or for an
 * reference/type.
 */
public class QueryCondition implements IQueryCondition {

    public enum Operation {
        EQUALS(1),
        NOTEQUAL(2),
        SMALLER(3),
        GREATER(4),
        SMALLEROREQUAL(5),
        GREATEROREQUAL(6),
        LIKE(7);

        private int value;

        private Operation(int value) {
            this.value = value;
        }

    }

    public enum ConditionType {
        VALUE(1),
        TYPE(2),
        DBID(3);

        private int value;

        private ConditionType(int value) {
            this.value = value;
        }
    }

    private QueryParameter[] params = new QueryParameter[1];
    private Operation op;
    private ConditionType type = ConditionType.VALUE;
    private String condId;

    protected QueryCondition() {}

    /**
     * Create a query
     *
     * @param fieldName Fieldname of field in Object
     */
    public QueryCondition(String fieldName) {
        this(fieldName, fieldName);
    }

    /**
     * Create a query
     *
     * @param condId ID of the condition
     * @param fieldName Fieldname of field in Object
     */
    public QueryCondition(String condId, String fieldName) {
        this(condId, fieldName, fieldName);
    }

    /**
     * Create a query
     *
     * @param condId ID of the condition
     * @param paraId ID of the parameter
     * @param fieldName Fieldname of field in Object
     */
    public QueryCondition(String condId, String paraId, String fieldName) {
        this.params[0] = new QueryParameter(paraId, fieldName);
        this.condId = condId;
        op = Operation.EQUALS;
        if (fieldName.equals("_id"))
        {
            setDbIdCondition();
        }
    }

    public void setDbIdCondition() { this.type = ConditionType.DBID; }
    public boolean isDbIdCondition() { return this.type == ConditionType.DBID; }
    public void setTypeCondition() { this.type = ConditionType.TYPE; }
    public boolean isTypeCondition() { return this.type == ConditionType.TYPE; }

    /**
     * Append condition to an SQL-Query
     *
     * @param fields All fields contained in the persister (Map Sql-Fieldname -> SqlDataField)
     * @param sb Stringbuilder which contains the SQL-Query to append the condition to
     * @return SQL-Query with appended condition
     */
    @Override
    public StringBuilder append(Map<String, SqlDataField> fields, StringBuilder sb) {
        String sqlFieldName = null;
        SqlDataField sqlField = null;
        if (isTypeCondition())
            sqlFieldName = DbNameHelper.getFieldName(params[0].fieldName(), SqlDataField.SqlFieldType.OBJECT_NAME);
        else if (isDbIdCondition()) {
            sqlFieldName = params[0].fieldName();
            fields.put("_id", new SqlDataField("_id", SqlDataField.SqlFieldType.LONG));
        }
        else
            sqlFieldName = DbNameHelper.getSimpleFieldName(params[0].fieldName());

        sqlField = fields.get(sqlFieldName);
        if (null == sqlField)
            throw new DBException(String.format("Unable to create query: Could not find sqlField with name \"%s\"", params[0].fieldName()));

        sb.append(sqlField.getSqlName());

        switch (op)
        {
            case EQUALS:
                sb.append(" = ");
                break;
            case NOTEQUAL:
                sb.append(" != ");
                break;
            case SMALLER:
                sb.append(" < ");
                break;
            case GREATER:
                sb.append(" > ");
                break;
            case SMALLEROREQUAL:
                sb.append(" <= ");
                break;
            case GREATEROREQUAL:
                sb.append(" >= ");
                break;
            case LIKE:
                sb.append(" LIKE ");
                break;
        }

        sb.append("? ");

        return sb;
    }

    public Operation op() {
        return op;
    }

    public void setOperation(Operation op)
    {
        this.op = op;
        if (Operation.LIKE == this.op)
            this.params[0].setValueFilter(
                    new IQueryParamValueFilter() {
                        @Override
                        public Object filterValue(Object oldValue) {
                            return String.format("%%%s%%", oldValue.toString());
                        }
                    }
            );
        else
            this.params[0].setValueFilter(null);
    }

    @Override
    public String id() { return condId; }

    @Override
    public QueryParameter[] parameters() {
        return params;
    }

    @Override
    public IQueryCondition clone() {
        QueryCondition newCond = new QueryCondition();

        QueryParameter[] newParams = new QueryParameter[params.length];
        for(int i = 0; i < params.length; i++)
        {
            newParams[i] = params[i].clone();
        }
        newCond.params = newParams;
        newCond.condId = this.condId;
        newCond.op = this.op;
        newCond.type = this.type;

        return newCond;
    }

    @Override
    public void setDefaultValues(Map<String, SqlDataField> fields) {
        if (!isTypeCondition()) {
            if (op() == QueryCondition.Operation.LIKE)
            {
                for (QueryParameter para : parameters())
                    para.setValue("");
            }
        }

        for (QueryParameter para : parameters()) {
            /**
             * Set default if no value set yet
             */
            if (null == para.value()) {
                String dbFieldName = DbNameHelper.getFieldName(para.fieldName(), SqlDataField.SqlFieldType.OBJECT_NAME);
                if (fields.containsKey(dbFieldName)) {
                    SqlDataField sqlField = fields.get(dbFieldName);
                    para.setValue(sqlField.getField().getType().getName());
                }
            }
        }
    }
}
