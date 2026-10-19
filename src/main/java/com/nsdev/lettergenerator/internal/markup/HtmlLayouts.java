package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ColumnsBlock;
import com.nsdev.lettergenerator.model.layout.Borders;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.BoxAppearance;
import com.nsdev.lettergenerator.model.layout.Column;
import com.nsdev.lettergenerator.model.layout.PositionedContent;
import com.nsdev.lettergenerator.model.table.Border;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import org.jsoup.nodes.Element;

/** Compiles the finite canvas, independent-column and decorated-box markup vocabulary. */
final class HtmlLayouts {
    /** Prevents utility instances. */
    private HtmlLayouts() {}

    /**
     * Reads one bounded canvas with stable child identifiers in painting order.
     *
     * @param source canvas element
     * @param assets owned image registrations
     * @param width available parent width
     * @param height parent image-height basis
     * @param theme optional final document theme for nested table image geometry
     * @return shared immutable canvas model
     */
    static CanvasBlock canvas(
            Element source,
            Map<String, byte[]> assets,
            double width,
            double height,
            LetterTheme theme) {
        MarkupSupport.attributes(source, "width-mm", "height-mm");
        MarkupSupport.elementOnlyContent(source);
        double canvasWidth = number(source, "width-mm", width);
        double canvasHeight = number(source, "height-mm", Double.NaN);
        var elements = new ArrayList<PositionedContent>();
        for (Element child : source.children()) {
            MarkupSupport.require(
                    child.normalName().equals("box"),
                    child,
                    "Canvas requires positioned box children");
            MarkupSupport.attributes(child, "id", "x-mm", "y-mm", "width-mm", "height-mm");
            Bounds bounds =
                    new Bounds(
                            number(child, "x-mm", 0),
                            number(child, "y-mm", 0),
                            number(child, "width-mm", Double.NaN),
                            number(child, "height-mm", Double.NaN));
            elements.add(
                    new PositionedContent(
                            child.attr("id"),
                            bounds,
                            HtmlBlocks.read(
                                    child,
                                    null,
                                    assets,
                                    bounds.width(),
                                    bounds.height(),
                                    false,
                                    theme),
                            HtmlStyles.text(null, child)));
        }
        return new CanvasBlock(canvasWidth, canvasHeight, elements, HtmlStyles.text(null, source));
    }

    /**
     * Reads weighted columns with independent rich content.
     *
     * @param source columns element
     * @param assets image registrations
     * @param width available parent width
     * @param height image-height basis
     * @param theme optional final document theme for nested table image geometry
     * @return shared column model
     */
    static ColumnsBlock columns(
            Element source,
            Map<String, byte[]> assets,
            double width,
            double height,
            LetterTheme theme) {
        MarkupSupport.attributes(source, "gap-mm");
        MarkupSupport.elementOnlyContent(source);
        double gap = number(source, "gap-mm", 0);
        var columns = new ArrayList<Column>();
        double total = 0;
        for (Element child : source.children()) {
            MarkupSupport.require(
                    child.normalName().equals("column"), child, "Columns require column children");
            MarkupSupport.attributes(child, "weight");
            total += number(child, "weight", 1);
        }
        double available = width - gap * Math.max(0, source.childrenSize() - 1);
        for (Element child : source.children()) {
            double weight = number(child, "weight", 1);
            columns.add(
                    new Column(
                            weight,
                            HtmlBlocks.read(
                                    child,
                                    null,
                                    assets,
                                    available * weight / total,
                                    height,
                                    false,
                                    theme),
                            HtmlStyles.text(null, child)));
        }
        return new ColumnsBlock(columns, gap, HtmlStyles.text(null, source));
    }

    /**
     * Reads flow decoration, retaining a separate border for each physical edge.
     *
     * @param source decorated box
     * @param assets image registrations
     * @param width available width
     * @param height image-height basis
     * @param body whether body-level page breaks are permitted
     * @param theme optional final document theme for nested table image geometry
     * @return shared flowing box
     */
    static BoxBlock box(
            Element source,
            Map<String, byte[]> assets,
            double width,
            double height,
            boolean body,
            LetterTheme theme) {
        MarkupSupport.attributes(source);
        Element common = source.clone();
        var remaining = new StringBuilder();
        var edges = new java.util.HashMap<String, Border>();
        for (String declaration : source.attr("style").split(";")) {
            if (declaration.isBlank()) {
                continue;
            }
            int colon = declaration.indexOf(':');
            MarkupSupport.require(colon > 0, source, "Invalid CSS declaration");
            String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            if (java.util.Set.of("border-top", "border-right", "border-bottom", "border-left")
                    .contains(property)) {
                edges.put(
                        property,
                        HtmlTables.border(declaration.substring(colon + 1).trim(), source));
            } else {
                remaining.append(declaration).append(';');
            }
        }
        common.attr("style", remaining.toString());
        var parsed = HtmlTables.appearance(common);
        var cells = parsed.cells();
        Border uniform = cells == null ? null : cells.border();
        Borders borders =
                new Borders(
                        edges.getOrDefault("border-top", uniform),
                        edges.getOrDefault("border-right", uniform),
                        edges.getOrDefault("border-bottom", uniform),
                        edges.getOrDefault("border-left", uniform));
        BoxAppearance appearance =
                new BoxAppearance(
                        cells == null ? null : cells.padding(),
                        borders,
                        cells == null ? null : cells.backgroundColor());
        double innerWidth = width;
        if (appearance.padding() != null) {
            innerWidth -= appearance.padding().leftMm() + appearance.padding().rightMm();
        }
        innerWidth -= borderWidth(borders.left()) + borderWidth(borders.right());
        return new BoxBlock(
                HtmlBlocks.read(source, null, assets, innerWidth, height, body, theme),
                appearance,
                parsed.style());
    }

    /**
     * Converts visible physical borders to millimeters.
     *
     * @param border optional border
     * @return horizontal occupied width
     */
    private static double borderWidth(Border border) {
        return border == null
                        || border.style() == com.nsdev.lettergenerator.model.table.BorderStyle.NONE
                ? 0
                : com.nsdev.lettergenerator.internal.layout.Units.pointsToMillimeters(
                        border.widthPt());
    }

    /**
     * Parses a finite dimension or a caller-selected missing-attribute default.
     *
     * @param element source node
     * @param name dimension name
     * @param fallback missing-attribute default
     * @return dimension in millimeters or proportional weight
     */
    private static double number(Element element, String name, double fallback) {
        try {
            double value =
                    element.hasAttr(name) ? Double.parseDouble(element.attr(name)) : fallback;
            MarkupSupport.require(Double.isFinite(value), element, name + " must be finite");
            return value;
        } catch (NumberFormatException exception) {
            throw MarkupSupport.error(element, name + " must be a number");
        }
    }
}
