package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.jsoup.nodes.Element;

/** Maps a finite inline-CSS vocabulary to the existing nullable style properties. */
public final class HtmlStyles {
    /** Index of the blue component in a three-digit hexadecimal color. */
    private static final int BLUE_DIGIT_INDEX = 3;

    /** Typographic point density used for CSS conversion. */
    private static final double POINTS_PER_INCH = 72.0;

    /** CSS pixel density used for image dimensions and px lengths. */
    private static final double PIXELS_PER_INCH = 96.0;

    /** Millimeters per inch used for CSS unit conversion. */
    private static final double MM_PER_INCH = 25.4;

    /** Divisor converting percentage values to fractions. */
    private static final double PERCENT_SCALE = 100.0;

    /** Supported decimal lengths and optional mm, pt, px or percentage units. */
    private static final Pattern LENGTH =
            Pattern.compile("([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))(mm|pt|px|%)?");

    /** Accepted six-digit hexadecimal color syntax. */
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");

    /** Accepted three-digit shorthand color syntax. */
    private static final Pattern SHORT_HEX = Pattern.compile("#[0-9a-fA-F]{3}");

    /** Finite named-color vocabulary mapped to six-digit hexadecimal values. */
    private static final Map<String, String> COLORS =
            Map.ofEntries(
                    Map.entry("black", "#000000"),
                    Map.entry("white", "#FFFFFF"),
                    Map.entry("gray", "#808080"),
                    Map.entry("grey", "#808080"),
                    Map.entry("red", "#FF0000"),
                    Map.entry("green", "#008000"),
                    Map.entry("blue", "#0000FF"),
                    Map.entry("navy", "#000080"),
                    Map.entry("teal", "#008080"));

    /** Semantic heading font sizes in points. */
    private static final Map<String, Double> HEADING_SIZES =
            Map.of("h1", 18d, "h2", 16d, "h3", 14d, "h4", 12d, "h5", 11d, "h6", 10d);

    /** Prevents construction of this static helper. */
    private HtmlStyles() {}

    /**
     * Parsed CSS before the caller applies element-specific restrictions and dimension bases.
     *
     * @param style nullable-property text/block overrides, never null when returned by read
     * @param background normalized background color, or null when absent
     * @param width unconverted CSS dimension, or null when absent
     * @param height unconverted CSS dimension, or null when absent
     */
    public record Appearance(Style style, String background, String width, String height) {}

