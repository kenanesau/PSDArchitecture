package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 8/7/17.
 */

public class MeasurementSysFactory {
    public enum System {
        METRIC(0),
        US(1),
        MIXED(-1);

        private int _val;

        private System(int val) {
            _val = val;
        }

        public int val() { return _val; }
    };

    public enum Type {
        LENGTH(0),
        WEIGHT(1),
        LIQUIDVOLUME(2);

        private int _val;

        private Type(int val) {
            _val = val;
        }

        public int val() { return _val; }
    };

    private MeasurementValue.ValueSpec spec;
    private static AbstractMeasurementSystem[][] systems = {
            {new MetricLengthMeasurementSystem(), new MetricWeightMeasurementSystem(), new MetricLiquidVolumeMeasurementSystem()},
            {new USLengthMeasurementSystem(), new USWeightAvoirdupoisMeasurementSystem(), new USLiquidVolumeMeasurementSystem()}
    };

    public static MeasurementValue.ValueSpec[] createSpecs(Type type) {
        MeasurementValue.ValueSpec[] ar = new MeasurementValue.ValueSpec[System.values().length];

        for (int i=0; i<System.values().length; i++) {
            ar[i] = new MeasurementValue.ValueSpec(System.values()[i], type, 0);
        }

        return ar;
    }

    public static AbstractMeasurementSystem create(System sys, Type type) {
        return systems[sys.val()][type.val()];
    }

    public MeasurementSysFactory(MeasurementValue.ValueSpec spec) {
        this.spec = spec;
    }

    public MeasurementValue val(double val) {
        return new MeasurementValue(spec.getSys(), spec.getType(), spec.getUnit(), val);
    }
}
