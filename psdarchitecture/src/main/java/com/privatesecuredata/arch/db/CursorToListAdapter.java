package com.privatesecuredata.arch.db;

import android.database.Cursor;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Filter;

import com.privatesecuredata.arch.db.query.Query;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;
import com.privatesecuredata.arch.mvvm.vm.EncapsulatedListViewModel.IModelListCallback;
import com.privatesecuredata.arch.mvvm.vm.IDataChangedListener;
import com.privatesecuredata.arch.mvvm.vm.OrderBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * This implementation of IModelListCallback encapsulates a cursor. It directly writes all changes
 * done to the database. The main purpose of this is to hide the database specific stuff to the
 * upper ViewModel-Layer.
 *
 * Usually this type is used within an EncapsulatedListViewModel.
 *
 * @param <M>
 */
public class CursorToListAdapter<M extends IPersistable> implements IModelListCallback<M>
{
	private PersistanceManager pm;
	private Cursor csr;
    private Query query;
    private IDataChangedListener listener;
	private IPersister<M> persister;
	private Class<?> parentClazz;
	private IPersistable parent;
	private Class<M> childClazz;
	private List<ICursorChangedListener> csrListeners = new ArrayList<ICursorChangedListener>();
    private CursorToListAdapterFilter filter;
    private String filteredParamId;
    private IDataChangedListener externalChangeListener;
    private OrderByTerm[] sortOrderTerms;

    private ReentrantLock lock = new ReentrantLock();
    /**
     * Counter for object which failed to load...
     */
    private int failedObjects = 0;

    public CursorToListAdapter(PersistanceManager _pm, ICursorChangedListener listener) {
		this(_pm);
		addCursorChangedListener(listener);
    }

	public CursorToListAdapter(PersistanceManager _pm) {
        this.pm = _pm;
	}
	
	public boolean addCursorChangedListener(ICursorChangedListener listener)
	{
		return csrListeners.add(listener);
	}
	
	public boolean removeCursorChangedListener(ICursorChangedListener listener)
	{
		return csrListeners.remove(listener);
	}

	private Cursor doDbAction() {
        Cursor newCursor = null;
        if (null == query) {
            if (null == parent) {
                newCursor = pm.getCursor(parentClazz, childClazz, sortOrderTerms);
            } else {
                // Only load a cursor if there can be something in the DB
                // If it is a new object (no DB-ID) -> don't load the cursor
                if (((IPersistable) parent).getDbId() != null) {
                    newCursor = pm.getCursor((IPersistable) parent, childClazz, sortOrderTerms);
                }
            }
        } else {
            newCursor = query.run();
        }

        return newCursor;
    }

    private void notifyNewCursorChange(Cursor oldCursor, Cursor newCursor) {
        this.csr = newCursor;

        try {
            for (ICursorChangedListener listener : CursorToListAdapter.this.csrListeners) {
                listener.notifyCursorChanged(csr);
            }
        } catch (Exception e) {
            throw new DBException("Error notifying cursor changes to listeners", e);
        } finally {
            if (null != oldCursor)
                oldCursor.close();
        }
    }

    private void updateCursor() {
        try {
            lock.lock();
            Cursor oldCursor = csr;
            Cursor newCursor = doDbAction();
            notifyNewCursorChange(oldCursor, newCursor);
        }
        finally {
            lock.unlock();
        }
    }

	public Cursor getCursor() { return this.csr; }

    @Override
	public void init(Class<?> parentClazz, Object parent, Class<M> childClazz) {
        this.parentClazz = parentClazz;
        this.parent = (IPersistable) parent;
        if ( (null != query) && (parent instanceof IPersistable) )
            this.query.setForeignKeyParameter((IPersistable) parent);
        this.childClazz = childClazz;
        persister = pm.getPersister(childClazz);

        updateCursor();
	}

    @Override
    public void setSortOrder(OrderBy... orders) {
        if (null != orders) {
            sortOrderTerms = new OrderByTerm[orders.length];
            int i = 0;
            for (OrderBy order : orders)
                sortOrderTerms[i++] = new OrderByTerm(order.getType(), order.getFieldName(), order.isAscending());
        }
        else
            sortOrderTerms = null;
    }

