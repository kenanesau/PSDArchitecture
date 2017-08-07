package com.privatesecuredata.arch.tools.unitconversion;

import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;

/**
 * Created by kenan on 8/7/17.
 */

public class MeasurementValue implements IPersistable{
    public static final String FLD_VAL = "val";
    public static final String FLD_TYPE = "type";

    private DbId<MeasurementValue> dbId;

    private MeasurementSysFactory.System sys;
    private MeasurementSysFactory.Type type;
    private int unit;
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
        this.sys = sys;
        this.type = type;
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
        return MeasurementSysFactory.create(sys, type).getUnit(unit);
    }

    public MeasurementSysFactory.System getSys() {
        return sys;
    }

    public MeasurementSysFactory.Type getType() {
        return type;
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
}
