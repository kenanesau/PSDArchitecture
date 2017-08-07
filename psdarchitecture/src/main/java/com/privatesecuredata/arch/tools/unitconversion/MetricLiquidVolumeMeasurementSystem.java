package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 8/7/17.
 */

public class MetricLiquidVolumeMeasurementSystem extends AbstractMeasurementSystem {

    public enum Units {
        MILLILITER(0),
        LITER(1);

        private int value;

        private Units(int value) {
            this.value = value;
        }

        public int val() {
            return value;
        }
    }

    private Conversion[] convs = new Conversion[] {
            new Conversion("Milliliter", "ml", 1000, 1),
            new Conversion("Liter", "l", 1000, 1)
    };

    private Conversion metricConversion = new Conversion("Milliliter", "ml", 1, 1);

    @Override
    public Conversion[] getUnits() {
        return convs;
    }

    @Override
    public Conversion getMetricConversion() {
        return metricConversion;
    }
}
