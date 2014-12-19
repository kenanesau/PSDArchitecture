package com.privatesecuredata.arch.mvvm.vm;

import java.util.ArrayList;
import java.util.Collection;

import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IViewModel;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;
import com.privatesecuredata.arch.mvvm.ViewModelCommitHelper;

/**
 * @author kenan
 *
 * Base for all ViewModel-Classes
 * 
 * @see SimpleValueVM, ListViewModel
 */
public abstract class ViewModel<MODEL> implements IViewModelChangedListener, IViewModel<MODEL> {
	
	private Collection<IViewModelChangedListener> changeListeners = new ArrayList<IViewModelChangedListener>();
	private boolean isDirty = false;
	private MODEL model;
	
	/**
	 * Sets the model
	 */
	protected void setModel (MODEL m) { this.model = m; }
	
	@Override
	public MODEL getModel() throws MVVMException { return this.model; }

	
	@Override
	public void addViewModelListener(IViewModelChangedListener listener)
	{
		this.changeListeners.add(listener);
	}
	
	@Override
	public void delViewModelListener(IViewModelChangedListener listener)
	{
		this.changeListeners.remove(listener);
	}
	
	@Override
	public void notifyViewModelDirty(IViewModel<?> changedModel, IViewModel<?> originator)
	{
		this.setDirty();
		for(IViewModelChangedListener listener : changeListeners)
			listener.notifyViewModelDirty(this, originator);
	}
	
	@Override
	public void notifyViewModelDirty()
	{
		this.notifyViewModelDirty(this, this);
	}

    @Override
    public void notifyModelChanged(IViewModel<?> changedModel, IViewModel<?> originator)
    {
        this.setClean();
        for(IViewModelChangedListener listener : changeListeners)
            listener.notifyModelChanged(this, originator);
    }

    @Override
    public void notifyModelChanged()
    {
        this.notifyModelChanged(this, this);
    }
	
	
	@Override
	public boolean isDirty() { return this.isDirty; }
	@Override
	public void setDirty() { this.isDirty = true; }
	@Override
	public void setClean() { this.isDirty = false; }

	/**
	 * Write data from ViewModel to Model
	 */
	protected abstract void commitData();
	
	@Override
	public void commit() 
	{
		this.commitData();
        this.notifyModelChanged();
		ViewModelCommitHelper.notifyCommit(this);
	}
	
	/**
	 * (re)load all data from the Model to the ViewModel 
	 */
	public abstract void reload(); 
	@Override
	public abstract int hashCode();
	@Override
	public abstract boolean equals(Object o);
	@Override
	public abstract String toString();
}
