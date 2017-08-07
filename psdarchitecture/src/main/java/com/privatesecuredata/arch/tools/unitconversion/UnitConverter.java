package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 5/19/17.
 */

public class UnitConverter {
    public enum UnitType {
        LENGTH,
        WEIGHT,
        FLUID,
    }

    private AbstractMeasurementSystem from;
    private AbstractMeasurementSystem to;

    /**
     * Constructor for conversions between two systems of measure
     * @param from
     * @param to
     */
    public UnitConverter(AbstractMeasurementSystem from, AbstractMeasurementSystem to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Constructor for conversions within one system of measure
     * @param from
     */
    public UnitConverter(AbstractMeasurementSystem from) {
        this(from, from);
    }

    /**
     * convert the value
     * @param from Unit to convert from (eg. millimeter)
     * @param to Unit to convert to (eg. yards)
     * @return
     */
    public static void convert(MeasurementValue from, MeasurementValue to) {
        AbstractMeasurementSystem fromSys = MeasurementSysFactory.create(from.getSys(), from.getType());
        AbstractMeasurementSystem toSys = MeasurementSysFactory.create(to.getSys(), to.getType());

        Conversion[] fromConvs = fromSys.getUnits();
        long enumerator = 1;
        long denominator = 1;

        /**
         * Convert to smallest unit within from-system
         */
        for (int i = 0; i<from.getUnitVal(); i++) {
            enumerator *= fromConvs[i].getFactorNextEnumerator();
            denominator *= fromConvs[i].getFactorNextDenominator();
        }

        double val = from.getVal() * (double)enumerator / (double)denominator;

        /**
         * Convert to metric-system
         */
        val = val * (double)fromSys.getMetricConversion().getFactorNextEnumerator() / (double)fromSys.getMetricConversion().getFactorNextDenominator();

        /**
         * Convert from metric-system to toSys
         */
        val = val * (double)fromSys.getMetricConversion().getFactorNextDenominator() / (double)fromSys.getMetricConversion().getFactorNextEnumerator();

        Conversion[] toConvs = toSys.getUnits();
        enumerator = 1;
        denominator = 1;
        /**
         * Convert to target unit in toSys
         */
        for (int i = 0; i<to.getUnitVal(); i++) {
            enumerator *= toConvs[i].getFactorNextEnumerator();
            denominator *= toConvs[i].getFactorNextDenominator();
        }

        val = val * (double)denominator / (double)enumerator;

        to.setVal(val);
        return;
    }

    public double convertDouble(int from, int to, double val) {
        return 0; //TODO Implement me
    }

    public int convertInt(int from, int to, int val) {
        return 0;
    }
}
