package com.privatesecuredata.arch.mvvm;

public interface IDataBinding {
	public <T> void addModelMapping(Class<T> type, int viewId, IGetModelCommand<T> getModelCmd);
}
