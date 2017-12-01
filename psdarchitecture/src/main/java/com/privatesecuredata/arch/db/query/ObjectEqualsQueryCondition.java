package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.IPersister;
import com.privatesecuredata.arch.db.ObjectRelation;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersisterDescription;
import com.privatesecuredata.arch.db.SqlDataField;
import com.privatesecuredata.arch.exceptions.ArgumentException;

import java.util.Collection;

/**
 * Created by kenan on 12/1/17.
 */

public class ObjectEqualsQueryCondition extends QueryConditionContainer implements IQueryCondition {

    Class referencedType = null;

    public ObjectEqualsQueryCondition(String localFieldName, Class referencedType) {
        super(localFieldName);

        this.referencedType = referencedType;
        params.addLast(new ObjectEqualsQueryParameter(getCondId(), getCondId()));
    }

    /**
     * Create a query
     *
     * @param condId ID of the condition
     */
    public ObjectEqualsQueryCondition(String condId, QueryCondition.Operation op) {
        super(condId);

        if ( (op != QueryCondition.Operation.EQUALS) ||
                (op != QueryCondition.Operation.NOTEQUAL) )
            throw new ArgumentException("This operation is not supported");

        if (op == QueryCondition.Operation.EQUALS)
            setOperation(Operation.AND);
        else
            setOperation(Operation.OR);
    }

    @Override
    public StringBuilder append(PersistanceManager pm, PersisterDescription desc, StringBuilder sb) {
        ObjectRelation objRel = desc.getOneToOneRelation(getCondId());

        Class otherType = (Class)objRel.getField().getType();
        IPersister referencedPersister = pm.getPersister(otherType);
        PersisterDescription referencedDesc = referencedPersister.getDescription();

        Collection oneToManies = referencedDesc.getOneToManyRelations();
        if (oneToManies.size() > 0)
            throw new ArgumentException("You are trying to compare to an object with a " +
                    "oneToMany-relation, whis is not supported yet");


        Collection<SqlDataField> lst = referencedDesc.getTableFields();
        for (SqlDataField sqlField : lst)
        {
            if (sqlField.getSqlType() == SqlDataField.SqlFieldType.OBJECT_NAME)
                continue;

            String fldName = sqlField.getObjectField().getName();

            IQueryCondition cond = null;
            if (sqlField.getSqlType() == SqlDataField.SqlFieldType.OBJECT_REFERENCE) {
                QueryCondition.Operation op = QueryCondition.Operation.EQUALS;
                if (op() == Operation.OR)
                    op = QueryCondition.Operation.NOTEQUAL;

                cond = QueryBuilder.createMatchingObjRefCondition(fldName, otherType, op);
            }
            else {
                QueryCondition qCond = new QueryCondition(getCondId(),
                        sqlField.getObjectField().getName());
                qCond.setPersistableType(otherType);

                if (op() == Operation.AND)
                    qCond.setOperation(QueryCondition.Operation.EQUALS);
                else
                    qCond.setOperation(QueryCondition.Operation.NOTEQUAL);

                cond = qCond;
            }

            this.addCondition(cond);
        }

        ObjectEqualsQueryParameter oeParam = (ObjectEqualsQueryParameter)params.getFirst();
        oeParam.setDescription(referencedDesc);

        return super.append(pm, desc, sb);
    }
}
