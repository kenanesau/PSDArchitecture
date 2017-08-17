package com.privatesecuredata.arch.tools.vm;

import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueLogicVM;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.mvvm.vm.StringFormatVM;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementSysFactory;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementValue;

import java.util.HashMap;

/**
 * Created by kenan on 8/7/17.
 */

public class MeasurementValueVM extends ComplexViewModel<MeasurementValue> {
    private SimpleValueVM<Double> valueVm;
    private StringFormatVM stringValueVm;
    private SimpleValueVM<Integer> sysVm;
    private SimpleValueVM<Integer> typeVm;
    private SimpleValueVM<Integer> intUnitVm;
    private SimpleValueLogicVM<String> strUnitVm;
    private SimpleValueVM<String> unitPrefixVm;
    private SimpleValueVM<String> unitPostfixVm;
    private StringFormatVM unitVm;

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
        this.sysVm = (SimpleValueVM<Integer>)childVMs.get(MeasurementValue.FLD_SYS);
        this.typeVm = (SimpleValueVM<Integer>)childVMs.get(MeasurementValue.FLD_TYPE);
        this.intUnitVm = (SimpleValueVM<Integer>)childVMs.get(MeasurementValue.FLD_UNIT);
        this.strUnitVm = new SimpleValueLogicVM<String>("gr", this.sysVm, this.typeVm, this.intUnitVm);
        this.strUnitVm.setDataCBs(null,
                new SimpleValueLogicVM.IGetData<String>() {
                    @Override
                    public String get(String intUnit) {
                        return MeasurementSysFactory.create(
                                MeasurementValueVM.this.getSys(),
                                MeasurementValueVM.this.getType())
                                    .getUnit(MeasurementValueVM.this.intUnitVm.get()).getUnit();
                    }
                });
        registerChildVM(strUnitVm);
        this.unitPrefixVm = new SimpleValueVM<String>("");
        registerChildVM(this.unitPrefixVm);
        this.unitPostfixVm = new SimpleValueVM<String>("");
        registerChildVM(unitPostfixVm);
        this.stringValueVm = new StringFormatVM("%.2f", valueVm);
        registerChildVM(stringValueVm);
        this.unitVm = new StringFormatVM("%s%s%s",
                this.unitPrefixVm,
                this.strUnitVm,
                this.unitPostfixVm);
        registerChildVM(unitVm);
    }

    public SimpleValueVM<Double> getValueVM() {
        return this.valueVm;
    }

    public SimpleValueVM<Integer> getSysVM() {
        return sysVm;
    }

    public SimpleValueVM<Integer> getTypeVM() {
        return typeVm;
    }

    public SimpleValueVM<Integer> getIntUnitVM() {
        return intUnitVm;
    }

    public SimpleValueVM<String> getTxtUnitVM() {
        return strUnitVm;
    }

    public MeasurementSysFactory.System getSys() {
        return MeasurementSysFactory.System.values()[this.getSysVM().get()];
    }

    public MeasurementSysFactory.Type getType() {
        return MeasurementSysFactory.Type.values()[this.getTypeVM().get()];
    }

    public StringFormatVM getStrValueVM() {
        return stringValueVm;
    }

    public SimpleValueVM<String> getUnitPrefixVM() {
        return unitPrefixVm;
    }

    public SimpleValueVM<String> getUnitPostfixVM() {
        return unitPostfixVm;
    }

    public StringFormatVM getUnitVM() {
        return unitVm;
    }
}
