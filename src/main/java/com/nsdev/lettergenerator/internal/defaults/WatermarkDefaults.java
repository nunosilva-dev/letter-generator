package com.nsdev.lettergenerator.internal.defaults;

/** Watermark policy shared by model factories and HTML watermark input. */
public final class WatermarkDefaults {
    /** Maximum default image watermark fitting width in millimeters. */
    public static final double IMAGE_MAX_WIDTH_MM = 80.0;

    /** Maximum default image watermark fitting height in millimeters. */
    public static final double IMAGE_MAX_HEIGHT_MM = 40.0;

    /** Default text/image paint opacity on the zero-to-one scale. */
    public static final double OPACITY = 0.12;

    /** Prevents instances of this shared watermark-policy holder. */
    private WatermarkDefaults() {}
}
