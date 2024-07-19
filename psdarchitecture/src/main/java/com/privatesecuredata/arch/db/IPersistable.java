package com.privatesecuredata.arch.db;

/**
 * 
 * @author Kenan Esau
 * 
 * Interface which has to be implemented by every class whose instances 
 * should be saved to a DB
 *
 * @see DbId
 */
public interface IPersistable {
	<T extends IPersistable> DbId<T> getDbId();
	<T extends IPersistable> void setDbId(DbId<T> dbId);
}
