package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 8/7/17.
 */

public class USLiquidVolumeMeasurementSystem extends AbstractMeasurementSystem {
    public enum Units {
        MINIM(0),
        FLUIDDRAM(1),
        TEASPOON(2),
        TABLESPOON(3),
        FLUIDOUNCE(4),
        SHOT(5),
        GIL(6),
        CUP(7),
        PINT(8),
        QUART(9),
        GALLON(10),
        OILBARREL(11),
        HOGSHEAD(12);

        private int value;

        private Units(int value) {
            this.value = value;
        }

        public int val() {
            return value;
        }
    }

    private Conversion[] convs = new Conversion[] {
            new Conversion("Minim", "min", 60, 1),
            new Conversion("Fluid Dram", "fl dr", 4, 3),
            new Conversion("Teaspoon", "tsp", 3, 1),
            new Conversion("Tablepoon", "Tbsp", 2, 1),
            new Conversion("Fluid ounce", "fl oz", 3, 2),
            new Conversion("US shot", "jig", 8, 3),
            new Conversion("US gill", "gi", 2, 1),
            new Conversion("US cup", "gi", 2, 1),
            new Conversion("US pint", "pt", 2, 1),
            new Conversion("US quart", "qt", 4, 1),
            new Conversion("US gallon", "gal", 42, 1),
            new Conversion("Oil barrel", "bbl", 3, 2),
            new Conversion("Hogshead", "h", 0, 0),
    };

    private Conversion metricConversion = new Conversion("Milliliter", "ml",
            473176473L, (1000000L * 7680L));

    /*private Conversion metricConversion = new Conversion("Milliliter", "ml",
            162307, 10000);*/

    @Override
    public MeasurementSysFactory.Type getType() {
        return MeasurementSysFactory.Type.LIQUIDVOLUME;
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
