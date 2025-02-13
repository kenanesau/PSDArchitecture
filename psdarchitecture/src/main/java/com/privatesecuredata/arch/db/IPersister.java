package com.privatesecuredata.arch.db;

import android.database.Cursor;

import com.privatesecuredata.arch.db.query.Query;
import com.privatesecuredata.arch.exceptions.DBException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

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
public interface IPersister<T extends IPersistable> extends ILoadCollection<T> {
	void init(Object obj) throws DBException;
	long insert(T persistable) throws DBException;
	long update(T persistable) throws DBException;
	T rowToObject(int pos, Cursor csr) throws DBException;
    List<SqlDataField> getSqlFields();
	Cursor getLoadAllCursor() throws DBException;
    Cursor getLoadAllCursor(OrderByTerm[] orderTerms) throws DBException;
    Cursor getFilteredCursor(String fieldName, CharSequence constraint) throws DBException;
    Cursor getFilteredCursor(String fieldName, CharSequence constraint,
                                    OrderByTerm[] orderTerms) throws DBException;
	T load(long id) throws DBException;
	Collection<T> loadAll() throws DBException;
	void delete(T persistable) throws DBException;
	void updateForeignKey(DbId<T> persistableId, DbId<?> foreignId) throws DBException;
    long updateCollectionProxySize(DbId<T> persistableId, Field field, long newCollectionSize) throws DBException;

    /**
     * Returns true if a corresponding table in the db exists (If this is a persister
     * for a father-class that might not be the case).
     * @return
     */
    boolean tableExists();
    void addExtendingPersister(AutomaticPersister childPersister);
    List<AutomaticPersister> getExtendingPersisters();
    T createPersistable();

    Query getQuery(String queryId);
    PersisterDescription<T> getDescription();

    void registerAction(PersistanceManager.Action<T> action);
    void unregisterAction(PersistanceManager.Action<T> action);
    boolean hasDeleteActions();
}
