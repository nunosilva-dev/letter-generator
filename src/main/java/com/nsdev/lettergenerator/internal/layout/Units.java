package com.nsdev.lettergenerator.internal.layout;

/** Physical and font coordinate conversions; FOP uses integer millipoints. */
public final class Units {
    /** Scale of the normalized font metrics used by the font and glyph-bound calculations. */
    public static final double FONT_UNITS_PER_EM = 1000.0;

    /** PDF/PostScript point scale used for physical-page conversion. */
    private static final double POINTS_PER_INCH = 72.0;

    /** Exact millimeter length of one inch. */
    private static final double MILLIMETERS_PER_INCH = 25.4;

    /** Prevents instantiation of the scalar conversion helpers. */
    private Units() {}

    /**
     * Converts physical model dimensions to PDF points without rounding.
     *
     * @param millimeters distance in millimeters
     * @return distance in points, using 72 points per inch
     */
    public static double millimetersToPoints(double millimeters) {
        return millimeters * POINTS_PER_INCH / MILLIMETERS_PER_INCH;
    }

    /**
     * Scales normalized glyph metrics to FOP's millipoint coordinates.
     *
     * @param fontUnits metric expressed on a 1000-units-per-em scale
     * @param fontSizeMpt font size in millipoints
     * @return scaled metric in millipoints
     */
    public static double fontUnitsToMillipoints(double fontUnits, int fontSizeMpt) {
        return fontUnits * fontSizeMpt / FONT_UNITS_PER_EM;
    }

    /**
     * Scales normalized font metrics to the point coordinates used by PDFBox.
     *
     * @param fontUnits metric expressed on a 1000-units-per-em scale
     * @param fontSizePt font size in points
     * @return scaled metric in points
     */
    public static double fontUnitsToPoints(double fontUnits, double fontSizePt) {
        return fontUnits * fontSizePt / FONT_UNITS_PER_EM;
    }
}
