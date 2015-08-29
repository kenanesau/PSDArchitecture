package com.privatesecuredata.arch.mvvm;

import com.privatesecuredata.arch.mvvm.vm.IViewModel;

public interface IWriteModelCommand<T> {
	void execute(IViewModel vm, T val);
}
