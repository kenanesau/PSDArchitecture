package com.privatesecuredata.arch.mvvm.vm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.CommitException;
import com.privatesecuredata.arch.mvvm.CommitCommand;
import com.privatesecuredata.arch.mvvm.ICommitCommand;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;

/**
 * Class to encapsulate primitive values. All ComplexViewModels are composed of SimpleValueVMs
 * (+ other VMs)
 * @param <T> The type of the value to be encapsulated
 * @see ComplexViewModel
 */
public class SimpleValueVM<T> extends ViewModel<T> implements IViewModel<T> {
    /**
     * Interface for specifying a validator-CB to run before setting the value
     * of the VM. If the validate-function returns false the value is not set in
     * the VM.
     * @param <T> The type of value
     */
	public interface IValidator<T> {
        /**
         * Validation function. If this function returns false, the value newData is
         * not set in the VM
         * @param oldData Data which is currently in the VM
         * @param newData Data which should be set in the VM if the validation succeeds
         * @return true if the validation succeeds, false otherwise
         */
		boolean validate(T oldData, T newData);
	}

    /**
     * Interface for specifying a filter-callback which is run on each get()-call on the
     * VM. Using the CB the returned value can be modified. Use this for example for setting
     * a default-value for empty VMs.
     * @param <T> Type of the value
     */
	public interface IDefaultFilter<T>
	{
        /**
         * This function is called on each get()-of the VM. Its return-value is returned by
         * the get()-function of the VM.
         * @param data Data currently in the VM
         * @return New return-value for the get()-function of the VM
         */
		T filter(T data);
	}

	private T data;
	private Class<T> clazz;
	private List<ICommitCommand> commitCommands = new ArrayList<ICommitCommand>();
	private IValidator<T> validator = null;
	private IDefaultFilter<T> filter = null;
	
	private void init(Class<T> clazz, T data)
	{
		this.clazz = clazz;
		this.setModel(data);
		this.data = data;
	}
	
	public SimpleValueVM(Class<T> clazz, T data)
	{
		this.init(clazz, data);
	}
	
	public SimpleValueVM(T data)
	{
		if (null==data)
			throw new ArgumentException("Parameter \"data\" must not be null. Use SimpleValueVM(Class<T>, T) if you want to cope with null values.");
		init((Class<T>) data.getClass(), data);
	}

	public void setValidator(IValidator<T> validator) {
		this.validator = validator;
	}

	public void set(T newData, IViewModelChangedListener originator) throws ArgumentException
	{
		if ( (null == data) || (!this.data.equals(newData)) )
		{
			if ( (null == validator ? true : validator.validate(this.data, newData)) ) {
				this.setDirty();
				this.data = newData;
				notifyChangeListeners(this, originator);
			}
			else {
				throw new ArgumentException("Validation failed!!!");
			}
		}
	}

	public void set(T newData) throws ArgumentException
	{
		set(newData, null);
	}

	public void setDefaultFilter(IDefaultFilter<T> filter)
	{
		this.filter = filter;
	}

	public T get() {  return filter != null ? filter.filter(this.data) : this.data; }
	
	public void RegisterCommitCommand(ICommitCommand command)
	{
		commitCommands.add(command);		
	}
	
	public void RegisterCommitCommand(final Object complexModel, final Method commitMethod)
	{
		commitCommands.add( new CommitCommand(complexModel, commitMethod) {
			
			@Override
			public void commit() {
				try {
					commitMethod.invoke(complexModel, SimpleValueVM.this.get());
				} catch (Exception e) {
					throw new CommitException(String.format("Error committing data via Method \"%s\"", commitMethod.getName()), e);
				}
			}
		});		
	}
	
	@Override
	public void commitData() 
	{
		this.setModel(data);
				
		for(ICommitCommand cmd : commitCommands)
			cmd.commit();
	}
	
	@Override
	public void reload() 
	{
		
		if (!data.equals(getModel()))
		{
			this.data = getModel();
			notifyViewModelDirty(this, this);
		}
	}
	
	public Class<T> getContentClass()
	{
		return this.clazz;
	}
	
	public Class<SimpleValueVM<T>> getVMClass()
	{
		return (Class<SimpleValueVM<T>>) this.getClass();
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(this.data); 
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o.getClass().isInstance(this)) {
			SimpleValueVM<T> that = (SimpleValueVM<T>) o;
			if (this.data==that.data)
				return true;
			
			return (this.data==null ? false : this.data.equals(that.data));
		}
		else {
			return false;
		}
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("data", (data==null ? "null" : this.data.toString()))
				.add("model",(this.getModel()==null ? "null" : this.getModel().toString()))
				.toString();

	}
}
