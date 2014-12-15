package com.privatesecuredata.arch.mvvm;

/**
 * Interface, implemented by all ViewModels
 * 
 * @author kenan
 *
 * @param <MODEL>
 */
public interface IViewModel<MODEL> extends IViewModelChangedListener {

	/**   
	 * @return Returns the model
	 */
	public abstract MODEL getModel();

	public abstract void addChangedListener(IViewModelChangedListener listener);
	public abstract void delChangedListener(IViewModelChangedListener listener);
	public abstract void notifyChange(IViewModel<?> changedModel, IViewModel<?> originator);
	public abstract void notifyChange();

	public abstract boolean isDirty();
	public abstract void setDirty();
	public abstract void setClean();

	/**
	 * commit all changes from the ViewModel to the Model;
	 */
	public abstract void commit();
	
	/**
	 * reload data from Model to ViewModel
	 */
	public abstract void reload();

	/**
	 * Standard Overrides
	 * @return
	 */
	public abstract int hashCode();
	public abstract boolean equals(Object o);
	public abstract String toString();

}