    @Override
	public M get(int pos) {
        try {
            lock.lock();
            if (csr == null) return null;
            boolean retry = false;
            M obj;
            do {
                obj = pm.load(persister, csr, pos);

                if (obj == null) {
                    failedObjects++;
                    pos++;

                    if (pos < csr.getCount())
                        retry = true;
                    else
                        retry = false;
                }
                else
                    retry = false;
            }
            while(retry);

            /**
             * If an object retry to load -> reload the cursor
             *
             * Worst case: This can happen one time for every type the referencing object references.
             */
            if (retry) {
                updateCursor();
                failedObjects = 0;
            }

            return obj;
        }
        catch (Exception e)
        {
            if (query != null)
                throw new DBException(String.format("Error loading data from cursor with SQL-statement \"%s\"", query.getSqlStatement()), e);
            else
                throw new DBException("Error loading data from cursor", e);
        }
        finally {
            lock.unlock();
        }
    }
    public long getPosition(DbId dbId) {
        try {
            lock.lock();
            if (csr == null)
                return -1;

            long pos = -1;
            long id = dbId.getId();
            csr.moveToFirst();
            long i = 0;
            do {
                if (id == csr.getLong(0)) {
                    pos = i;
                    break;
                }
                i++;
            } while (csr.moveToNext());

            return pos;
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public DbId getDbId(int pos) {
        try {
            lock.lock();
            if (null == csr)
                return null;

            csr.moveToPosition(pos);
            DbId dbId = new DbId(this.childClazz, csr.getLong(0));
            return dbId;
        }
        finally {
            lock.unlock();
        }
    }

    @Override
	public int size() {
        try {
            lock.lock();
            return csr != null ? csr.getCount() - failedObjects : 0;
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public Observable<IModelListCallback<M>> loadModel() {
        return null;
    }

    @Override
	public void commitFinished() {
        /**
         * change the cursor -> send the new cursor to all listeners
         */

        updateCursor();
    }

	@Override
	public void remove(M item) {
        pm.delete(item);
        item.setDbId(null);
	}

	@Override
	public void save(Collection<M> items) {

		/**
		 * We need to set the persistable list-items to dirty().
         **/
        for(IPersistable item : items)
        {
            DbId<?> dbId = item.getDbId();
            if (null != dbId) {
                dbId.setDirtyForeignKey();
            }
        }

		if (null != parent) {
            pm.saveAndUpdateForeignKey(items, parent);
        }
		else
			pm.save(items);
	}

    @Override
    public void save(SparseArray<M> items) {
        Collection<M> lst = new ArrayList<M>(items.size());

        for(int i = 0; i < items.size(); i++) {
            lst.add(items.get(items.keyAt(i)));
        }

        save(lst);
    }

    @Override
	public List<M> getList() {
        try {
            lock.lock();
            return csr == null ? new ArrayList<M>() : pm.loadCursor(childClazz, csr);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public Filter getFilter() {
        if (null == filter)
            filter = new CursorToListAdapterFilter(this);

        return filter;
    }

    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        try {
            lock.lock();
            Cursor csr;

            if (null == query) {
                if (this.sortOrderTerms != null)
                    csr = persister.getFilteredCursor(DbNameHelper.getSimpleFieldName(getFilteredParamId()),
                            constraint, sortOrderTerms);
                else
                    csr = persister.getFilteredCursor(DbNameHelper.getSimpleFieldName(getFilteredParamId()),
                            constraint);
            } else {
                query.setParameter(getFilteredParamId(), constraint);
                csr = query.run();
            }
            return csr;
        }
        finally {
            lock.unlock();
        }
    }

    public void changeCursor(Cursor newCursor)
    {
        Cursor oldCursor = null;
        try {
            lock.lock();
            oldCursor = this.csr;

            this.csr = newCursor;

            notifyNewCursorChange(oldCursor, newCursor);
        } finally {
            lock.unlock();
        }

        if (null != externalChangeListener)
            externalChangeListener.notifyDataChanged();
    }

    public String getFilteredParamId() {
        return filteredParamId;
    }

    public void setFilterParamId(String filteredColumn)
    {
        this.filteredParamId = filteredColumn;
    }

    @Override
    public void registerForDataChange(IDataChangedListener listener) {
        externalChangeListener = listener;
    }

    @Override
    public void setQuery(Query q)
    {
        query = q;
    }

    @Override
    public void setQuery(String queryId) {
        query = pm.getQuery(queryId);
        if (null == query)
            throw new ArgumentException(String.format("No Query with id \"%s\" found!", queryId));
    }

    @Override
    public void where(String paramId, Object value) {
        if (null == query)
            throw new ArgumentException("No Query set yet. Use setQueryId() first!");

        query.setParameter(paramId, value);
    }

    public void where(String paramId, Class type) {
        if (null == query)
            throw new ArgumentException("No Query set yet. Use setQueryId() first!");

        query.setParameter(paramId, type);
    }

    @Override
    public void runQuery() {
        changeCursor(query.run());
    }

    @Override
    public void dispose() {
        try {
            lock.lock();
            if (null != csr)
                this.csr.close();
        }
        finally {
            lock.unlock();
        }
    }
}
