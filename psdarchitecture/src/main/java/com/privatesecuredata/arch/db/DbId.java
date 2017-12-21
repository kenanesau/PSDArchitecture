package com.privatesecuredata.arch.db;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

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
public class DbId<T extends IPersistable> implements IDirtyChangedListener {
	private boolean dirty = true;
    private boolean dirtyForeignKey = false; //Parent container changed
    private Class type = null;
	private long id = -1;
	private boolean isComposition = false;
	private IDirtyChangedListener dirtyListener;
	
	private T persistableObj;
	private LinkedList<DbId<?>> loadedChildren;
	private LinkedList<DbId<?>> dirtyChildren;
	
	public DbId(Class type, long id)
	{
        this.type = type;
		this.id = id;
	}
	
	public void setObj(T persObj)
	{
		this.persistableObj = persObj;
	}
	public T getObj()
	{
		return persistableObj;
	}
    public Class<?> getType() { return this.type; }
	
	public boolean getDirty() { return this.dirty; }
	public void setDirty() {
	    if (!isComposition) //Compositions are NEVER dirty -- they are saved with their "parents"
	        this.dirty = true;
	}
	public void setClean() {
        this.dirty = false;
    }
    public boolean getDirtyForeignKey() { return this.dirtyForeignKey; }
    public void setDirtyForeignKey() { this.dirtyForeignKey = true; }
    public void setCleanForeignKey() { this.dirtyForeignKey = false; }
    public boolean isComposition() { return this.isComposition; }
    public void setComposition() { this.isComposition = true; }
	
	public long getId() { return this.id; }

    public void addChild(DbId<?> childId)
	{
		childId.addDirtyChangedListener(this);
		if (null == loadedChildren)
			loadedChildren = new LinkedList<DbId<?>>();
			
		loadedChildren.add(childId);
	}

	private void addDirtyChangedListener(IDirtyChangedListener listener) {
		if ( (null != this.dirtyListener) && (this.getDirty()) )
			this.dirtyListener.removeFromDirtyList(this);
		this.dirtyListener = listener;
	}

	@Override
	public void onDirtyChanged(DbId<?> childId) 
	{
		if (null == dirtyChildren)
		    dirtyChildren = new LinkedList<DbId<?>>();
		
		dirtyChildren.add(childId);
	}

	@Override
	public void removeFromDirtyList(DbId<?> childId) {
		if (this.dirtyChildren.contains(childId))
			dirtyChildren.remove(childId);
	}

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("dirty", dirty)
                .add("type", ((persistableObj == null) ? "unknown" : persistableObj.getClass().getName()))
                .toString();
    }

	@Override
	public int hashCode() {
		return Objects.hashCode(id, type);
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (null == o) {
            return false;
        }
        if (o instanceof DbId) {
            DbId that = (DbId) o;
            return Objects.equal(this.id, that.id) &&
                    Objects.equal(this.type, that.type);
        }
        else {
            return false;
        }
    }
}
