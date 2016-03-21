package com.privatesecuredata.arch.db;

/**
 * Created by kenan on 3/11/16.
 */
public interface IConversionDescription {

    Class[][] getEntityMappings();
    Class[][] getObjectConverters();
}
