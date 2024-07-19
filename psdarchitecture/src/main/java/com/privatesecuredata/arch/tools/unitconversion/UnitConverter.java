package com.privatesecuredata.arch.tools.unitconversion;

import com.privatesecuredata.arch.exceptions.ArgumentException;

/**
 * Created by kenan on 5/19/17.
 */

public class UnitConverter {
    public enum UnitType {
        LENGTH,
        WEIGHT,
        FLUID,
    }

    private AbstractMeasurementSystem fromSys;
    private AbstractMeasurementSystem toSys;

    /**
     * Constructor for conversions between two systems of measure
     * @param fromSys
     * @param toSys
     */
    public UnitConverter(AbstractMeasurementSystem fromSys, AbstractMeasurementSystem toSys) {
        if (fromSys.getType() != toSys.getType())
            throw new ArgumentException("Types of Measurement-systems difer!");

        this.fromSys = fromSys;
        this.toSys = toSys;
    }

    /**
     * Constructor for conversions within one system of measure
     * @param fromSys
     */
    public UnitConverter(AbstractMeasurementSystem fromSys) {
        this(fromSys, fromSys);
    }

    /**
     * convert the value
     * @param fromVal Unit toSys convert fromSys (eg. millimeter)
     * @param toVal Unit toSys convert toSys (eg. yards)
     * @return
     */
    public static void convert(MeasurementValue fromVal, MeasurementValue toVal) {
        AbstractMeasurementSystem fromSys = MeasurementSysFactory.create(fromVal.getSys(), fromVal.getType());
        AbstractMeasurementSystem toSys = MeasurementSysFactory.create(toVal.getSys(), toVal.getType());

        UnitConverter converter = new UnitConverter(fromSys, toSys);
        double val = converter.convertDouble(fromVal.getUnitVal(), toVal.getUnitVal(), fromVal.getVal());
        toVal.setVal(val);
    }

    public double convertDouble(int unitFrom, int unitTo, double val) {
        Conversion[] fromConvs = fromSys.getUnits();
        long srcEnum = 1;
        long srcDenom = 1;

        /**
         * Convert to smallest unit within fromSys-system
         */
        for (int i = 0; i<unitFrom; i++) {
            srcEnum *= fromConvs[i].getFactorNextEnumerator();
            srcDenom *= fromConvs[i].getFactorNextDenominator();
        }

        long fromMetricEnum = 1;
        long fromMetricDenom = 1;
        if (fromSys != toSys) {
            /**
             * Convert to metric-system
             */
            fromMetricEnum = fromSys.getMetricConversion().getFactorNextEnumerator();
            fromMetricDenom = fromSys.getMetricConversion().getFactorNextDenominator();

            /**
             * Convert from metric-system to toSys
             */
            fromMetricEnum *= toSys.getMetricConversion().getFactorNextDenominator();
            fromMetricDenom *= toSys.getMetricConversion().getFactorNextEnumerator();
        }

        Conversion[] toConvs = toSys.getUnits();
        long targetEnum = 1;
        long targetDenom = 1;
        /**
         * Convert toSys target unit in toSys
         */
        for (int i = 0; i<unitTo; i++) {
            targetDenom *= toConvs[i].getFactorNextEnumerator();
            targetEnum *= toConvs[i].getFactorNextDenominator();
        }

        val *= ((double)srcEnum / (double)srcDenom);
        if ((fromSys != toSys)) {
            val *= (double)fromMetricEnum / (double)fromMetricDenom;
        }
        val *= (double)targetEnum / (double)targetDenom;

        return val;
    }

    public int convertInt(int unitFrom, int unitTo, int val) {
        double res = convertDouble(unitFrom, unitTo, (double)val);
        return (int)Math.round(res);
    }

    public long convertLong(int unitFrom, int unitTo, long val) {
        double res = convertDouble(unitFrom, unitTo, (double)val);
        return (long)Math.round(res);
    }

}
