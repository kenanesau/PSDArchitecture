package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 5/19/17.
 */

public class USLengthMeasurementSystem extends AbstractMeasurementSystem {
    public enum Units {
        POINT(0),
        PICA(1),
        INCH(2),
        FOOT(3),
        YARD(4),
        MILE(5);

        private int value;

        private Units(int value) {
            this.value = value;
        }

        public int val() {
            return value;
        }
    }

    private Conversion[] convs = new Conversion[] {
        new Conversion("Point", "p", 12, 1),
        new Conversion("Pica", "P", 6, 1),
        new Conversion("Inch", "in", 12, 1),
        new Conversion("Foot", "ft", 3, 1),
        new Conversion("Yard", "yd", 1760, 1),
        new Conversion("Mile", "mi", 0, 0)
    };

    private Conversion metricConversion = new Conversion("Millimeter", "mm", 254, 10 * 12 * 6);

    @Override
    public Conversion[] getUnits() {
        return convs;
    }

    @Override
    public Conversion getMetricConversion() {
        return metricConversion;
    }
}
