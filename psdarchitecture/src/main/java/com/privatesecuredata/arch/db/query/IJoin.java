package com.privatesecuredata.arch.db.query;

/**
 * Created by kenan on 2/3/17.
 */

public interface IJoin {
    Class getJoinedType();
    Class getLocalType();

    String getJoinedFieldName();
    String getLocalFieldName();
}
