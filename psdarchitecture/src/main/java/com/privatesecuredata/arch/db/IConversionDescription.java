package com.privatesecuredata.arch.db;

/**
 * This interface has to be implemented to do the actual conversion from one DB-model to
 * the next.
 */
public interface IConversionDescription {
    /**
     * This mapping from new to old is used in the process of converting an old
     * database to a new one.
     *
     * @return Get a "2 columned table" of new classes mapped to old classes
     */
    Class[][] getEntityMappings();

    /**
     * This is a mapping of all new classes to object converters.
     *
     * @return Get a "2 columned table" of new types mapped types of object converters
     */
    Class[][] getObjectConverters();

    /**
     * This method will be called on an DB-upgrade. So the user can load the relevant data
     * and do an ease upgrade using the convMan
     *
     * @param convMan
     */
    void convert(ConversionManager convMan);
}
