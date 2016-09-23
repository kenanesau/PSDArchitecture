package com.privatesecuredata.arch.testdata.vm;

import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.testdata.Tuple;

import java.util.HashMap;

public class TupleVM<U, V> extends ComplexViewModel<Tuple<U, V>> {

    public static class VmFactory implements ComplexViewModel.VmFactory<TupleVM, Tuple> {

        @Override
        public TupleVM create(MVVM mvvm, Tuple m) {
            return new TupleVM(mvvm, m);
        }
    }

	private SimpleValueVM<U> xVM;
	private SimpleValueVM<V> yVM;
	
	@SuppressWarnings("unchecked")
	public TupleVM(MVVM mvvm, Tuple<U, V> model)
	{
        super(mvvm, model);
	}

    @Override
    protected void doMappings(HashMap<String, IViewModel<?>> childVMs) {
        xVM = (SimpleValueVM<U>)childVMs.get("x");
        yVM = (SimpleValueVM<V>)childVMs.get("y");
    }

    public SimpleValueVM<U> getxVM() {
		return xVM;
	}
	
	public void setxVM(SimpleValueVM<U> xVM) {
		this.xVM = xVM;
	}
	
	public SimpleValueVM<V> getyVM() {
		return yVM;
	}
	
	public void setyVM(SimpleValueVM<V> yVM) {
		this.yVM = yVM;
	}
}
