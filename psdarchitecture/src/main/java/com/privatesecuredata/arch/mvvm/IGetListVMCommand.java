package com.privatesecuredata.arch.mvvm;

import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;

public interface IGetListVMCommand<T extends ComplexViewModel> {
	IListViewModel getVM(T container);
}
