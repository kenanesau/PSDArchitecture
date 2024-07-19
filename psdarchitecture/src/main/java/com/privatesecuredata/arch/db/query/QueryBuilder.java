package com.privatesecuredata.arch.db.query;

import android.util.Pair;

import com.privatesecuredata.arch.db.AbstractPersister;
import com.privatesecuredata.arch.db.AutomaticPersister;
import com.privatesecuredata.arch.db.DbNameHelper;
import com.privatesecuredata.arch.db.OrderByTerm;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersisterDescription;
import com.privatesecuredata.arch.db.SqlDataField;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A QueryBuilder can be used to build queries. The Querybuilder is for setting up the
 * structure of the query. Call its createQuery()-method to build a Query-Object which
 * can be used to set the parameter-values and actually run the query
 *
 * @sa com.privatesecuredata.arch.db.query.Query
 * @sa com.privatesecuredata.arch.db.query.QueryCondition
 * @param <T>
 */
public class QueryBuilder<T> {

    public interface IDescriptionGetter {
        PersisterDescription getDescription(PersistanceManager pm);
    }

    public static class DefaultDescriptionGetter implements IDescriptionGetter {
        private Class type;

        public DefaultDescriptionGetter(Class type) {
            this.type = type;
        }

        @Override
        public PersisterDescription getDescription(PersistanceManager pm) {
            AutomaticPersister persister = (AutomaticPersister)pm.getIPersister(type);
            PersisterDescription desc = persister.getDescription();

            return desc;
        }
    }

    private String queryId;
    /* <foreignType, (object)-fieldname> -> Type */
    private LinkedHashMap<IJoin, Class> joins;
    private List<Class> froms = new LinkedList<>();


    /**
     * Condition-ID -> Condition
     */
    private QueryConditionContainer rootCondition = new QueryConditionContainer("root");
    private LinkedList<OrderByTerm> orderByTerms = new LinkedList<>();
    private IDescriptionGetter descriptionGetter;
    private Map<String, QueryParameter> predefinedparams = new LinkedHashMap<>();

    protected QueryBuilder() {}

    public QueryBuilder(Class type, String queryId) {
        this(new DefaultDescriptionGetter(type), queryId);
    }

    public QueryBuilder(IDescriptionGetter getter, String queryId) {
        this.descriptionGetter = getter;
        this.queryId = queryId;
    }

    protected void setDescriptionGetter(IDescriptionGetter getter) {
        this.descriptionGetter = getter;
    }

    public void addCondition(IQueryCondition cond) {
        rootCondition.addCondition(cond);

        IQueryCondition localCondition = cond.clone();
        for (QueryParameter param : localCondition.parameters())
        {
            predefinedparams.put(param.id(), param);
        }
    }

    /**
     * Add a condition for a joined type. Conditions for fields of joined types must be added
     * via this method.
     *
     * @param persistableType joined type
     * @param fldName field name
     */
    public void addCondition(Class persistableType, String fldName) {
        QueryCondition cond = new QueryCondition(fldName);
        cond.setPersistableType(persistableType);
        addCondition(cond);
    }

    public void addCondition(String fldName) {
        addCondition(new QueryCondition(fldName));
    }

    public void addCondition(String condId, String fldName) {
        addCondition(new QueryCondition(condId, fldName));
    }

    public void addCondition(String localObjField, Class referencedType, String referencedObjField) {
        QueryCondition cond = new QueryCondition(localObjField, referencedObjField, referencedObjField);
        cond.setPersistableType(referencedType);
        addCondition(cond);
    }

    public void addTypeCondition(Class persistableType, String condId, String paraId, String fldName) {
        QueryCondition cond = new QueryCondition(condId, paraId, fldName);
        cond.setTypeCondition();
        cond.setPersistableType(persistableType);
        addCondition(cond);
    }

    public void addTypeCondition(String condId, String paraId, String fldName) {
        QueryCondition cond = new QueryCondition(condId, paraId, fldName);
        cond.setTypeCondition();
        addCondition(cond);
    }

    public void addTypeCondition(String condId, String fldName) {
        addTypeCondition(condId, fldName, fldName);
    }

    public void addTypeCondition(Class persistableType, String fldNameAndId) {
        addTypeCondition(persistableType, fldNameAndId, fldNameAndId, fldNameAndId);
    }

