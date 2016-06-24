package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.DbNameHelper;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersisterDescription;
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
        DBID(3),
        FOREIGNKEY(4);

        private int value;

        private ConditionType(int value) {
            this.value = value;
        }
    }

    private QueryParameter[] params = new QueryParameter[1];
    private Operation op;
    private ConditionType type = ConditionType.VALUE;
    private String condId;
    /*
        If this is set, the condition is meant for another table which is joined. this type is the
        persistable type of that other table
     */
    private Class persistableType;

    protected QueryCondition() {}

    /**
     * Constructor for creating a query condition for foreign-key-related queries
     *
     * @param condId ID of the condition
     * @param paraId ID of the parameter
     * @param referencingType The foreign key type
     */
    public QueryCondition(String condId, String paraId, Class<?> referencingType) {
        this.params[0] = new ForeignKeyParameter(paraId, referencingType);
        this.condId = condId;
        op = Operation.EQUALS;
    }

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
    public void setForeignKeyCondition() { this.type = ConditionType.FOREIGNKEY; }
    public boolean isForeignKeyCondition() { return this.type == ConditionType.FOREIGNKEY; }


    /**
     * @return The type for which this condition is meant (only used for joins)
     */
    public Class getPersistableType() {
        return persistableType;
    }

    /**
     * Set the type/table for which this condition is meant (only needed for joins)
     * @param persistableType
     */
    public void setPersistableType(Class persistableType) {
        this.persistableType = persistableType;
    }

    public String getPersistableTypeDb() {
        return DbNameHelper.getDbTypeName(persistableType);
    }

    /**
     * Append condition to an SQL-Query
     *
     * @param pm Reference to the persistance-manager
     * @param desc Description of all fields contained in the persister
     * @param sb Stringbuilder which contains the SQL-Query to append the condition to
     * @return SQL-Query with appended condition
     */
    @Override
    public StringBuilder append(PersistanceManager pm, PersisterDescription desc, StringBuilder sb) {
        String sqlFieldName = null;
        SqlDataField sqlField = null;
        Map<String, SqlDataField> fields = desc.getFieldMap();

        if (isTypeCondition())
            sqlFieldName = DbNameHelper.getFieldName(params[0].fieldName(), SqlDataField.SqlFieldType.OBJECT_NAME);
        else if (isDbIdCondition()) {
            sqlFieldName = params[0].fieldName();
            fields.put("_id", new SqlDataField("_id", SqlDataField.SqlFieldType.LONG));
        }
        else if (isForeignKeyCondition()) {
            sqlFieldName = DbNameHelper.getForeignKeyFieldName(params[0].fieldName());
        }
        else
            sqlFieldName = DbNameHelper.getSimpleFieldName(params[0].fieldName());

        if (getPersistableType() == null) {
            /**
             * "Non-Join"-case
             */
            if (isForeignKeyCondition()) {
                /**
                 * Foreign-Key
                 */
                Class foreignKeyType = ((ForeignKeyParameter)params[0]).getForeignKeyType();
                sqlField = desc.getForeignKeyField(foreignKeyType);
            } else {
                /**
                 * Non-Foreign-Key-field
                 */
                sqlField = fields.get(sqlFieldName);
            }
            if (null == sqlField)
                throw new DBException(String.format("Unable to create query: Could not find sqlField with name \"%s\"", params[0].fieldName()));
        }
        else {
            /**
             * Join-case
             */
            PersisterDescription joinedDesc = pm.getPersister(getPersistableType()).getDescription();
            sqlField = joinedDesc.getTableField(sqlFieldName);
            if (null == sqlField)
                throw new DBException(String.format("Unable to create query: Could not find sqlField of joined type \"%s\" with name \"%s\"",
                        getPersistableType().getName(),
                        params[0].fieldName()));

            sb.append(DbNameHelper.getTableName(getPersistableType()))
                    .append(".");
        }

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
                    para.setValue(sqlField.getObjectField().getType().getName());
                }
            }
        }
    }
}
