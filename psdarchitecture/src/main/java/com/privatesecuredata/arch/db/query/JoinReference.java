package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.DbNameHelper;
import com.privatesecuredata.arch.db.SqlDataField;
import com.privatesecuredata.arch.exceptions.ArgumentException;

/**
 * The "usual" join where one row in a table reference another row in another table (OBJECT_REFERENCE)
 */
public class JoinReference implements IJoin {

    private Class joinedType;
    private Class localtype;
    private String objField;

    public JoinReference(Class typeToJoin, Class alreadyJoinedType, String objFieldname)
    {
        if (null == typeToJoin)
            throw new ArgumentException("Type to join with must not be null!");

        this.joinedType = typeToJoin;
        this.localtype = alreadyJoinedType;
        this.objField = objFieldname;
    }

    @Override
    public Class getJoinedType() {
        return joinedType;
    }

    @Override
    public Class getLocalType() { return localtype; }

    @Override
    public String getJoinedFieldName() {
        return "_id";
    }

    @Override
    public String getLocalFieldName() {
        return DbNameHelper.getFieldName(objField, SqlDataField.SqlFieldType.OBJECT_REFERENCE);
    }
}
