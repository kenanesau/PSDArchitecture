package com.privatesecuredata.arch.testdata.vm;

import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.FastListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.testdata.TimeSeries;

import java.util.HashMap;

public class TimeSeriesVM extends ComplexViewModel<TimeSeries> {

    public static class VmFactory implements ComplexViewModel.VmFactory<TimeSeriesVM, TimeSeries> {

        @Override
        public TimeSeriesVM create(MVVM mvvm, TimeSeries m) {
            return new TimeSeriesVM(mvvm, m);
        }
    }
	
	private SimpleValueVM<String> nameVM;
	private FastListViewModel<TimeSeries, TimeSeriesVM> dataVM;
	
	public TimeSeriesVM(MVVM mvvm, TimeSeries ts)
	{
        super(mvvm, ts);
		HashMap<String, IViewModel<?>> childModels = getNameViewModelMapping();
		
		nameVM = ((SimpleValueVM<String>) childModels.get("name"));
		dataVM = ((FastListViewModel<TimeSeries, TimeSeriesVM>) childModels.get("data"));
	}

	public String getName() {
		return nameVM.get();
	}

	public void setName(String name) {
		this.nameVM.set(name);
	}

	public FastListViewModel<TimeSeries, TimeSeriesVM> getData() {
		return dataVM;
	}

}
