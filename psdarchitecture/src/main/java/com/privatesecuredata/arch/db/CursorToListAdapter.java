package com.privatesecuredata.arch.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.database.Cursor;

import com.privatesecuredata.arch.mvvm.vm.EncapsulatedListViewModel.IModelListCallback;

public class CursorToListAdapter<M extends IPersistable<M>> implements IModelListCallback<M>, ICursorChangedProvider
{
	private PersistanceManager pm;
	private Cursor csr;
	private IPersister<M> persister;
	private Class<?> parentClazz;
	private IPersistable<?> parent;
	private Class<M> childClazz;
	private List<ICursorChangedListener> csrListeners = new ArrayList<ICursorChangedListener>();
	
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
		if (null==parent)
			this.csr = pm.getCursor(parentClazz, childClazz);
		else
			this.csr = pm.getCursor((IPersistable<?>)parent, childClazz);
		
		persister = pm.getPersister(childClazz);
		this.parentClazz = parentClazz;
		this.parent = (IPersistable<?>) parent;
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
		init(this.parentClazz, this.parent, this.childClazz);
		
		for(ICursorChangedListener listener : this.csrListeners)
		{
			listener.notifyCursorChanged(csr);
		}
	}

	@Override
	public void remove(M item) {
		pm.delete(item);
	}

	@Override
	public void save(Collection<M> items) {
		
		/**
		 * We don't need to set the persistable items to dirty() since
		 * this was already done by the DBViewModelCommitListeners which
		 * where triggered during super.commit() of the EncapsulatedListViewModel
		 */
//		for(M item : items)
//		{
//			IPersistable persistable = (IPersistable)item; 
//			DbId<?> dbId = persistable.getDbId();
//			if (null != dbId)
//				dbId.setDirty();
//		}
		if (null != parent)
			pm.saveAndUpdateForeignKey(items, parent.getDbId());
		else
			pm.save(items);
	}

	@Override
	public List<M> getList() {
		return pm.loadCursor(childClazz, csr);
	}
}
