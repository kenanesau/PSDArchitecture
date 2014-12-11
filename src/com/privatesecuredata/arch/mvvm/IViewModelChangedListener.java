package com.privatesecuredata.arch.mvvm;

public interface IViewModelChangedListener {
	void notifyChange(IViewModel<?> vm, IViewModel<?> originator);
}