    /**
     * Reads supported inline declarations only; later declarations override earlier ones.
     * Background and dimensions are separated so text, image and table callers can restrict them.
     * No selector stylesheet or browser-computed style participates.
     *
     * @param element non-null element containing an optional style attribute
     * @return parsed text overrides plus optional background and raw dimensions
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if a declaration,
     *     property, color, font or parsed scalar value is unsupported
     */
    public static Appearance read(Element element) {
        var style = Style.builder();
        String background = null;
        String width = null;
        String height = null;
        for (String declaration : element.attr("style").split(";")) {
            if (declaration.isBlank()) {
                continue;
            }
            int colon = declaration.indexOf(':');
            MarkupSupport.require(colon > 0, element, "Invalid CSS declaration: " + declaration);
            String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = declaration.substring(colon + 1).trim();
            switch (property) {
                case "font-family" -> style.fontFamily(family(value, element));
                case "font-size" -> style.fontSizePt(points(value, element));
                case "color" -> style.color(color(value, element));
                case "font-weight" ->
                        style.bold(choice(value, "bold", "700", "normal", "400", element));
                case "font-style" ->
                        style.italic(
                                choice(value, "italic", "italic", "normal", "normal", element));
                case "text-decoration" ->
                        style.underline(
                                choice(value, "underline", "underline", "none", "none", element));
                case "text-align" -> {
                    try {
                        style.textAlign(TextAlign.valueOf(value.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException exception) {
                        throw MarkupSupport.error(element, "Unsupported text-align: " + value);
                    }
                }
                case "line-height" -> style.lineHeight(number(value, element));
                case "margin-top" -> style.spaceBeforeMm(lengthMm(value, 0, false, element));
                case "margin-bottom" -> style.spaceAfterMm(lengthMm(value, 0, false, element));
                case "background-color" -> background = color(value, element);
                case "width" -> width = value;
                case "height" -> height = value;
                default ->
                        throw MarkupSupport.error(element, "Unsupported CSS property: " + property);
            }
        }
        return new Appearance(style.build(), background, width, height);
    }

    /**
     * Resolves markup text as inherited properties, then semantic tag defaults, then local CSS.
     * Parent block spacing is dropped; local margin-top/bottom become this block's spacing.
     *
     * @param parent parent properties, or null for no inherited overrides
     * @param element non-null text element
     * @return nullable style overrides after cascading
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException for invalid CSS or
     *     dimensions/backgrounds, which are not supported on text elements
     */
    public static Style text(Style parent, Element element) {
        var appearance = read(element);
        MarkupSupport.require(
                appearance.width() == null && appearance.height() == null,
                element,
                "CSS width/height are supported only on images");
        MarkupSupport.require(
                appearance.background() == null,
                element,
                "Background colors are supported on table elements");
        return inherited(parent).overlay(semantic(element)).overlay(appearance.style());
    }

    /**
     * Supplies bold/italic/underline tag semantics and fixed heading sizes before local CSS.
     *
     * @param element non-null element whose normalized name selects the defaults
     * @return semantic overrides, or an empty style for an unstyled tag
     */
    public static Style semantic(Element element) {
        return switch (element.normalName()) {
            case "strong", "b", "th" -> Style.builder().bold().build();
            case "em", "i" -> Style.builder().italic().build();
            case "u" -> Style.builder().underline().build();
            case "h1", "h2", "h3", "h4", "h5", "h6" ->
                    Style.builder()
                            .bold()
                            .fontSizePt(HEADING_SIZES.get(element.normalName()))
                            .build();
            default -> Style.plain();
        };
    }

    /**
     * Copies inheritable text properties while clearing before/after paragraph spacing.
     *
     * @param parent nullable parent style
     * @return empty style for null, otherwise a copy without block margins
     */
    public static Style inherited(Style parent) {
        if (parent == null) {
            return Style.plain();
        }
        return new Style(
                parent.fontFamily(),
                parent.fontSizePt(),
                parent.color(),
                parent.bold(),
                parent.italic(),
                parent.underline(),
                parent.textAlign(),
                parent.lineHeight(),
                null,
                null);
    }

    /**
     * Converts CSS font size to points, preserving explicit pt values and rejecting percentages.
     *
     * @param value local value to interpret
     * @param element parsed element being read and used for diagnostics
     * @return font size in points
     */
    private static double points(String value, Element element) {
        double millimeters = lengthMm(value, 0, false, element);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.endsWith("pt")
                ? number(normalized.substring(0, normalized.length() - 2), element)
                : millimeters * POINTS_PER_INCH / MM_PER_INCH;
    }

    /**
     * Converts nonnegative mm, pt, px or percentage lengths into millimeters. Pixels use 96 dpi and
     * points use 72 per inch. A unitless zero is always accepted; other unitless values require
     * image-style pixel interpretation. Percentages require a positive reference base.
     *
     * @param value non-null length text
     * @param percentageBase percentage reference in millimeters; zero disables percentages
     * @param pixelsWhenUnitless whether nonzero unitless values mean pixels
     * @param element node used to locate errors
     * @return converted nonnegative length in millimeters
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if syntax, numeric range,
     *     unit or percentage context is unsupported
     */
    public static double lengthMm(
            String value, double percentageBase, boolean pixelsWhenUnitless, Element element) {
        var matcher = LENGTH.matcher(value.trim().toLowerCase(Locale.ROOT));
        MarkupSupport.require(matcher.matches(), element, "Invalid length: " + value);
        double amount = number(matcher.group(1), element);
        MarkupSupport.require(amount >= 0, element, "Length must be non-negative: " + value);
        String unit = matcher.group(2);
        if (unit == null) {
            MarkupSupport.require(
                    amount == 0 || pixelsWhenUnitless,
                    element,
                    "Length requires mm, pt or px: " + value);
            return amount * MM_PER_INCH / PIXELS_PER_INCH;
        }
        return switch (unit) {
            case "mm" -> amount;
            case "pt" -> amount * MM_PER_INCH / POINTS_PER_INCH;
            case "px" -> amount * MM_PER_INCH / PIXELS_PER_INCH;
            case "%" -> {
                MarkupSupport.require(
                        percentageBase > 0, element, "Percentage is unsupported here");
                yield percentageBase * amount / PERCENT_SCALE;
            }
            default -> throw MarkupSupport.error(element, "Unsupported length unit: " + unit);
        };
    }

    /**
     * Parses a finite Java double without imposing a sign constraint.
     *
     * @param value non-null numeric text
     * @param element node used to locate errors
     * @return finite numeric value
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if unparsable/non-finite
     */
    public static double number(String value, Element element) {
        try {
            double result = Double.parseDouble(value);
            MarkupSupport.require(Double.isFinite(result), element, "Number must be finite");
            return result;
        } catch (NumberFormatException exception) {
            throw MarkupSupport.error(element, "Invalid number: " + value);
        }
    }

    /**
     * Accepts the finite named-color vocabulary or three/six hexadecimal digits after #.
     *
     * @param value non-null color text; supported names are case-insensitive
     * @param element node used to locate errors
     * @return six-digit hexadecimal color, expanding shorthand and resolving names
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if unsupported
     */
    public static String color(String value, Element element) {
        String normalized = COLORS.getOrDefault(value.toLowerCase(Locale.ROOT), value);
        if (SHORT_HEX.matcher(normalized).matches()) {
            normalized =
                    "#"
                            + normalized.charAt(1)
                            + normalized.charAt(1)
                            + normalized.charAt(2)
                            + normalized.charAt(2)
                            + normalized.charAt(BLUE_DIGIT_INDEX)
                            + normalized.charAt(BLUE_DIGIT_INDEX);
        }
        MarkupSupport.require(
                HEX.matcher(normalized).matches(),
                element,
                "Expected a supported color or #RRGGBB: " + value);
        return normalized;
    }

    /**
     * Maps only the supported literal on/off spellings; arbitrary CSS keywords are rejected.
     *
     * @param value local value to interpret
     * @param yes accepted true spelling
     * @param yesAlias alternate true spelling
     * @param no accepted false spelling
     * @param noAlias alternate false spelling
     * @param element parsed element being read and used for diagnostics
     * @return true for either true spelling, false for either false spelling
     */
    private static boolean choice(
            String value, String yes, String yesAlias, String no, String noAlias, Element element) {
        MarkupSupport.require(
                value.equals(yes)
                        || value.equals(yesAlias)
                        || value.equals(no)
                        || value.equals(noAlias),
                element,
                "Unsupported style value: " + value);
        return value.equals(yes) || value.equals(yesAlias);
    }

    /**
     * Removes optional outer quotes and requires one nonblank family, with no fallback list.
     * Registration is checked later against the font registry.
     *
     * @param value local value to interpret
     * @param element parsed element being read and used for diagnostics
     * @return single unquoted family identifier
     */
    private static String family(String value, Element element) {
        String family = value.replaceAll("^['\"]|['\"]$", "");
        MarkupSupport.require(
                !family.isBlank() && !family.contains(","),
                element,
                "Supply one registered font family");
        return family;
    }
}
