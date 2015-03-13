package com.privatesecuredata.arch.mvvm;

import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

public interface IGetVMCommand<T> {
	SimpleValueVM<T> getVM(IViewModel<?> vm);
}