    public void addTypeCondition(String fldNameAndId) {
        addTypeCondition(fldNameAndId, fldNameAndId, fldNameAndId);
    }

    public void addLikeCondition(String condId, String paraId, String fldName) {
        QueryCondition cond = new QueryCondition(condId, paraId, fldName);
        cond.setOperation(QueryCondition.Operation.LIKE);
        addCondition(cond);
    }

    public void addLikeCondition(String condId, String fldName) {
        addLikeCondition(condId, fldName, fldName);
    }

    public void addLikeCondition(String fldNameAndId) {
        addLikeCondition(fldNameAndId, fldNameAndId, fldNameAndId);
    }

    public void addForeignKeyCondition(String condId, String paraId, Class foreignKeyType, QueryCondition.Operation op)
    {
        QueryCondition cond = new QueryCondition(condId, paraId, foreignKeyType);
        cond.setForeignKeyCondition();
        cond.setOperation(op);
        addCondition(cond);
    }

    public void addForeignKeyCondition(String condId, String paraId, Class foreignKeyType)
    {
        addForeignKeyCondition(condId, paraId, foreignKeyType, QueryCondition.Operation.EQUALS);
    }

    public void addForeignKeyCondition(Class otherType, String condId, String paraId, Class foreignKeyType, QueryCondition.Operation op)
    {
        QueryCondition cond = new QueryCondition(condId, paraId, foreignKeyType);
        cond.setForeignKeyCondition();
        cond.setPersistableType(otherType);
        cond.setOperation(op);
        addCondition(cond);
    }

    public void addForeignKeyCondition(Class otherType, String condId, String paraId, Class foreignKeyType)
    {
        addForeignKeyCondition(otherType, condId, paraId, foreignKeyType, QueryCondition.Operation.EQUALS);
    }

    public void addForeignKeyCondition(String condId, Class foreignKeyType, QueryCondition.Operation op)
    {
        addForeignKeyCondition(condId, condId, foreignKeyType, op);
    }

    public void addForeignKeyCondition(String condId, Class foreignKeyType)
    {
        addForeignKeyCondition(condId, condId, foreignKeyType);
    }

    public void addForeignKeyCondition(Class otherType, String condId, Class foreignKeyType)
    {
        addForeignKeyCondition(otherType, condId, condId, foreignKeyType);
    }

    public void addForeignKeyCondition(Class foreignKeyType, QueryCondition.Operation op) {
        addForeignKeyCondition(DbNameHelper.getForeignKeyFieldName(foreignKeyType), foreignKeyType, op);
    }

    public void addForeignKeyCondition(Class foreignKeyType)
    {
        addForeignKeyCondition(DbNameHelper.getForeignKeyFieldName(foreignKeyType), foreignKeyType);
    }

    public void addForeignKeyCondition(Class otherType, Class foreignKeyType)
    {
        addForeignKeyCondition(otherType,
                DbNameHelper.getForeignKeyFieldName(otherType, foreignKeyType),
                foreignKeyType);
    }

    public void addForeignKeyCondition(Class otherType, Class foreignKeyType, QueryCondition.Operation op)
    {
        String condId = DbNameHelper.getForeignKeyFieldName(otherType, foreignKeyType);
        addForeignKeyCondition(otherType,
                condId,
                condId,
                foreignKeyType, op);
    }

    /**
     * Add a condition that NOT the same object has to be referenced
     *
     * Type and db-id must match
     *
     * @param otherType Type of a joined table
     * @param fldName Field name
     */
    public void addNotMatchinObjRefCondition(String fldName, Class otherType) {
        QueryConditionContainer cont = createMatchingObjRefCondition(fldName, otherType, QueryCondition.Operation.NOTEQUAL);
        addCondition(cont);
    }

    /**
     * Add a condition that the same object has to be referenced
     *
     * Type and db-id must match
     *
     * @param otherType Type of a joined table
     * @param fldName Field name
     */
    public void addMatchingObjRefCondition(String fldName, Class otherType) {
        QueryConditionContainer cont = createMatchingObjRefCondition(fldName, otherType, QueryCondition.Operation.EQUALS);
        addCondition(cont);
    }

