package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 8/7/17.
 */

public class USWeightAvoirdupoisMeasurementSystem extends AbstractMeasurementSystem {
    public enum Units {
        GRAIN(0),
        DRAM(1),
        OUNCE(2),
        POUND(3),
        HUNDREDWEIGHT(4),
        TON(5);

        private int value;

        private Units(int value) {
            this.value = value;
        }

        public int val() {
            return value;
        }
    }

    private Conversion[] convs = new Conversion[] {
            new Conversion("Grain", "gr", 27*32+11, 32),
            new Conversion("Dram", "dr", 16, 1),
            new Conversion("Ounce", "oz", 16, 1),
            new Conversion("Pound", "lb", 100, 1),
            new Conversion("Hundredweight", "cwt", 20, 1),
            new Conversion("Ton", "short ton", 0, 0)
    };

    private Conversion metricConversion = new Conversion("Milligramm", "mg", 6479891, 100000);

    @Override
    public MeasurementSysFactory.Type getType() {
        return MeasurementSysFactory.Type.WEIGHT;
    }

    @Override
    public Conversion[] getUnits() {
        return convs;
    }

    @Override
    public Conversion getMetricConversion() {
        return metricConversion;
    }
}
