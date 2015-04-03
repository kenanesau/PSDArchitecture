package com.privatesecuredata.arch.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import android.database.Cursor;
import android.widget.Filter;
import com.privatesecuredata.arch.exceptions.DBException;
import com.privatesecuredata.arch.mvvm.vm.EncapsulatedListViewModel.IModelListCallback;

/**
 * This implementation of IModelListCallback encapsulates a cursor. It directly writes
 * all changes done to the database.
 *
 * @param <M>
 */
public class CursorToListAdapter<M extends IPersistable> implements IModelListCallback<M>, ICursorChangedProvider
{
	private PersistanceManager pm;
	private Cursor csr;
	private IPersister<M> persister;
	private Class<?> parentClazz;
	private IPersistable parent;
	private Class<M> childClazz;
	private List<ICursorChangedListener> csrListeners = new ArrayList<ICursorChangedListener>();
    private CursorToListAdapterFilter filter;
    private String filteredColumn;

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
	
	public Cursor getCursor() { return this.csr; }
	
	@Override
	public void init(Class<?> parentClazz, Object parent, Class<M> childClazz) {
		if (null == parent)
			this.csr = pm.getCursor(parentClazz, childClazz);
		else
			this.csr = pm.getCursor((IPersistable)parent, childClazz);

        if (null == csr)
            throw new DBException("Unable to load cursor");
		
		persister = pm.getPersister(childClazz);
		this.parentClazz = parentClazz;
		this.parent = (IPersistable) parent;
		this.childClazz = childClazz;
	}

	@Override
	public M get(int pos) {
		return  pm.load(persister, csr, pos);
	}

	@Override
	public int size() {
		return csr.getCount();
	}

	@Override
	public void commitFinished() {
        Cursor oldCursor = this.csr;

        /**
         * init() changes the cursor -> send the new cursor to all listeners
         */
		init(this.parentClazz, this.parent, this.childClazz);

        try {
            for (ICursorChangedListener listener : this.csrListeners) {
                listener.notifyCursorChanged(csr);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if (null != oldCursor)
                oldCursor.close();
        }
    }

	@Override
	public void remove(M item) {
		pm.delete(item);
	}

	@Override
	public void save(Collection<M> items) {

		/**
		 * We need to set the persistable list-items to dirty().
         **/
        for(IPersistable item : items)
        {
            DbId<?> dbId = item.getDbId();
            if (null != dbId)
                dbId.setDirty();
        }

		if (null != parent) {
            pm.saveAndUpdateForeignKey(items, parent);
        }
		else
			pm.save(items);
	}

	@Override
	public List<M> getList() {
		return pm.loadCursor(childClazz, csr);
	}

    @Override
    public Filter getFilter() {
        if (null == filter)
            filter = new CursorToListAdapterFilter(this);

        return filter;
    }

    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        Cursor csr = persister.getFilteredCursor(getFilteredColumn(), constraint);

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
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if (null != oldCursor)
                oldCursor.close();
        }
    }

    public String getFilteredColumn() {
        return filteredColumn;
    }

    public void setFilteredColumn(String filteredColumn)
    {
        this.filteredColumn = filteredColumn;
    }
}
