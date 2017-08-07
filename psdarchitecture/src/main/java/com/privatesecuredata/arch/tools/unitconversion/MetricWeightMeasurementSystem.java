package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 8/7/17.
 */

public class MetricWeightMeasurementSystem extends AbstractMeasurementSystem {

    public enum Units {
        MILLIGRAMM(0),
        GRAMM(1),
        KILOGRAMM(2),
        TONNE(3);

        private int value;

        private Units(int value) {
            this.value = value;
        }

        public int val() {
            return value;
        }
    }

    private Conversion[] convs = new Conversion[] {
            new Conversion("Milligramm", "mg", 1000, 1),
            new Conversion("Gramm", "gr", 1000, 1),
            new Conversion("Kilogramm", "kg", 1000, 1),
            new Conversion("Tonne", "to", 0, 0)
    };

    private Conversion metricConversion = new Conversion("Milligramm", "mg", 1, 1);

    @Override
    public Conversion[] getUnits() {
        return convs;
    }

    @Override
    public Conversion getMetricConversion() {
        return metricConversion;
    }
}
