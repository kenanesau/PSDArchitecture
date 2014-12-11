package com.privatesecuredata.arch.mvvm.vm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.CommitException;
import com.privatesecuredata.arch.mvvm.CommitCommand;
import com.privatesecuredata.arch.mvvm.ICommitCommand;
import com.privatesecuredata.arch.mvvm.IViewModel;

public class SimpleValueVM<T> extends ViewModel<T> implements IViewModel<T> {
	private T data;
	private Class<T> clazz;
	List<ICommitCommand> commitCommands = new ArrayList<ICommitCommand>();
	
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
		init((Class<T>)data.getClass(), data);
	}
	
	public void set(T newData)
	{
		if ( (null == data) || (!this.data.equals(newData)) )
		{
			this.setDirty();
			this.data = newData;
			notifyChange(this, this);
		}
	}
	
	public T get() { return this.data; }
	
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
			notifyChange(this, this);
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
