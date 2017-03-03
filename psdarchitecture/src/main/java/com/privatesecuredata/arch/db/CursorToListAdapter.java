package com.privatesecuredata.arch.db;

import android.database.Cursor;
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

/**
 * This implementation of IModelListCallback encapsulates a cursor. It directly writes all changes
 * done to the database. The main purpose of this is to hide the database specific stuff to the
 * upper ViewModel-Layer.
 *
 * Usually this type is used within an EncapsulatedListViewModel.
 *
 * @param <M>
 */
public class CursorToListAdapter<M extends IPersistable> implements IModelListCallback<M>, ICursorChangedProvider
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
    private OrderByTerm[] sortOrderTerms;

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

    private void updateCursor() {
        Cursor oldCursor = csr;

        if (null == query) {
            if (null == parent) {
                this.csr = pm.getCursor(parentClazz, childClazz, sortOrderTerms);
            } else {
                // Only load a cursor if there can be something in the DB
                // If it is a new object (no DB-ID) -> don't load the cursor
                if (((IPersistable)parent).getDbId() != null) {
                    this.csr = pm.getCursor((IPersistable) parent, childClazz, sortOrderTerms);
                }
            }
        }
        else
        {
            this.csr = query.run();
        }

        try {
            for (ICursorChangedListener listener : this.csrListeners) {
                listener.notifyCursorChanged(csr);
            }
        }
        catch(Exception e) {
            throw new DBException("Error notifying cursor changes to listeners", e);
        }
        finally {
            if (null != oldCursor)
                oldCursor.close();
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
            if (csr == null) return null;
            boolean failed = false;
            M obj;
            do {
                obj = pm.load(persister, csr, pos);

                if (obj == null) {
                    failedObjects++;
                    if (pos < csr.getCount()) {
                        failed = true;
                        pos++;
                    }
                }
                else
                    failed = false;
            }
            while (failed);

            /**
             * If an object failed to load -> reload the cursor
             *
             * Worst case: This can happen one time for every type the referencing object references.
             */
            if (failed) {
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
	}
    public long getPosition(DbId dbId) {
        if (csr == null)
            return -1;

        long pos = -1;
        long id = dbId.getId();
        csr.moveToFirst();
        long i = 0;
        do {
            if (id==csr.getLong(0)) {
                pos = i;
                break;
            }
            i++;
        } while (csr.moveToNext());

        return pos;
    }

    @Override
    public DbId getDbId(int pos) {
        if (null == csr)
            return null;

        csr.moveToPosition(pos);
        DbId dbId = new DbId(this.childClazz, csr.getLong(0));
        return dbId;
    }

    @Override
	public int size() {
		return csr != null ? csr.getCount() - failedObjects : 0;
	}

	@Override
	public void commitFinished() {
        /**
         * init() changes the cursor -> send the new cursor to all listeners
         */
		init(this.parentClazz, this.parent, this.childClazz);
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
		return csr == null ? new ArrayList<M>() : pm.loadCursor(childClazz, csr);
	}

    @Override
    public Filter getFilter() {
        if (null == filter)
            filter = new CursorToListAdapterFilter(this);

        return filter;
    }

    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        Cursor csr;

        if (null == query) {
            if (this.sortOrderTerms != null)
                csr = persister.getFilteredCursor(DbNameHelper.getSimpleFieldName(getFilteredParamId()),
                        constraint, sortOrderTerms);
            else
                csr = persister.getFilteredCursor(DbNameHelper.getSimpleFieldName(getFilteredParamId()),
                        constraint);
        }
        else
        {
            query.setParameter(getFilteredParamId(), constraint);
            csr = query.run();
        }
        return csr;
    }

    public void changeCursor(Cursor newCursor)
    {
        Cursor oldCursor = this.csr;

        this.csr = newCursor;

        try {
            for (ICursorChangedListener listener : this.csrListeners) {
                listener.notifyCursorChanged(csr);
            }

            if (listener != null)
                listener.notifyDataChanged();
        }
        catch(Exception e) {
            throw new DBException("Error changing cursor!", e);
        }
        finally {
            if (null != oldCursor)
                oldCursor.close();
        }
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
        this.listener = listener;
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
        if (null != csr)
            this.csr.close();
    }
}
