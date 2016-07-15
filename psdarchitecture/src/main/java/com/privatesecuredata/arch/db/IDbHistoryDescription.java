package com.privatesecuredata.arch.db;

import java.util.Map;

/**
 * Objects implementing IbHistoryDescription link the DB-version to the implementation of
 * IConversionDescription. The IDBHistoryDescription can contain the complete description
 * of all DB-changes from the beginning of time ;-)
 */
public interface IDbHistoryDescription {
    Map<Integer, IDbDescription> getDbDescriptionHistory();
    Map<Integer, IConversionDescription> getDbConversions();

    IDbDescription getDbDescription(int version, int instance);
}
