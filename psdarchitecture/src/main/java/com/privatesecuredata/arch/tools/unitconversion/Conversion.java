package com.privatesecuredata.arch.tools.unitconversion;

/**
 * Created by kenan on 5/19/17.
 */

public class Conversion {
    private String name; // eg. Millimeter
    private String unit; // eg. mm
    private long factorNextEnumerator; // eg. 10 (to get to centimeter)
    private long factorNextDenominator; // eg. 1 (to get to centimeter)

    public Conversion(String name, String unit, long enumerator, long denominator) {
        this.name = name;
        this.unit = unit;
        this.factorNextEnumerator = enumerator;
        this.factorNextDenominator = denominator;
    }

    public String getName()
    {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public long getFactorNextEnumerator() {
        return factorNextEnumerator;
    }

    public long getFactorNextDenominator() {
        return factorNextDenominator;
    }
}
