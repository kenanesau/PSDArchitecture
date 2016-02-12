package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.db.query.QueryBuilder;

/**
 * 
 * @author Kenan Esau
 *
 * Simple DBDescription which is used by the PersistanceManager to create the 
 * database and decide whether an update is needed or not
 * 
 * @see PersistanceManager
 */
public interface IDbDescription {
    /**
     * @return Returns the base-name of the DB-file
     */
    String getBaseName();

    /**
     * @return Returns the full name of the DB-file (base-name + version + instance)
     * @see com.privatesecuredata.arch.db.AbstractDbDescription
     */
    String getName();

    /**
     * @return Return the version of the DB (positive integer)
     */
    Integer getVersion();

    /**
     * @return Return the number of the instance of the database -- in case you have
     * more than one instance running at the same time
     */
    Integer getInstance();

    /**
     * @return Returns the SQL-create-statements
     */
    String[] getCreateStatements();

    /**
     * @return Returns an array of all classes where hand-made persisters exist
     */
    Class<?>[] getPersisterTypes();

    /**
     * @return Returns an array of classes which can be persisted by an automatic persister
     * (@see AutomaticPersister) which is created by the annotations of the persistable
     * class
     * @see com.privatesecuredata.arch.db.annotations.DbField
     * @see com.privatesecuredata.arch.db.annotations.DbForeignKeyField
     * @see com.privatesecuredata.arch.db.annotations.DbThisToMany
     * @see com.privatesecuredata.arch.db.annotations.DbThisToOne
     */
    Class<?>[] getPersistentTypes();

    /**
     * @return Returns an array of classes which can be used to issue queries
     * @see QueryBuilder
     */
    Class<?>[] getQueryBuilderTypes();
}