package com.nsdev.lettergenerator.model.layout;

/** Nonnegative page margins in millimeters; validated when a document is generated. */
public record Margins(double top, double right, double bottom, double left) {
    private static final double DEFAULT_MARGIN_MM = 20.0;

    public static Margins defaults() {
        return new Margins(
                DEFAULT_MARGIN_MM, DEFAULT_MARGIN_MM, DEFAULT_MARGIN_MM, DEFAULT_MARGIN_MM);
    }

    public static Margins uniform(double millimeters) {
        return new Margins(millimeters, millimeters, millimeters, millimeters);
    }
}
