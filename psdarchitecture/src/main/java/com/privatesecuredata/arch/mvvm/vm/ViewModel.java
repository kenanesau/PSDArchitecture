	package com.privatesecuredata.arch.mvvm.vm;

import android.util.Log;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

    /**
 * @author kenan
 *
 * Base for all ViewModel-Classes
 *
 * @see SimpleValueVM, ListViewModel
 */
public abstract class ViewModel<MODEL> implements IViewModelChangedListener, IViewModel<MODEL> {

	private HashMap<Integer, IViewModelChangedListener> viewModelChangeListeners = new HashMap<>();
	private HashMap<Integer, IModelChangedListener> modelChangeListeners = new HashMap<>();
	private boolean isDirty = false;
	private MODEL model;
    private String name;
    private ReentrantLock vmLock = new ReentrantLock();

    /**
	 * Sets the model
	 */
	public void setModel (MODEL m) { this.model = m; }

    protected Collection<IModelChangedListener> getModelListeners() {
        return new ArrayList<>(modelChangeListeners.values());
    }

    protected Collection<IViewModelChangedListener> getViewModelListeners() {
        return new ArrayList<>(viewModelChangeListeners.values());
    }

	@Override
	public MODEL getModel() throws MVVMException { return this.model; }
	public boolean hasModel() { return (model != null);}

	@Override
	public void addViewModelListener(IViewModelChangedListener listener) {
        try {
            vmLock.lock();
            if (listener == this)
                throw new ArgumentException("Cannot register viewmodel-listener with itself!!!");

            if ( (null != listener) && (!this.viewModelChangeListeners.containsKey(System.identityHashCode(listener))) )
                this.viewModelChangeListeners.put(System.identityHashCode(listener), listener);
        }
        finally {
            vmLock.unlock();
        }
    }

	@Override
	public void delViewModelListener(IViewModelChangedListener listener)
	{
        try {
            vmLock.lock();
            this.viewModelChangeListeners.remove(listener);
        }
        finally {
            vmLock.unlock();
        }
    }

	@Override
	public void addModelListener(IModelChangedListener listener) {
        try {
            vmLock.lock();
            if (listener == this)
                throw new ArgumentException("Cannot register model-listener with itself!!!");

            if ( (null != listener) && (!this.modelChangeListeners.containsKey(System.identityHashCode(listener))) )
                this.modelChangeListeners.put(System.identityHashCode(listener), listener);
        }
        finally {
            vmLock.unlock();
        }
	}

	@Override
	public void delModelListener(IModelChangedListener listener)
	{
        try {
            vmLock.lock();
            if (null != listener)
                this.modelChangeListeners.remove(listener);
        }
        finally {
            vmLock.unlock();
        }
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
        try {
            vmLock.lock();
            this.setDirty();
            for (IViewModelChangedListener listener : viewModelChangeListeners.values())
                if (originator != listener) listener.notifyViewModelDirty(this, originator);
        }
        finally {
            vmLock.unlock();
        }
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
        try {
            vmLock.lock();
            /**
             * Update of Adapter data-source and the appropriate notifyDataSetchanged have to be
             * called from the main-thread...
             */

            ViewModel.this.setClean();
            for (IModelChangedListener listener : modelChangeListeners.values())
                listener.notifyModelChanged(ViewModel.this, originator);
        }
        finally {
            vmLock.unlock();
        }
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
            this.startCommit();
            this.commitData();
            this.finishCommit();
        }
	}

    public <M extends IViewModel<MODEL>> ObservableTransformer<M, M> applyCommit() {
        return new ObservableTransformer<M, M> () {

            @Override
            public ObservableSource<M> apply(Observable<M> upstream) {
                return upstream.map(new Function<M, M>() {
                    @Override
                    public M apply(M vm) throws Exception {
                        if (vm.isDirty()) {
                            ((ViewModel)vm).startCommit();
                            ((ViewModel)vm).commitData();
                        }
                        return vm;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<M, M>() {
                    @Override
                    public M apply(M vm) throws Exception {
                        ((ViewModel)vm).finishCommit();
                        return vm;
                    }
                });
            }
        };
    }

    protected void startCommit() {}
    protected void finishCommit() {
	    notifyModelChanged();
	    /*Observable.just(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( (vm) -> vm.notifyModelChanged());*/

    }

	@Override
	public <M extends IViewModel<MODEL>> Observable<M> commitAsync() {
		return Observable.defer(new Callable<ObservableSource<M>>() {
            @Override
            public ObservableSource<M> call() throws Exception {
                return Observable.just((M)ViewModel.this);
            }
        })
        .compose(applyCommit());
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

    public String getName() {
        return this.model != null ? model.getClass().getName() : "";
    }
}
