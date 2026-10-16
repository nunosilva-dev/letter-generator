package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.jsoup.nodes.Element;

/** Maps a finite inline-CSS vocabulary to the existing nullable style properties. */
public final class HtmlStyles {
    private static final int BLUE_DIGIT_INDEX = 3;
    private static final double POINTS_PER_INCH = 72.0;
    private static final double PIXELS_PER_INCH = 96.0;
    private static final double MM_PER_INCH = 25.4;
    private static final double PERCENT_SCALE = 100.0;
    private static final Pattern LENGTH =
            Pattern.compile("([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))(mm|pt|px|%)?");
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern SHORT_HEX = Pattern.compile("#[0-9a-fA-F]{3}");
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
    private static final Map<String, Double> HEADING_SIZES =
            Map.of("h1", 18d, "h2", 16d, "h3", 14d, "h4", 12d, "h5", 11d, "h6", 10d);

    private HtmlStyles() {}

    public record Appearance(Style style, String background, String width, String height) {}

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

    private static double points(String value, Element element) {
        double millimeters = lengthMm(value, 0, false, element);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.endsWith("pt")
                ? number(normalized.substring(0, normalized.length() - 2), element)
                : millimeters * POINTS_PER_INCH / MM_PER_INCH;
    }

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

    public static double number(String value, Element element) {
        try {
            double result = Double.parseDouble(value);
            MarkupSupport.require(Double.isFinite(result), element, "Number must be finite");
            return result;
        } catch (NumberFormatException exception) {
            throw MarkupSupport.error(element, "Invalid number: " + value);
        }
    }

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

    private static String family(String value, Element element) {
        String family = value.replaceAll("^['\"]|['\"]$", "");
        MarkupSupport.require(
                !family.isBlank() && !family.contains(","),
                element,
                "Supply one registered font family");
        return family;
    }
}
