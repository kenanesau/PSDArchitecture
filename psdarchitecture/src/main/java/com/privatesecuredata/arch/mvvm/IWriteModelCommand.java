package com.privatesecuredata.arch.mvvm;

import com.privatesecuredata.arch.mvvm.vm.IViewModel;

public interface IWriteModelCommand<T> {
	void write(IViewModel vm, T val);
}
