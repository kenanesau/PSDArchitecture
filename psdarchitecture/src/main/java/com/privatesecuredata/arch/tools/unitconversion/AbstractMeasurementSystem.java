package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 5/19/17.
 */

public abstract class AbstractMeasurementSystem {
    public abstract Conversion[] getUnits();
    /**
     * Return the conversion of the smallest unit to the smallest metric unit
     */
    public abstract Conversion getMetricConversion();

    /**
     * Returns the smalles unit handeled
     * @return
     */
    public Conversion getSmallestUnit() {
        return getUnits()[0];
    }

    public Conversion getUnit(int unit) {
        return getUnits()[unit];
    }

    public static <T> T convert(int fromUnit, int toUnit, AbstractMeasurementSystem otherSystem, T value) {
        return null;
    }
}
