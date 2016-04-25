package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.AbstractPersister;
import com.privatesecuredata.arch.db.AutomaticPersister;
import com.privatesecuredata.arch.db.OrderByTerm;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersisterDescription;

import java.util.LinkedList;

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

    private Class type;
    private String queryId;
    /**
     * Condition-ID -> Condition
     */
    private QueryConditionContainer rootCondition = new QueryConditionContainer("root");
    private LinkedList<OrderByTerm> orderByTerms = new LinkedList<>();
    private IDescriptionGetter descriptionGetter;

    public QueryBuilder(Class type, String queryId) {
        this(new DefaultDescriptionGetter(type), queryId);
    }

    public QueryBuilder(IDescriptionGetter getter, String queryId) {
        this.descriptionGetter = getter;
        this.queryId = queryId;
    }

    public void addCondition(IQueryCondition cond) {
        rootCondition.addCondition(cond);
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

    public Query createQuery(PersistanceManager pm) {
        Query query = new Query(id());
        PersisterDescription desc = this.descriptionGetter.getDescription(pm);

        StringBuilder sb;
        OrderByTerm[] termAr = null;
        if (orderByTerms.size() > 0) {
            termAr = new OrderByTerm[orderByTerms.size()];
            orderByTerms.toArray(termAr);
            sb = new StringBuilder(
                    AbstractPersister.createSelectAllStatement(
                            desc.getTableName(),
                            desc.getTableFields(),
                            termAr));
        }
        else
            sb = new StringBuilder(AbstractPersister.createSelectAllStatement(
                    desc.getTableName(),
                    desc.getTableFields(),
                    null));

        sb.append(" WHERE ");
        sb = rootCondition.append(desc.getFieldMap(), sb);
        query.addCondition(rootCondition);

        AbstractPersister.appendOrderByString(sb, termAr);

        query.prepare(pm, desc, sb.toString());
        /** maybe prohibit future changes...
        query.seal() **/

        return query;
    }

}
