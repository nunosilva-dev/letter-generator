package com.nsdev.lettergenerator.model.watermark;

import com.nsdev.lettergenerator.model.layout.PageSelector;

/**
 * Text centered behind selected pages using a built-in or registered regular font. Positioning uses
 * the physical page center, independently of margins and reserved zones. Text size uses points;
 * rotation is about the page center and positive angles rotate counterclockwise in PDF coordinates.
 * Opacity ranges from zero (invisible) to one (opaque).
 *
 * <p>The canonical constructor stores every value unchanged; nulls do not select defaults.
 * Generation requires nonblank XML-compatible text, a supported family, positive finite size, a
 * {@code #RRGGBB} color, nonnull page selection, opacity in [0, 1], and finite rotation. Use {@link
 * #of(String)} for a complete set of default appearance values.
 *
 * @param text literal watermark text, required and nonblank during generation
 * @param fontFamily built-in or registered regular font family; required during generation
 * @param fontSizePt font size in points (1/72 inch); required and positive during generation
 * @param color six-digit hexadecimal RGB text color, required during generation
 * @param pages page selection, required during generation
 * @param opacity finite paint opacity between zero and one, required during generation
 * @param rotationDegrees finite counterclockwise angle in degrees, required during generation
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
    /** Regular family used by the text factory. */
    private static final String DEFAULT_FONT_FAMILY = "Helvetica";

    /** Text factory font size in points. */
    private static final double DEFAULT_FONT_SIZE_PT = 50.0;

    /** Text factory RGB fill color. */
    private static final String DEFAULT_COLOR = "#B0B0B0";

    /** Text factory paint opacity, on the zero-to-one scale. */
    private static final double DEFAULT_OPACITY = 0.12;

    /** Text factory counterclockwise rotation about page center, in degrees. */
    private static final double DEFAULT_ROTATION_DEGREES = 45.0;

    /**
     * Creates a watermark for all pages using Helvetica, 50 pt, {@code #B0B0B0}, opacity 0.12, and
     * a 45-degree counterclockwise rotation. Text is checked during generation.
     *
     * @param text literal watermark text
     * @return watermark with default appearance and the supplied text
     */
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
