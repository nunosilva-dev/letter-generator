package com.nsdev.lettergenerator.internal.defaults;

/** Shared layout policy for ordinary Java letters and HTML letter templates. */
public final class LetterLayoutDefaults {
    /** Initial header content-box height, excluding the body gap, in millimeters. */
    public static final double HEADER_HEIGHT_MM = 25.0;

    /** Initial footer content-box height, excluding the body gap, in millimeters. */
    public static final double FOOTER_HEIGHT_MM = 15.0;

    /** Maximum default fitted logo width in millimeters. */
    public static final double LOGO_MAX_WIDTH_MM = 45.0;

    /** Maximum default fitted logo height in millimeters. */
    public static final double LOGO_MAX_HEIGHT_MM = 20.0;

    /** Initial signature reservation and HTML image height basis in millimeters. */
    public static final double SIGNATURE_HEIGHT_MM = 20.0;

    /** Prevents instances of this shared layout-policy holder. */
    private LetterLayoutDefaults() {}
}
