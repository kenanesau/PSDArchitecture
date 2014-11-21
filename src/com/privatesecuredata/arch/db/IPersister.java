package com.privatesecuredata.arch.db;

import java.util.Collection;

import com.privatesecuredata.arch.exceptions.DBException;

import android.database.Cursor;

/**
 * 
 * @author Kenan Esau
 *
 * A Persister is a class which is capable of persisting a certain Type T
 * to a database. Each class that implements IPersistable has to have one
 * corresponding implementation of IPersister which is responsible for saving
 * the IPersistable-instance to disk.
 *
 * @param <T> Type of the persistable class
 * @see IPersistable
 */
public interface IPersister<T extends IPersistable<T>> extends ILoadCollection<T> {
	void init(Object obj) throws DBException;
	long insert(T persistable) throws DBException;
	long update(T persistable) throws DBException;
	T rowToObject(int pos, Cursor csr) throws DBException;
	Cursor getLoadAllCursor() throws DBException;
	T load(DbId<T> id) throws DBException;	
	Collection<T> loadAll() throws DBException;
	void delete(T persistable) throws DBException;
	void updateForeignKey(T persistable, DbId<?> foreignId) throws DBException;
}
