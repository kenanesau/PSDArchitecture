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
        return;
    }

    public double convertDouble(int from, int to, double val) {
        return 0; //TODO Implement me
    }

    public int convertInt(int from, int to, int val) {
        return 0;
    }
}
