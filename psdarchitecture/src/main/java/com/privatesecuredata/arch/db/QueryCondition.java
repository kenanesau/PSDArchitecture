package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.exceptions.DBException;

import java.util.Map;

/**
 * Created by kenan on 1/29/16.
 */
public class QueryCondition {

    public enum Operations {
        EQUALS(1),
        NOTEQUAL(2),
        SMALLER(3),
        GREATER(4),
        SMALLEROREQUAL(5),
        GREATEROREQUAL(6);

        private int value;

        private Operations(int value) {
            this.value = value;
        }

    }

    public enum ConditionType {
        VALUE(1),
        TYPE(1);

        private int value;

        private ConditionType(int value) {
            this.value = value;
        }
    }

    private QueryParameter[] params = new QueryParameter[1];
    private String condId;
    private Operations op;
    private ConditionType type = ConditionType.VALUE;

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
        op = Operations.EQUALS;
    }

    public void setTypeCondition() { this.type = ConditionType.TYPE; }
    public boolean isTypeCondition() { return this.type == ConditionType.TYPE; }

    /**
     * Append condition to an SQL-Query
     *
     * @param fields All fields contained in the persister (Map Sql-Fieldname -> SqlDataField)
     * @param sb Stringbuilder which contains the SQL-Query to append the condition to
     * @return SQL-Query with appended condition
     */
    public StringBuilder append(Map<String, SqlDataField> fields, StringBuilder sb) {
        String sqlFieldName = null;
        SqlDataField sqlField = null;
        if (isTypeCondition())
            sqlFieldName = DbNameHelper.getFieldName(params[0].fieldName(), SqlDataField.SqlFieldType.OBJECT_NAME);
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
        }

        sb.append("?");

        return sb;
    }

    public String Id() {
        return condId;
    }

    public Operations op() {
        return op;
    }

    public QueryParameter[] parameters() {
        return params;
    }

    public QueryCondition clone() {
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
}
