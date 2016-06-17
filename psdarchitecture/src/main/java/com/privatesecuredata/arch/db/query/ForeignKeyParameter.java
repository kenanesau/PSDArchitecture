package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.DbNameHelper;

/**
 * Created by kenan on 6/17/16.
 */
public class ForeignKeyParameter extends QueryParameter {
    private Class foreignKeyType;

    public ForeignKeyParameter(String paraId, Class foreignKeyType) {
        super(paraId, DbNameHelper.getTableName(foreignKeyType));
        this.foreignKeyType = foreignKeyType;
    }

    Class getForeignKeyType() { return foreignKeyType; }
}
