package com.privatesecuredata.arch.testdata;

import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.SimpleVmMapping;
import com.privatesecuredata.arch.testdata.vm.TupleVM;

@ComplexVmMapping(vmType = TupleVM.class, vmFactoryType = TupleVM.VmFactory.class)
public class Tuple<U, V> {
	@SimpleVmMapping
	U x;
	@SimpleVmMapping
	V y;
	
	public Tuple(U x, V y)
	{
		this.x = x;
		this.y = y;
	}
	
	public U getX() { return this.x; }
	public V getY() { return this.y; }
	public void setX(U x) { this.x = x; }
	public void setY(V y) { this.y = y;; }
}
