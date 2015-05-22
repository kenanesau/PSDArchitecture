package com.privatesecuredata.arch.mvvm;

public interface IDataBinding {
	public <T> void setModelMapping(Class<T> type, int viewId, IGetVMCommand<T> getModelCmd);
}