    public static QueryConditionContainer createMatchingObjRefCondition(String fldName, Class otherType, QueryCondition.Operation op)
    {
        if ( !((op == QueryCondition.Operation.EQUALS) ||
                (op == QueryCondition.Operation.NOTEQUAL)) )
            throw new ArgumentException("This operation is not supported");

        QueryCondition typeNameCond = new QueryCondition(DbNameHelper.getFieldName(fldName, SqlDataField.SqlFieldType.OBJECT_REFERENCE));
        typeNameCond.setDirectFieldnameCondition();
        if (null != otherType)
            typeNameCond.setPersistableType(otherType);
        QueryCondition idCond = new QueryCondition(DbNameHelper.getFieldName(fldName, SqlDataField.SqlFieldType.OBJECT_NAME));
        idCond.setDirectFieldnameCondition();
        if (null != otherType)
            idCond.setPersistableType(otherType);
        QueryConditionContainer andContainer = new QueryConditionContainer(fldName + "_obj_ref_cnt", typeNameCond, idCond);

        if (op == QueryCondition.Operation.NOTEQUAL) {
            typeNameCond.setOperation(QueryCondition.Operation.NOTEQUAL);
            idCond.setOperation(QueryCondition.Operation.NOTEQUAL);
            andContainer.setOperation(QueryConditionContainer.Operation.OR);
        }
        return andContainer;
    }

    /**
     * Add a condition that the same object has to be referenced
     *
     * Type and db-id must match
     *
     * @param fldName Field name
     */
    public void addMatchingObjRefCondition(String fldName) {
        addMatchingObjRefCondition(fldName, null);
    }

    /**
     * Add a condition that the DB object to load the object in fldName and compare it to
     * the supplied param
     *
     * TODO: Implementation not finished yet, Setting the parameter value does not work yet
     *
     * @param otherType Corresponding joined/composed Type
     * @param referencingFldName Field name
     * @param op Operation
     */
    public void addEqualsObjCondition(String referencingFldName, Class otherType, QueryCondition.Operation op) {
        if (! ((op.val() == QueryCondition.Operation.EQUALS.val()) ||
                (op.val() == QueryCondition.Operation.NOTEQUAL.val())) ) {
            throw new ArgumentException("This operation is not allowed!");
        }

        /**
         * By default the container has an AND-Operation
         */
        QueryConditionContainer cont = new ObjectEqualsQueryCondition(referencingFldName, otherType);
        if (op == QueryCondition.Operation.NOTEQUAL)
            cont.setOperation(QueryConditionContainer.Operation.OR);

        addCondition(cont);
    }

    /**
     * Add a condition that the DB object to load the object in fldName and compare it to
     * the supplied param
     *
     * TODO: Implementation not finished yet, Setting the parameter value does not work yet
     *
     * @param fldName Field name
     */
    public void addEqualsObjCondition(String fldName, Class otherType) {
        addEqualsObjCondition(fldName, otherType, QueryCondition.Operation.EQUALS);
    }

    /**
     * Add a condition that the DB object to load the object in fldName and compare it to
     * the supplied param
     *
     * TODO: Implementation not finished yet, Setting the parameter value does not work yet
     *
     * @param fldName Field name
     * @param op
     */
    public void addEqualsObjCondition(String fldName, QueryCondition.Operation op) {
        addEqualsObjCondition(fldName, null, op);
    }

    /**
     * Add a condition that the DB object to load the object in fldName and compare it to
     * the supplied param
     *
     * TODO: Implementation not finished yet, Setting the parameter value does not work yet
     *
     * @param fldName Field name
     */
    public void addEqualsObjCondition(String fldName) {
        addEqualsObjCondition(fldName, null, QueryCondition.Operation.EQUALS);
    }

    public void appendOrderByTerm(OrderByTerm term) {
        orderByTerms.addLast(term);
    }

    public void appendOrderByTerm(String fldName, boolean asc) {
        OrderByTerm term = new OrderByTerm(fldName, asc);
        orderByTerms.addLast(term);
    }

    public void appendOrderByTerm(String fldName) {
        appendOrderByTerm(fldName, true);
    }

    public String id() { return queryId; }

    /**
     * Set a predefined parameter for the query. When the query is created via createQuery()
     * these parameter-values will already be set.
     *
     * @param paraId ID of the parameter
     * @param value Value of the parameter
     */
    public void setParameter(String paraId, Object value)
    {
        predefinedparams.get(paraId).setValue(value);
    }

