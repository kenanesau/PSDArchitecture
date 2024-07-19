package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.DbNameHelper;

/**
 * Join current table with another table wich it has a foreign key relation with
 */

public class JoinForeignKey implements IJoin {

    private Class foreignType;
    private Class localType;

    public JoinForeignKey(Class foreignType) {
        this.foreignType = foreignType;
        this.localType = null;
    }

    public JoinForeignKey(Class joinedType, Class foreignType) {
        this.foreignType = foreignType;
        this.localType = joinedType;
    }

    @Override
    public Class getJoinedType() {
        return foreignType;
    }

    @Override
    public String getJoinedFieldName() {
        return "_id";
    }

    @Override
    public String getLocalFieldName() {
        return DbNameHelper.getForeignKeyFieldName(foreignType);
    }

    @Override
    public Class getLocalType() {
        return localType;
    }

}
