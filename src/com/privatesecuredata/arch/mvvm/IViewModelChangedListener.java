package com.privatesecuredata.arch.mvvm;

public interface IViewModelChangedListener {
	void notifyChange(IModel<?> vm, IModel<?> originator);
}
