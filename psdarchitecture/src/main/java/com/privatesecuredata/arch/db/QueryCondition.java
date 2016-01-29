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

    private QueryParameter[] params = new QueryParameter[1];
    private String condId;
    private Operations op;

    protected QueryCondition() {}

    public QueryCondition(String fieldName) {
        this(fieldName, fieldName);
    }

    public QueryCondition(String condId, String fieldName) {
        this(condId, fieldName, fieldName);
    }

    public QueryCondition(String condId, String paraId, String fieldName) {
        this.params[0] = new QueryParameter(paraId, fieldName);
        this.condId = condId;
        op = Operations.EQUALS;
    }

    public StringBuilder append(Map<String, SqlDataField> fields, StringBuilder sb) {

        SqlDataField field = fields.get(params[0].fieldName());
        if (null == field)
            throw new DBException(String.format("Unable to create query: Could not find field with name \"%s\"", params[0].fieldName()));

        sb.append(field.getSqlName());

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

        return newCond;
    }
}
