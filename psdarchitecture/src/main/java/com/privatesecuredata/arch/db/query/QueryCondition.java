package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.DbNameHelper;
import com.privatesecuredata.arch.db.ObjectRelation;
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
        LIKE(7),
        NULL(8),
        NOTNULL(9);

        private int value;

        public int val() {return value; }

        private Operation(int value) {
            this.value = value;
        }

    }

    public enum ConditionType {
        VALUE(1),
        TYPE(2),
        DBID(3),
        FOREIGNKEY(4),
        DIRECTFIELDNAME(5), //No field name mangling through DBName-Helper
        LOADOBJANDCMP(6);  // Load referenced obj and compare to param

        private int value;

        public int val() {return value; }

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
     * Create a query-condition
     *
     * @param fieldName Fieldname of field in the Object
     */
    public QueryCondition(String fieldName) {
        this(fieldName, fieldName);
    }

    /**
     * Create a query-condition
     *
     * @param condId ID of the condition
     * @param fieldName Fieldname of field in the Object
     */
    public QueryCondition(String condId, String fieldName) {
        this(condId, fieldName, fieldName);
    }

    /**
     * Create a query-condition
     *
     * @param condId ID of the condition
     * @param paraId ID of the parameter
     * @param fieldName Fieldname of field in the Object
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
    public void setDirectFieldnameCondition() { this.type = ConditionType.DIRECTFIELDNAME; }
    public boolean isDirectFieldnameCondition() { return this.type == ConditionType.DIRECTFIELDNAME; }
    public void setForeignKeyCondition() { this.type = ConditionType.FOREIGNKEY; }
    public boolean isForeignKeyCondition() { return this.type == ConditionType.FOREIGNKEY; }
    public void setObjEqualsCondition() { this.type = ConditionType.LOADOBJANDCMP; }
    public boolean isObjEqualsCondition() { return this.type == ConditionType.LOADOBJANDCMP; }

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

    protected SqlDataField findField(PersisterDescription desc, String sqlFieldName)
    {
        SqlDataField sqlField = null;

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
            sqlField = desc.getTableField(sqlFieldName);
        }

        return sqlField;
    }

    protected String getSqlFieldName(String objFieldName) {
        String sqlFieldName = null;

        if (isTypeCondition())
            sqlFieldName = DbNameHelper.getFieldName(objFieldName, SqlDataField.SqlFieldType.OBJECT_NAME);
        else if (isForeignKeyCondition() || isDirectFieldnameCondition()) {
            sqlFieldName = params[0].fieldName();
        }
        else
            sqlFieldName = DbNameHelper.getSimpleFieldName(objFieldName);

        return sqlFieldName;
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
        String sqlFieldName = getSqlFieldName(params[0].fieldName());
        SqlDataField sqlField = null;

        if (getPersistableType() == null) {
            sqlField = isDbIdCondition() ?
                    new SqlDataField("_id", SqlDataField.SqlFieldType.LONG) :
                    findField(desc, sqlFieldName);

            if (null == sqlField)
                throw new DBException(String.format("Could not find sqlField with name \"%s\" in description for type \"%s\"",  params[0].fieldName(), desc.getDbTypeName()));

            sb.append(desc.getTableName())
                    .append(".");
        }
        else {
            /**
             * Join- and compose-case
             */
            ObjectRelation objRel = desc.getOneToOneRelation(condId);
            boolean isComposition = ((null != objRel) && (objRel.isComposition()));

            PersisterDescription realDesc = pm.getPersister(getPersistableType()).getDescription();
            if (isComposition) {
                SqlDataField compField = realDesc.getTableField(sqlFieldName);
                SqlDataField actualField = new SqlDataField(objRel.getField(), compField);
                sqlFieldName = actualField.getSqlName();
                sqlField = findField(desc, sqlFieldName);
            }
            else {
                sqlField = findField(realDesc, sqlFieldName);
            }


            if (null == sqlField)
                throw new DBException(String.format("Unable to create query: Could not find sqlField of joined type \"%s\" with name \"%s\"",
                        getPersistableType().getName(),
                        params[0].fieldName()));

            if (!isComposition)
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
            case NULL:
                sb.append(" IS NULL ");
                params = null;
                break;
            case NOTNULL:
                sb.append(" IS NOT NULL ");
                params = null;
                break;
        }

        if ( (op != Operation.NULL) && (op != Operation.NOTNULL) ) {
            sb.append("? ");
            if (params == null)
                params = new QueryParameter[1];
        }

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
        return params != null ? params : new QueryParameter[0];
    }

    @Override
    public IQueryCondition clone() {
        QueryCondition newCond = new QueryCondition();

        if (null != params) {
            QueryParameter[] newParams = new QueryParameter[params.length];
            for (int i = 0; i < params.length; i++) {
                newParams[i] = params[i].clone();
            }
            newCond.params = newParams;
        }
        else {
            newCond.params = null;
        }
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
                String dbFieldName = DbNameHelper.getFieldName(para.fieldName(),
                        SqlDataField.SqlFieldType.OBJECT_NAME);
                if (fields.containsKey(dbFieldName)) {
                    SqlDataField sqlField = fields.get(dbFieldName);
                    para.setValue(sqlField.getObjectField().getType().getName());
                }
            }
        }
    }
}
