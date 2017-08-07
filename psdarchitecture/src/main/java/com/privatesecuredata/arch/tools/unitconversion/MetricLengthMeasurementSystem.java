package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 5/19/17.
 */

public class MetricLengthMeasurementSystem extends AbstractMeasurementSystem {
    public enum Units {
        MILLIMETER(0),
        CENTIMETER(1),
        METER(2),
        KILOMETER(3);

        private int value;

        private Units(int value) {
            this.value = value;
        }

        public int val() {
            return value;
        }
    }

    private Conversion[] convs = new Conversion[] {
        new Conversion("Millimeter", "mm", 10, 1),
        new Conversion("Centimeter", "cm", 100, 1),
        new Conversion("Meter", "m", 1000, 1),
        new Conversion("Kilometer", "km", 0, 0)
    };

    private Conversion metricConversion = new Conversion("Millimeter", "mm", 1, 1);

    @Override
    public Conversion[] getUnits() {
        return convs;
    }

    @Override
    public Conversion getMetricConversion() {
        return metricConversion;
    }
}