    /**
     * Set a predefined parameter for the query. When the query is created via createQuery()
     * these parameter-values will already be set.
     *
     * @param paraId ID of the parameter
     * @param value Type-Value
     */
    public void setParameter(String paraId, Class value)
    {
        predefinedparams.get(paraId).setValue(DbNameHelper.getDbTypeName(value));
    }

    /**
     * Join the table associated with the type t to the rest of the query
     *
     * @param t Type / Table to join with
     * @param joinedType Type / Table which was already joined with this query(can be null if unique)
     * @param fieldName Name of Field wich contains the reference to t
     */
    public void join(Class t, Class joinedType, String fieldName) {
        if (null == joins)
            joins = new LinkedHashMap<>();

        joins.put(new JoinReference(t, joinedType, fieldName), t);
    }

    /**
     * Join the table associated with the type t to the rest of the query
     *
     * @param t Type / Table to join with
     * @param fieldName Name of Field wich contains the reference to t
     */
    public void join(Class t, String fieldName) {
        join(t, null, fieldName);
    }

    /**
     * Join with the table associated with foreigntype which is also referencing the type
     * associated with this query
     *
     * @param foreignType Type to join with
     */
    public void joinByForeingkey(Class foreignType) {
        if (null == joins)
            joins = new LinkedHashMap<>();

        joins.put(new JoinForeignKey(foreignType), foreignType);
    }

    /**
     * Join with the table associated with foreigntype which is also referencing the type
     * associated with this query
     *
     * @param joinedType Joined type
     * @param foreignType Type to join with
     */
    public void joinByForeingkey(Class joinedType, Class foreignType) {
        if (null == joins)
            joins = new LinkedHashMap<>();

        joins.put(new JoinForeignKey(joinedType, foreignType), foreignType);
    }

    /**
     * Remove join
     *
     * @param foreignType
     * @param fieldName
     */
    public void unjoin(Class foreignType, String fieldName) {
        if (null != joins)
            joins.remove(new Pair(foreignType, fieldName));
    }

    public void from(Class otherType) {
        froms.add(otherType);
    }

    public Query createQuery(PersistanceManager pm) {
        try {
            Query query = new Query(id());
            PersisterDescription desc = this.descriptionGetter.getDescription(pm);

            StringBuilder sb;

            if (froms.size() > 0) {
                List<PersisterDescription> descriptions = new LinkedList<PersisterDescription>();

                descriptions.add(desc);
                for(Class otherType : froms)
                    descriptions.add(pm.getPersister(otherType).getDescription());

                sb = new StringBuilder(AbstractPersister.createSelectAllStatement(descriptions));
            }
            else {
                sb = new StringBuilder(AbstractPersister.createSelectAllStatement(
                        desc.getTableName(),
                        desc.getTableFields(),
                        (OrderByTerm[]) null));
            }

            if (null != joins) {
                for (IJoin join : joins.keySet()) {
                    String otherTable = DbNameHelper.getTableName(join.getJoinedType());
                    sb.append(" JOIN ")
                            .append(otherTable)
                            .append(" ON ")
                            .append(join.getLocalType() == null ?
                                    desc.getTableName() :
                                    DbNameHelper.getTableName(join.getLocalType()))
                            .append(".")
                            .append(join.getLocalFieldName())
                            .append("==")
                            .append(otherTable)
                            .append(".")
                            .append(join.getJoinedFieldName());
                }
            }

            if (!rootCondition.isEmpty()) {
                sb.append(" WHERE ");
                sb = rootCondition.append(pm, desc, sb);
                query.addCondition(rootCondition);

                if (orderByTerms.size() > 0) {
                    OrderByTerm[] termAr = null;
                    termAr = new OrderByTerm[orderByTerms.size()];
                    orderByTerms.toArray(termAr);
                    query.setOrderByTerms(termAr);
                }
            }

            /**
             * Set the predefined parameters
             */
            for (String paramId : predefinedparams.keySet()) {
                QueryParameter para = predefinedparams.get(paramId);
                if (para.value() != null)
                    query.setParameter(paramId, para.value());
            }
            query.prepare(pm, desc, sb.toString());
            /** maybe prohibit future changes...
             query.seal() **/

            return query;
        }
        catch (Exception ex) {
            throw new DBException(String.format("Query \"%s\" was unable to build query string!", id()), ex);
        }
    }

}
