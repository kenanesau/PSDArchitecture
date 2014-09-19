package com.privatesecuredata.arch.db;

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
	String getName();
	Integer getVersion();
	Integer getInstance();
	String[] getCreateStatements();
	Class<?>[] getPersisterClasses();
}
