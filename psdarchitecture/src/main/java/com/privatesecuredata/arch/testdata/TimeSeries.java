package com.privatesecuredata.arch.testdata;

import com.privatesecuredata.arch.db.IPersistableFactory;
import com.privatesecuredata.arch.db.annotations.DbFactory;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.ListVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.SimpleVmMapping;
import com.privatesecuredata.arch.testdata.vm.CandleVM;
import com.privatesecuredata.arch.testdata.vm.TimeSeriesVM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@DbFactory(factoryType = TimeSeries.DbFactory.class)
@ComplexVmMapping(vmType = TimeSeriesVM.class, vmFactoryType = TimeSeriesVM.VmFactory.class)
public class TimeSeries {

    public static class DbFactory implements IPersistableFactory<TimeSeries> {

        @Override
        public TimeSeries create() {
            return new TimeSeries();
        }
    }

	@ListVmMapping(parentType=TimeSeries.class, vmType=CandleVM.class, modelType=Candle.class)
	private List<Candle>  data;
	
	@SimpleVmMapping
	private String name;

    public TimeSeries() {}

	public TimeSeries(Collection<Candle> timeSeries)
	{
		data = new ArrayList<Candle>(timeSeries);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public List<Candle> getData() 
	{
		return data;
	}
    public void setData(List<Candle> data)
    {
        this.data = data;
    }
}
