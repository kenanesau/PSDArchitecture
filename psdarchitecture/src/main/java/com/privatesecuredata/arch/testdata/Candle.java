package com.privatesecuredata.arch.testdata;

import com.privatesecuredata.arch.db.IPersistableFactory;
import com.privatesecuredata.arch.db.annotations.DbFactory;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.SimpleVmMapping;
import com.privatesecuredata.arch.testdata.vm.CandleVM;
import com.privatesecuredata.arch.testdata.vm.TupleVM;

import java.util.Calendar;

@DbFactory(factoryType = Candle.DbFactory.class)
@ComplexVmMapping(vmType = CandleVM.class, vmFactoryType = CandleVM.VmFactory.class)
public class Candle {

    public static class DbFactory implements IPersistableFactory<Candle> {

        @Override
        public Candle create() {
            return new Candle();
        }
    }

    @ComplexVmMapping(vmType = TupleVM.class, loadLazy=false)
	private Tuple<Calendar, Double> open;
	@ComplexVmMapping(vmType = TupleVM.class, loadLazy=false)
	private Tuple<Calendar, Double> close;
	@ComplexVmMapping(vmType = TupleVM.class, loadLazy=false)
	private Tuple<Calendar, Double> high;
	@ComplexVmMapping(vmType = TupleVM.class, loadLazy=false)
	private Tuple<Calendar, Double> low;
	@SimpleVmMapping
	private Calendar time;

    public Candle() {}
	
	public Candle(Calendar time, Double open, Double close, Double high, Double low)
	{
		if ( (null == time) || (null == open) || (null == close) || (null==high) ||  (null == low) )
			throw new ArgumentException("Parameters of cunstructor must not be null!");
		if ( (high < low) || (high < close) || (high < open) )
			throw new ArgumentException("Parameter \"high\" has to be greater or equal to all other parameters");
		if ( (low > high) || (low > close) || (low > open) )
			throw new ArgumentException("Parameter \"low\" has to be lesser or equal to all other parameters");
		
		this.setTime(time);
		this.open = new Tuple<Calendar, Double>(time, open);
		this.close = new Tuple<Calendar, Double>(time, close);
		this.high = new Tuple<Calendar, Double>(time, high);
		this.low = new Tuple<Calendar, Double>(time, low);
	}
	
	public Tuple<Calendar, Double> getOpen() {
		return open;
	}
	public void setOpen(Tuple<Calendar, Double> open) {
		this.open = open;
	}
	public Tuple<Calendar, Double> getClose() {
		return close;
	}
	public void setClose(Tuple<Calendar, Double> close) {
		this.close = close;
	}
	public Tuple<Calendar, Double> getHigh() {
		return high;
	}
	public void setHigh(Tuple<Calendar, Double> high) {
		this.high = high;
	}
	public Tuple<Calendar, Double> getLow() {
		return low;
	}
	public void setLow(Tuple<Calendar, Double> low) {
		this.low = low;
	}

	public Calendar getTime() {
		return time;
	}
	public void setTime(Calendar time) {
		this.time = time;
	}
}
