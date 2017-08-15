package com.privatesecuredata.arch.tools.vm;

import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueLogicVM;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementSysFactory;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementValue;

import java.util.HashMap;

/**
 * Created by kenan on 8/7/17.
 */

public class MeasurementValueVM extends ComplexViewModel<MeasurementValue> {
    private SimpleValueVM<Double> valueVm;
    private SimpleValueVM<MeasurementSysFactory.System> sysVm;
    private SimpleValueVM<MeasurementSysFactory.Type> typeVm;
    private SimpleValueVM<Integer> unitVm;
    private SimpleValueLogicVM<String> txtUnitVm;

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
        this.typeVm = (SimpleValueVM<MeasurementSysFactory.Type>)childVMs.get(MeasurementValue.FLD_TYPE);
        this.sysVm = (SimpleValueVM<MeasurementSysFactory.System>)childVMs.get(MeasurementValue.FLD_SYS);
        this.unitVm = (SimpleValueVM<Integer>)childVMs.get(MeasurementValue.FLD_UNIT);
        this.txtUnitVm = new SimpleValueLogicVM<String>("gr", this.sysVm, this.typeVm, this.unitVm);
        this.txtUnitVm.setDataCBs(null,
                intUnit -> MeasurementSysFactory.create(
                            this.sysVm.get(),
                            this.typeVm.get()).getUnit(this.unitVm.get()).getUnit() );
    }

    public SimpleValueVM<Double> getValueVM() {
        return this.valueVm;
    }

    public SimpleValueVM<MeasurementSysFactory.System> getSysVM() {
        return sysVm;
    }

    public SimpleValueVM<MeasurementSysFactory.Type> getTypeVM() {
        return typeVm;
    }

    public SimpleValueVM<Integer> getUnitVM() {
        return unitVm;
    }

    public SimpleValueVM<String> getTxtUnitVM() {
        return txtUnitVm;
    }
}
