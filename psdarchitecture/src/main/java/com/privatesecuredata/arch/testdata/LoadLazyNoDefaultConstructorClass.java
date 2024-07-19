package com.privatesecuredata.arch.testdata;

import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.testdata.vm.LoadLazyNoDefaultConstructorClassVM;
import com.privatesecuredata.arch.testdata.vm.TupleVM;

/**
 * Providing no default constructor, but using lazy initialization has to lead to
 * an error
 * 
 * @author kenan
 */
@ComplexVmMapping(vmType = LoadLazyNoDefaultConstructorClassVM.class)
public class LoadLazyNoDefaultConstructorClass {
	@ComplexVmMapping(vmType = TupleVM.class)
	private Tuple<Integer, Integer> data1;
	@ComplexVmMapping(vmType = TupleVM.class)
	private Tuple<Integer, Integer> data2;
	
	public LoadLazyNoDefaultConstructorClass(Tuple<Integer, Integer> d1, Tuple<Integer, Integer> d2)
	{
		this.data1 = d1;
		this.data2 = d2;
	}
}
