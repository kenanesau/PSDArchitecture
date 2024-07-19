package com.privatesecuredata.arch.testdata.vm;

import java.util.HashMap;

import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.testdata.LoadLazyNoDefaultConstructorClass;

public class LoadLazyNoDefaultConstructorClassVM extends
		ComplexViewModel<LoadLazyNoDefaultConstructorClass> {
	
	TupleVM<Integer, Integer> d1VM;
	public LoadLazyNoDefaultConstructorClassVM(MVVM mvvm, LoadLazyNoDefaultConstructorClass model)
	{
        super(mvvm, model);
		HashMap<String, IViewModel<?>> children = getNameViewModelMapping();
		d1VM = (TupleVM<Integer, Integer>)children.get("data1");
	}


	
	public TupleVM<Integer, Integer> getD1VM() { return d1VM; } 
}
