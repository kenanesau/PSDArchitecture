package com.privatesecuredata.arch.db;

import java.util.LinkedList;

/**
 * 
 * @author Kenan Esau
 *
 * The DbId wraps the unique database-identifier for the underlying persistable object.
 * Additionally the DbId keeps track of the dirty-status of the persistable object.
 *
 * @param <T> Persistable type
 * @see IPersistable
 */
public class DbId<T extends IPersistable<T>> implements IDirtyChangedListener {
	private boolean dirty = true;
	private long _id = -1;
	private IDirtyChangedListener dirtyListener;
	
	private IPersistable<T> persistableObj;
	private LinkedList<DbId<?>> loadedChildren;
	private LinkedList<DbId<?>> dirtyChildren;
	
//	public DbId(IPersister<? extends IPersistable> _persister)
//	{
//		this.persister = _persister;		
//	}
	
	public DbId(long id)
	{
		this._id = id;
	}
	
	public void setObj(IPersistable<?> persObj)
	{
		this.persistableObj = (IPersistable<T>) persObj;
	}
	public IPersistable<T> getObj()
	{
		return persistableObj;
	}
	
	public boolean getDirty() { return this.dirty; }
	public void setDirty() { this.dirty = true; }
	public void setClean() { this.dirty = false; }
	
	public long getId() { return this._id; }
	
	public void addChild(DbId<?> childId)
	{
		childId.addDirtyChangedListener(this);
		loadedChildren.add(childId);
	}

	private void addDirtyChangedListener(IDirtyChangedListener listener) {
		if ( (null != this.dirtyListener) && (this.getDirty()) )
			this.dirtyListener.removeFromDirtyList(this);
		this.dirtyListener = listener;
	}

	@Override
	public void onDirtyChanged(DbId<?> childId) {
		
		dirtyChildren.add(childId);
	}

	@Override
	public void removeFromDirtyList(DbId<?> childId) {
		if (this.dirtyChildren.contains(childId))
			dirtyChildren.remove(childId);
	}
	
	
}
