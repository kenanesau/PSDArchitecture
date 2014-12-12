package com.privatesecuredata.arch.mvvm;

import java.lang.reflect.Method;

import com.privatesecuredata.arch.exceptions.ArgumentException;

public abstract class CommitCommand implements ICommitCommand {

	Method commitMethod = null;
	Object complexModel = null;
	
	public CommitCommand(Object complexModel, Method commitMethod)
	{
		if ( (null == complexModel) || (null == commitMethod) )
			throw new ArgumentException("The constructor of CommitComand does not accept parameters with value \"null\"!");
		
		this.complexModel = complexModel;
		this.commitMethod = commitMethod;
	}
	
	public abstract void commit(); 
}
