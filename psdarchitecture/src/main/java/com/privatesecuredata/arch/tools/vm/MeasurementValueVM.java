package com.privatesecuredata.arch.tools.vm;

import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementSysFactory;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementValue;

import java.util.HashMap;

/**
 * Created by kenan on 8/7/17.
 */

public class MeasurementValueVM extends ComplexViewModel<MeasurementValue> {
    private SimpleValueVM<Double> valueVm;
    private SimpleValueVM<MeasurementSysFactory.System> typeVm;

    public static class VmFactory implements ComplexViewModel.VmFactory<MeasurementValueVM, MeasurementValue> {

        @Override
        public MeasurementValueVM create(MVVM mvvm, MeasurementValue m) {
            return new MeasurementValueVM(mvvm, m);
        }
    }

    public MeasurementValueVM(MVVM mvvm, MeasurementValue model) { super(mvvm, model); }

    @Override
    protected void doMappings(HashMap<String, IViewModel<?>> childVMs)
    {
        this.valueVm = (SimpleValueVM<Double>)childVMs.get(MeasurementValue.FLD_VAL);
        this.typeVm = (SimpleValueVM<MeasurementSysFactory.System>)childVMs.get(MeasurementValue.FLD_TYPE);
    }

    public SimpleValueVM<Double> getValueVM() {
        return this.valueVm;
    }

    public SimpleValueVM<MeasurementSysFactory.System> getTypeVM() {
        return typeVm;
    }

    public SimpleValueVM<String> getTxtUnitVM() {
        return null; //TODO: Get with help of MeasurementSysFactory...
    }
}
