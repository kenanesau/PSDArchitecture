package com.privatesecuredata.arch.testdata.vm;

import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.testdata.Candle;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class CandleVM extends ComplexViewModel<Candle> {

    public static class VmFactory implements ComplexViewModel.VmFactory<CandleVM, Candle> {
        @Override
        public CandleVM create(MVVM mvvm, Candle m) {
            return new CandleVM(mvvm, m);
        }
    }

	private TupleVM<Date, Double> open;
	private TupleVM<Date, Double> close;
	private TupleVM<Date, Double> high;
	private TupleVM<Date, Double> low;
	private SimpleValueVM<Calendar> time;
	
	@SuppressWarnings("unchecked")
	public CandleVM(MVVM mvvm, Candle model) {
        super(mvvm, model);
		HashMap<String, IViewModel<?>> children = getNameViewModelMapping();
		this.open = (TupleVM<Date, Double>) children.get("open");
		this.close = (TupleVM<Date, Double>) children.get("close");
		this.high = (TupleVM<Date, Double>) children.get("high");
		this.low = (TupleVM<Date, Double>) children.get("low");
		this.time = (SimpleValueVM<Calendar>) children.get("time");
	}

	public TupleVM<Date, Double> getOpen() {
		return open;
	}

	public void setOpen(TupleVM<Date, Double> open) {
		this.open = open;
	}

	public TupleVM<Date, Double> getClose() {
		return close;
	}

	public void setClose(TupleVM<Date, Double> close) {
		this.close = close;
	}

	public TupleVM<Date, Double> getHigh() {
		return high;
	}

	public void setHigh(TupleVM<Date, Double> high) {
		this.high = high;
	}

	public TupleVM<Date, Double> getLow() {
		return low;
	}

	public void setLow(TupleVM<Date, Double> low) {
		this.low = low;
	}

	public SimpleValueVM<Calendar> getTime() {
		return time;
	}

	public void setTime(SimpleValueVM<Calendar> time) {
		this.time = time;
	}
	
	
}
