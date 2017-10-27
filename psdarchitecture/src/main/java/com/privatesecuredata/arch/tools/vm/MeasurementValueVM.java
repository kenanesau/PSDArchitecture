package com.privatesecuredata.arch.tools.vm;

import android.os.Parcel;
import android.os.Parcelable;

import com.privatesecuredata.arch.R;
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
    private SimpleValueLogicVM stringValueVm;
    private SimpleValueVM<String> formatStringVm;
    private SimpleValueVM<Integer> sysVm;
    private SimpleValueVM<Integer> typeVm;
    private SimpleValueVM<Integer> intUnitVm;
    private SimpleValueLogicVM<String> strUnitVm;
    private SimpleValueVM<String> unitPrefixVm;
    private SimpleValueVM<String> unitPostfixVm;
    private SimpleValueLogicVM<String> unitVm;

    public static class VmFactory implements ComplexViewModel.VmFactory<MeasurementValueVM, MeasurementValue> {

        @Override
        public MeasurementValueVM create(MVVM mvvm, MeasurementValue m) {
            return new MeasurementValueVM(mvvm, m);
        }
    }

    public MeasurementValueVM(MVVM mvvm, MeasurementValue model) { super(mvvm, model); }

    private MeasurementValueVM(Parcel in) {
        this.sysVm.set(in.readInt());
        this.typeVm.set(in.readInt());
        this.intUnitVm.set(in.readInt());
        this.valueVm.set(in.readDouble());

        initSecondaryVMs();
    }

    protected void initSecondaryVMs() {
        this.formatStringVm = new SimpleValueVM<String>("%.2f");
        this.strUnitVm = new SimpleValueLogicVM<String>("gr", this.valueVm, this.sysVm, this.typeVm, this.intUnitVm);
        this.strUnitVm.setDataCBs(null,
                new SimpleValueLogicVM.IGetData<String>() {
                    @Override
                    public String get(String currentVal) {

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
        this.stringValueVm = new SimpleValueLogicVM<String>("", this.valueVm);
        this.stringValueVm.setDataCBs(null,
                new SimpleValueLogicVM.IGetData<String>() {
                    @Override
                    public String get(String currentVal) {
                        if (MeasurementValueVM.this.valueVm.get() < 0.0) {
                            if (MeasurementValueVM.this.getType() == MeasurementSysFactory.Type.LENGTH)
                                return getResources().getString(R.string.psdarch_unspecified_length);
                            else if (MeasurementValueVM.this.getType() == MeasurementSysFactory.Type.WEIGHT)
                                return getResources().getString(R.string.psdarch_unspecified_weight);
                            else if (MeasurementValueVM.this.getType() == MeasurementSysFactory.Type.LIQUIDVOLUME)
                                return getResources().getString(R.string.psdarch_unspecified_volume);
                        }
                        else {
                            return String.format(
                                    MeasurementValueVM.this.getFormatStringVM().get(),
                                    MeasurementValueVM.this.getValueVM().get());
                        }

                        return currentVal;
                    }
                });
        registerChildVM(stringValueVm);
        this.unitVm = new SimpleValueLogicVM<String>("", this.valueVm, this.strUnitVm, this.unitPrefixVm, this.unitPostfixVm);
        this.unitVm.setDataCBs(null,
                new SimpleValueLogicVM.IGetData<String>() {
                    @Override
                    public String get(String currentValue) {
                        if (MeasurementValueVM.this.getValueVM().get() < 0.0)
                            return "";
                        else
                            return String.format("%s %s %s",
                                MeasurementValueVM.this.unitPrefixVm.get(),
                                MeasurementValueVM.this.strUnitVm.get(),
                                MeasurementValueVM.this.unitPostfixVm.get());

                    }
                });
        registerChildVM(unitVm);
    }

    @Override
    protected void doMappings(HashMap<String, IViewModel<?>> childVMs)
    {
        this.valueVm = (SimpleValueVM<Double>)childVMs.get(MeasurementValue.FLD_VAL);
        this.sysVm = (SimpleValueVM<Integer>)childVMs.get(MeasurementValue.FLD_SYS);
        this.typeVm = (SimpleValueVM<Integer>)childVMs.get(MeasurementValue.FLD_TYPE);
        this.intUnitVm = (SimpleValueVM<Integer>)childVMs.get(MeasurementValue.FLD_UNIT);

        initSecondaryVMs();
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

    public SimpleValueLogicVM<String> getStrValueVM() {
        return stringValueVm;
    }

    public SimpleValueVM<String> getUnitPrefixVM() {
        return unitPrefixVm;
    }

    public SimpleValueVM<String> getUnitPostfixVM() {
        return unitPostfixVm;
    }

    public SimpleValueLogicVM<String> getUnitVM() {
        return unitVm;
    }

    public SimpleValueVM<String> getFormatStringVM() { return  formatStringVm; }

    public void set(MeasurementValue measVal) {
        getSysVM().set(measVal.getSys().val());
        getTypeVM().set(measVal.getType().val());
        getIntUnitVM().set(measVal.getUnitVal());
        getValueVM().set(measVal.getVal());
    }

    /**
     * returns the uncommitted values as MeasurementValue
     * @return
     */
    public MeasurementValue get() {
        return new MeasurementValue(
                MeasurementSysFactory.System.values()[getSysVM().get()],
                MeasurementSysFactory.Type.values()[getTypeVM().get()],
                getIntUnitVM().get(),
                getValueVM().get()
        );
    }

}
