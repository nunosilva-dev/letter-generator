package com.nsdev.lettergenerator.internal.render.fo;

/**
 * Formats already validated model dimensions with explicit FO unit suffixes; performs no conversion
 * or rounding.
 */
final class FoLengths {
    /** Prevents instances of this FO length-formatting utility. */
    private FoLengths() {}

    /**
     * Formats a millimeter value for an FO length attribute.
     *
     * @param value distance already expressed in millimeters
     * @return number followed by the FO mm unit suffix
     */
    static String mm(double value) {
        return value + "mm";
    }

    /**
     * Formats a font/PDF point value for an FO length attribute.
     *
     * @param value size already expressed in PDF points
     * @return number followed by the FO pt unit suffix
     */
    static String pt(double value) {
        return value + "pt";
    }
}
