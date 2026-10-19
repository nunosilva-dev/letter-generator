package com.nsdev.lettergenerator.internal.content;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;

/** Resolves explicit absolute SVG viewport lengths and intrinsic viewBox aspect ratios. */
final class SvgGeometry {
    /** CSS pixels per physical inch. */
    private static final double PIXELS_PER_INCH = 96;

    /** Millimeters per physical inch. */
    private static final double MILLIMETERS_PER_INCH = 25.4;

    /** CSS points per inch. */
    private static final double POINTS_PER_INCH = 72;

    /** CSS picas per inch. */
    private static final double PICAS_PER_INCH = 6;

    /** Millimeters per centimeter for absolute SVG units. */
    private static final double MILLIMETERS_PER_CENTIMETER = 10;

    /** Number of numeric values required by the SVG viewBox grammar. */
    private static final int VIEWBOX_COORDINATES = 4;

    /** Index of the fourth viewBox coordinate, its positive height. */
    private static final int HEIGHT_INDEX = 3;

    /** Minimum viewport extent, avoiding native float underflow and fitting overflow. */
    private static final double MIN_EXTENT = 0.000001;

    /** Maximum intrinsic coordinate magnitude accepted before native processing. */
    private static final double MAX_COORDINATE = 1_000_000;

    /** Finite decimal SVG number syntax, with an optional absolute unit suffix. */
    private static final Pattern LENGTH =
            Pattern.compile(
                    "([+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)(px|mm|cm|in|pt|pc)?");

    /** Prevents construction of this static geometry helper. */
    private SvgGeometry() {}

    /**
     * Resolves positive intrinsic viewport dimensions without relative units or engine defaults.
     *
     * @param root checked SVG root element
     * @return positive dimensions in CSS pixels
     * @throws IllegalArgumentException if dimensions or viewBox are absent, malformed or unbounded
     */
    static SvgImages.Dimensions dimensions(Element root) {
        double[] viewBox = viewBox(root.getAttribute("viewBox"));
        Double width = length(root.getAttribute("width"));
        Double height = length(root.getAttribute("height"));
        if (width == null && height == null && viewBox != null) {
            width = viewBox[2];
            height = viewBox[HEIGHT_INDEX];
        } else if (viewBox != null && width == null) {
            width = height * viewBox[2] / viewBox[HEIGHT_INDEX];
        } else if (viewBox != null && height == null) {
            height = width * viewBox[HEIGHT_INDEX] / viewBox[2];
        }
        require(width != null && height != null, "SVG requires dimensions or a viewBox");
        positive(width);
        positive(height);
        return new SvgImages.Dimensions(width, height);
    }

    /**
     * Parses an optional absolute CSS length and converts it to CSS pixels at 96 dpi.
     *
     * @param value root width or height attribute
     * @return positive pixel length, or null for an absent attribute
     */
    private static Double length(String value) {
        if (value.isBlank()) {
            return null;
        }
        Matcher match = LENGTH.matcher(value.trim());
        require(match.matches(), "SVG dimensions require finite absolute lengths");
        double number = Double.parseDouble(match.group(1));
        String unit = match.group(2);
        double factor =
                unit == null
                        ? 1
                        : switch (unit) {
                            case "mm" -> PIXELS_PER_INCH / MILLIMETERS_PER_INCH;
                            case "cm" ->
                                    PIXELS_PER_INCH
                                            / (MILLIMETERS_PER_INCH / MILLIMETERS_PER_CENTIMETER);
                            case "in" -> PIXELS_PER_INCH;
                            case "pt" -> PIXELS_PER_INCH / POINTS_PER_INCH;
                            case "pc" -> PIXELS_PER_INCH / PICAS_PER_INCH;
                            default -> 1;
                        };
        positive(number * factor);
        return number * factor;
    }

    /**
     * Parses and bounds a viewBox, including its origin even when explicit dimensions exist.
     *
     * @param value optional viewBox attribute
     * @return four coordinates, or null when absent
     */
    private static double[] viewBox(String value) {
        if (value.isBlank()) {
            return null;
        }
        String[] pieces = value.trim().split("[\\s,]+");
        require(pieces.length == VIEWBOX_COORDINATES, "SVG viewBox requires four numbers");
        double[] coordinates = new double[pieces.length];
        try {
            for (int index = 0; index < pieces.length; index++) {
                coordinates[index] = Double.parseDouble(pieces[index]);
                require(
                        Double.isFinite(coordinates[index])
                                && Math.abs(coordinates[index]) <= MAX_COORDINATE,
                        "SVG viewBox must be finite and bounded");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid SVG viewBox", exception);
        }
        positive(coordinates[2]);
        positive(coordinates[HEIGHT_INDEX]);
        return coordinates;
    }

    /**
     * Requires a positive finite viewport extent within the accepted coordinate range.
     *
     * @param value candidate extent in CSS pixels
     */
    private static void positive(double value) {
        require(
                Double.isFinite(value) && value >= MIN_EXTENT && value <= MAX_COORDINATE,
                "SVG dimensions must be positive, finite and bounded");
    }

    /**
     * Converts an invalid SVG precondition into an authoring exception.
     *
     * @param condition required condition
     * @param message author-facing diagnostic
     */
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
