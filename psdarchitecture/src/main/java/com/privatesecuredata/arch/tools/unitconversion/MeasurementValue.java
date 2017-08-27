package com.privatesecuredata.arch.tools.unitconversion;

import com.google.common.base.Objects;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.IPersistableFactory;
import com.privatesecuredata.arch.db.annotations.DbFactory;
import com.privatesecuredata.arch.db.annotations.DbField;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.SimpleVmMapping;
import com.privatesecuredata.arch.tools.vm.MeasurementValueVM;

/**
 * Created by kenan on 8/7/17.
 */

@DbFactory(factoryType = MeasurementValue.DbFactory.class)
@ComplexVmMapping(vmType = MeasurementValueVM.class, vmFactoryType = MeasurementValueVM.VmFactory.class)
public class MeasurementValue implements IPersistable{
    public static final String FLD_SYS = "sys";
    public static final String FLD_TYPE = "type";
    public static final String FLD_UNIT = "unit";
    public static final String FLD_VAL = "val";

    public static class DbFactory implements IPersistableFactory<MeasurementValue> {

        @Override
        public MeasurementValue create() {
            return new MeasurementValue();
        }
    }

    private DbId<MeasurementValue> dbId;

    @SimpleVmMapping
    @DbField(id=FLD_SYS)
    private int sys;

    @SimpleVmMapping
    @DbField(id=FLD_TYPE)
    private int type;

    @SimpleVmMapping
    @DbField(id=FLD_UNIT)
    private int unit;

    @SimpleVmMapping
    @DbField(id=FLD_VAL)
    private double val;

    public MeasurementValue() {
        new ValueSpec(MeasurementSysFactory.System.METRIC, MeasurementSysFactory.Type.LENGTH, 0);
        val = 0.0d;
    }

    public MeasurementValue(MeasurementSysFactory.System sys, MeasurementSysFactory.Type type, int unit)
    {
        this(sys, type, unit, 0.0);
    }

    public MeasurementValue(MeasurementSysFactory.System sys, MeasurementSysFactory.Type type, int unit, double val)
    {
        this.sys = sys.val();
        this.type = type.val();
        this.unit = unit;
        this.val = val;
    }

    @Override
    public DbId<MeasurementValue> getDbId() {
        return dbId;
    }

    @Override
    public <T extends IPersistable> void setDbId(DbId<T> dbId) {
        this.dbId = (DbId<MeasurementValue>)dbId;
    }


    public double getVal() {
        return val;
    }

    public void setVal(double val) {
        this.val = val;
    }

    public Conversion getUnit() {
        return MeasurementSysFactory.create(getSys(), getType()).getUnit(unit);
    }

    public String getUnitString() {
        return getUnit().getUnit();
    }

    public MeasurementSysFactory.System getSys() {
        return MeasurementSysFactory.System.values()[sys];
    }

    public MeasurementSysFactory.Type getType() {
        return MeasurementSysFactory.Type.values()[type];
    }

    public int getUnitVal() {
        return unit;
    }

    public static class ValueSpec {
        private MeasurementSysFactory.System sys;
        private MeasurementSysFactory.Type type;
        private int unit;

        public ValueSpec(MeasurementSysFactory.System sys, MeasurementSysFactory.Type type, int unit) {
            this.sys = sys;
            this.type = type;
            this.unit = unit;
        }

        public MeasurementSysFactory.System getSys() {
            return sys;
        }

        public MeasurementSysFactory.Type getType() {
            return type;
        }

        public int getUnit() {
            return unit;
        }

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.sys, this.type, this.unit, this.val);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MeasurementValue) {
            MeasurementValue that = (MeasurementValue) o;
            return Objects.equal(getSys(), that.getSys()) &&
                    Objects.equal(getType(), that.getType()) &&
                    Objects.equal(getUnit(), that.getUnit()) &&
                    Objects.equal(getVal(), that.getVal());
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("%s: %s: %.2f %s", getSys().toString(),
                getType().toString(), getVal(), getUnit().getUnit());
    }
}
