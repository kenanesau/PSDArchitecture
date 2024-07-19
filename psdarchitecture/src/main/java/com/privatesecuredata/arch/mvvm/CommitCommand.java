package com.privatesecuredata.arch.mvvm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.privatesecuredata.arch.exceptions.ArgumentException;

public abstract class CommitCommand implements ICommitCommand {

	Field commitField = null;
	Object complexModel = null;
	
	public CommitCommand(Object complexModel, Field commitField)
	{
		if ( (null == complexModel) || (null == commitField) )
			throw new ArgumentException("The constructor of CommitComand does not accept parameters with value \"null\"!");
		
		this.complexModel = complexModel;
		this.commitField = commitField;
	}
	
	public abstract void commit(); 
}
