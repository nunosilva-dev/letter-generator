package com.nsdev.lettergenerator.model.table;

/**
 * Uniform border on all four cell edges. Values are checked at generation time.
 *
 * @param widthPt nonnegative line width in points
 * @param style required finite line style
 * @param color required six-digit RGB color, including for an invisible border
 */
public record Border(double widthPt, BorderStyle style, String color) {
    /** Legacy separator line width, in points. */
    private static final double DEFAULT_WIDTH_PT = 0.25;

    /** Legacy separator RGB color. */
    private static final String DEFAULT_COLOR = "#FFFFFF";

    /** Legacy white cell separator, one quarter of a point wide. */
    public static final Border DEFAULT =
            new Border(DEFAULT_WIDTH_PT, BorderStyle.SOLID, DEFAULT_COLOR);

    /** Explicitly invisible border. */
    public static final Border NONE = new Border(0, BorderStyle.NONE, DEFAULT_COLOR);
}
