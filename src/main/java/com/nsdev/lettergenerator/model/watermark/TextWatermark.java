package com.nsdev.lettergenerator.model.watermark;

import com.nsdev.lettergenerator.model.layout.PageSelector;

/**
 * Centered text watermark using a built-in or registered regular font; opacity is from zero to one.
 */
public record TextWatermark(
        String text,
        String fontFamily,
        Double fontSizePt,
        String color,
        PageSelector pages,
        Double opacity,
        Double rotationDegrees)
        implements Watermark {
    private static final String DEFAULT_FONT_FAMILY = "Helvetica";
    private static final double DEFAULT_FONT_SIZE_PT = 50.0;
    private static final String DEFAULT_COLOR = "#B0B0B0";
    private static final double DEFAULT_OPACITY = 0.12;
    private static final double DEFAULT_ROTATION_DEGREES = 45.0;

    public static TextWatermark of(String text) {
        return new TextWatermark(
                text,
                DEFAULT_FONT_FAMILY,
                DEFAULT_FONT_SIZE_PT,
                DEFAULT_COLOR,
                PageSelector.ALL,
                DEFAULT_OPACITY,
                DEFAULT_ROTATION_DEGREES);
    }
}
