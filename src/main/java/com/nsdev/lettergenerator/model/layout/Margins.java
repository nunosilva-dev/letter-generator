package com.nsdev.lettergenerator.model.layout;

/**
 * Physical-page insets in millimeters, outside both the body and reserved zones. Construction
 * stores values unchanged; generation requires finite nonnegative margins and usable page width and
 * height after margins and zones are removed.
 *
 * @param top inset from the physical page's top edge, in millimeters
 * @param right inset from the physical page's right edge, in millimeters
 * @param bottom inset from the physical page's bottom edge, in millimeters
 * @param left inset from the physical page's left edge, in millimeters
 */
public record Margins(double top, double right, double bottom, double left) {
    /** Default physical-page inset on each side, in millimeters. */
    private static final double DEFAULT_MARGIN_MM = 20.0;

    /**
     * Creates the default margins of 20 millimeters on each side.
     *
     * @return equal 20 mm margins
     */
    public static Margins defaults() {
        return new Margins(
                DEFAULT_MARGIN_MM, DEFAULT_MARGIN_MM, DEFAULT_MARGIN_MM, DEFAULT_MARGIN_MM);
    }

    /**
     * Creates equal margins without validating their value.
     *
     * @param millimeters inset to use on all four sides
     * @return margins carrying the supplied value on each side
     */
    public static Margins uniform(double millimeters) {
        return new Margins(millimeters, millimeters, millimeters, millimeters);
    }
}
