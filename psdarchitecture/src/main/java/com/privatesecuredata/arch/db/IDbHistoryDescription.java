package com.privatesecuredata.arch.db;

import java.util.Map;

/**
 * Created by kenan on 3/21/16.
 */
public interface IDbHistoryDescription {
    Map<Integer, IDbDescription> getDbDescriptionHistory();
    Map<Integer, IConversionDescription> getDbConversions();

    IDbDescription getDbDescription(int version, int instance);
}
