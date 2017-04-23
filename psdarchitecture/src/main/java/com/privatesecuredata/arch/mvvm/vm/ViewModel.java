	package com.privatesecuredata.arch.mvvm.vm;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;

import java.util.ArrayList;
import java.util.Collection;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

    /**
 * @author kenan
 *
 * Base for all ViewModel-Classes
 * 
 * @see SimpleValueVM, ListViewModel
 */
public abstract class ViewModel<MODEL> implements IViewModelChangedListener, IViewModel<MODEL> {
	
	private Collection<IViewModelChangedListener> viewModelChangeListeners = new ArrayList<IViewModelChangedListener>();
	private Collection<IModelChangedListener> modelChangeListeners = new ArrayList<IModelChangedListener>();
	private boolean isDirty = false;
	private MODEL model;

    /**
	 * Sets the model
	 */
	public void setModel (MODEL m) { this.model = m; }
	
	@Override
	public MODEL getModel() throws MVVMException { return this.model; }
	public boolean hasModel() { return (model != null);}

	@Override
	public void addViewModelListener(IViewModelChangedListener listener) {
        if (listener == this)
            throw new ArgumentException("Cannot register viewmodel-listener with itself!!!");

		if (null != listener)
			this.viewModelChangeListeners.add(listener);
	}

	@Override
	public void delViewModelListener(IViewModelChangedListener listener)
	{
		this.viewModelChangeListeners.remove(listener);
	}

	@Override
	public void addModelListener(IModelChangedListener listener) {
        if (listener == this)
            throw new ArgumentException("Cannot register model-listener with itself!!!");

		if (null != listener)
			this.modelChangeListeners.add(listener);
	}

	@Override
	public void delModelListener(IModelChangedListener listener)
	{
		if (null != listener)
			this.modelChangeListeners.remove(listener);
	}

	@Override
	public void addListeners(IViewModelChangedListener vmListener, IModelChangedListener modelListener)
	{
		addViewModelListener(vmListener);
		addModelListener(modelListener);
	}

	@Override
	public void delListeners(IViewModelChangedListener vmListener, IModelChangedListener modelListener)
	{
		delViewModelListener(vmListener);
		delModelListener(modelListener);
	}

	protected void notifyChangeListeners(IViewModel<?> vm, IViewModelChangedListener originator)
	{
		this.setDirty();
		for(IViewModelChangedListener listener : viewModelChangeListeners)
			if (originator != listener) listener.notifyViewModelDirty(this, originator);
	}

    @Override
    public void notifyViewModelDirty(IViewModel<?> changedViewModel, IViewModelChangedListener originator)
    {
        notifyChangeListeners(changedViewModel, originator);
    }
	
	@Override
	public void notifyViewModelDirty()
	{
		this.notifyViewModelDirty(this, this);
	}

	/**
	 * Called at the end of the commit when all VM-changes have been commited to the model
	 * @param changedViewModel Changed VM
	 * @param originator	   VM which initiated this call
     */
    @Override
    public void notifyModelChanged(IViewModel<?> changedViewModel, IViewModel<?> originator)
    {
        /**
         * Update of Adapter data-source and the appropriate notifyDataSetchanged have to be
         * called from the main-thread...
         */
        Observable.just(originator)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<IViewModel<?>>() {
                               @Override
                               public void accept(IViewModel<?> vm) throws Exception {
                                   ViewModel.this.setClean();
                                   for (IModelChangedListener listener : modelChangeListeners)
                                       listener.notifyModelChanged(ViewModel.this, vm);
                               }
                           });

    }

    /**
     * Called at the end of the commit when all VM-changes have been commited to the model
     */
    @Override
    public void notifyModelChanged()
    {
        this.notifyModelChanged(this, this);
    }
	
	@Override
	public boolean isDirty() { return this.isDirty; }

	protected void setDirty() { this.isDirty = true; }

	protected void setClean() { this.isDirty = false; }

	/**
	 * Write data from ViewModel to Model
	 */
	protected abstract void commitData();
	
	@Override
	public void commit() 
	{
        if (this.isDirty()) {
            this.commitData();
            this.notifyModelChanged();
        }
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
