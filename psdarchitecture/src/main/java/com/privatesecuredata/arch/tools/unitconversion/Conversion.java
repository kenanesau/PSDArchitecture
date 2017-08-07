package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 5/19/17.
 */

public class Conversion {
    private String name; // eg. Millimeter
    private String unit; // eg. mm
    private int factorNextEnumerator; // eg. 10 (to get to centimeter)
    private int factorNexDenominator; // eg. 1 (to get to centimeter)

    public Conversion(String name, String unit, int enumerator, int denominator) {
        this.name = name;
        this.unit = unit;
        this.factorNextEnumerator = enumerator;
        this.factorNexDenominator = denominator;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public int getFactorNextEnumerator() {
        return factorNextEnumerator;
    }

    public int getFactorNextDenominator() {
        return factorNexDenominator;
    }
}
