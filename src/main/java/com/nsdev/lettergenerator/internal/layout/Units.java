package com.nsdev.lettergenerator.internal.layout;

/** Physical and font coordinate conversions; FOP uses integer millipoints. */
public final class Units {
    public static final double FONT_UNITS_PER_EM = 1000.0;
    private static final double POINTS_PER_INCH = 72.0;
    private static final double MILLIMETERS_PER_INCH = 25.4;

    private Units() {}

    public static double millimetersToPoints(double millimeters) {
        return millimeters * POINTS_PER_INCH / MILLIMETERS_PER_INCH;
    }

    public static double fontUnitsToMillipoints(double fontUnits, int fontSizeMpt) {
        return fontUnits * fontSizeMpt / FONT_UNITS_PER_EM;
    }

    public static double fontUnitsToPoints(double fontUnits, double fontSizePt) {
        return fontUnits * fontSizePt / FONT_UNITS_PER_EM;
    }
}
