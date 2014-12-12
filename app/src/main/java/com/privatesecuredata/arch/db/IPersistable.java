package com.privatesecuredata.arch.db;

/**
 * 
 * @author Kenan Esau
 * 
 * Interface which has to be implemented by every class whose instances 
 * should be saved to a DB
 *
 * @param <T> The type of the persistable class
 * 
 * @see DbId
 */
public interface IPersistable<T extends IPersistable<T>> {
	DbId<T> getDbId();
	void setDbId(DbId<T> dbId);
}
