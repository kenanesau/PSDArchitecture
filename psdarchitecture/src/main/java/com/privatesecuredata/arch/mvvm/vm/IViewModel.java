package com.privatesecuredata.arch.mvvm.vm;

import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;

/**
 * Interface, implemented by all ViewModels
 * 
 * @author kenan
 *
 * @param <MODEL>
 */
public interface IViewModel<MODEL> extends IViewModelChangedListener, IModelChangedListener {

	/**   
	 * @return Returns the model
	 */
	MODEL getModel() throws MVVMException;

	void addViewModelListener(IViewModelChangedListener listener);
	void delViewModelListener(IViewModelChangedListener listener);
	void addModelListener(IModelChangedListener listener);
	void delModelListener(IModelChangedListener listener);
	void addListeners(IViewModelChangedListener vmListener, IModelChangedListener modelListener);
	void delListeners(IViewModelChangedListener vmListener, IModelChangedListener modelListener);
	void notifyViewModelDirty(IViewModel<?> changedModel, IViewModelChangedListener originator);
	void notifyViewModelDirty();
    void notifyModelChanged(IViewModel<?> changedModel, IViewModel<?> originator);
    void notifyModelChanged();
	void dispose();

	boolean isDirty();

	/**
	 * commit all changes from the ViewModel to the Model; After commit the state of the model
	 * and the VM are guaranteed to be the same
	 */
	void commit();
    <M extends IViewModel<MODEL>> ObservableTransformer<M, M> applyCommit();
    <M extends IViewModel<MODEL>> Observable<M> commitAsync();
	
	/**
	 * reload data from Model to ViewModel
	 */
	void reload();

	/**
	 * Standard Overrides
	 * @return
	 */
	int hashCode();
	boolean equals(Object o);
	String toString();
}