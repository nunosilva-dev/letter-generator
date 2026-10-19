package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.internal.composition.ThemeTables;
import com.nsdev.lettergenerator.internal.layout.Units;
import com.nsdev.lettergenerator.internal.table.TableCells;
import com.nsdev.lettergenerator.internal.table.TableGrid;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.Border;
import com.nsdev.lettergenerator.model.table.BorderStyle;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.Padding;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.TableOptions;
import com.nsdev.lettergenerator.model.table.TableRowGroup;
import com.nsdev.lettergenerator.model.table.TableRowPolicy;
import com.nsdev.lettergenerator.model.table.VerticalAlignment;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jsoup.nodes.Element;

/** Compiles native merged tables, inherited appearance and rich cell content. */
final class HtmlTables {
    /** Number of tokens in the finite width/style/color border shorthand. */
    private static final int BORDER_TOKEN_COUNT = 3;

    /** Number of edges in the four-value CSS padding shorthand. */
    private static final int PADDING_EDGE_COUNT = 4;

    /** Prevents construction of the static table compiler. */
    private HtmlTables() {}

    /**
     * Compiles table rows and independent header/body span grids. Supports column-weights,
     * colspan/rowspan, padding, border, vertical-align, table/tr row-keep and tbody keep-together.
     * Cell block images resolve against the merged inner width after padding and borders.
     *
     * @param element source table element
     * @param inherited nullable inherited text style
     * @param assets named image bytes supplied by the caller
     * @param width full available table width in millimeters
     * @param height available image-height base in millimeters
     * @return immutable table with validated span structure
     */
    static TableBlock read(
            Element element,
            Style inherited,
            Map<String, byte[]> assets,
            double width,
            double height) {
        return read(element, inherited, assets, width, height, null);
    }

    /**
     * Compiles rich cells with the same theme and explicit appearance precedence as rendering.
     *
     * @param element parsed table element
     * @param inherited nullable inherited typography
     * @param assets named encoded image bytes
     * @param width available table width in millimeters
     * @param height image fitting height basis in millimeters
     * @param theme optional final document theme
     * @return ordinary immutable table with correctly fitted rich images
     */
    static TableBlock read(
            Element element,
            Style inherited,
            Map<String, byte[]> assets,
            double width,
            double height,
            LetterTheme theme) {
        MarkupSupport.elementOnlyContent(element);
        MarkupSupport.attributes(element, "column-weights", "row-keep");
        ParsedAppearance parsed = appearance(element);
        Style style = HtmlStyles.inherited(inherited).overlay(parsed.style());
        var header = new ArrayList<TableCell>();
        var body = new ArrayList<List<TableCell>>();
        var policies = new ArrayList<TableRowPolicy>();
        var groups = new ArrayList<TableRowGroup>();
        var rich = new IdentityHashMap<TableCell, Element>();
        for (Element child : element.children()) {
            if (child.normalName().equals("tr")) {
                addRow(child, Style.plain(), null, false, header, body, policies, rich);
                continue;
            }
            boolean isHeader = child.normalName().equals("thead");
            MarkupSupport.require(
                    isHeader || child.normalName().equals("tbody"),
                    child,
                    "Unsupported table section");
            MarkupSupport.attributes(child, "keep-together");
            MarkupSupport.elementOnlyContent(child);
            ParsedAppearance section = appearance(child);
            int groupStart = body.size();
            for (Element row : child.children()) {
                MarkupSupport.require(row.normalName().equals("tr"), row, "Expected a table row");
                addRow(
                        row,
                        section.style(),
                        section.cells(),
                        isHeader,
                        header,
                        body,
                        policies,
                        rich);
            }
            if (child.hasAttr("keep-together")) {
                MarkupSupport.require(
                        !isHeader && body.size() > groupStart,
                        child,
                        "Row groups require nonempty body rows");
                groups.add(
                        new TableRowGroup(
                                groupStart,
                                body.size() - groupStart,
                                keep(child.attr("keep-together"), child)));
            }
        }
        MarkupSupport.require(!body.isEmpty(), element, "Table requires at least one body row");
        List<Double> weights;
        if (element.hasAttr("column-weights")) {
            weights = new ArrayList<>();
            double sum = 0;
            for (String value : element.attr("column-weights").split(",", -1)) {
                double weight = HtmlStyles.number(value.trim(), element);
                MarkupSupport.require(weight > 0, element, "Column weights must be positive");
                sum += weight;
                weights.add(weight);
            }
            MarkupSupport.require(
                    Double.isFinite(sum), element, "Column weight sum must be finite");
        } else {
            long count =
                    (header.isEmpty() ? body.get(0) : header)
                            .stream()
                                    .mapToLong(
                                            cell ->
                                                    cell.columnSpan() == null
                                                            ? 1
                                                            : cell.columnSpan())
                                    .sum();
            MarkupSupport.require(
                    count > 0 && count <= Integer.MAX_VALUE,
                    element,
                    "Table requires a representable positive column count");
            weights = Collections.nCopies((int) count, 1d);
        }
        var options =
                new TableOptions(
                        parsed.cells(),
                        element.hasAttr("row-keep")
                                ? keep(element.attr("row-keep"), element)
                                : null,
                        policies,
                        groups);
        var table = new TableBlock(weights, header, body, style, options);
        String path = MarkupSupport.path(element);
        var headerGrid =
                header.isEmpty()
                        ? List.<List<TableGrid.PlacedCell>>of()
                        : TableGrid.layout(List.of(header), weights.size(), path + ".header", true);
        var bodyGrid = TableGrid.layout(body, weights.size(), path + ".rows", false);
        List<TableCell> completedHeader =
                headerGrid.isEmpty()
                        ? List.of()
                        : complete(
                                headerGrid.get(0), rich, table, assets, width, height, theme, true);
        var completedRows = new ArrayList<List<TableCell>>();
        for (var row : bodyGrid) {
            completedRows.add(complete(row, rich, table, assets, width, height, theme, false));
        }
        return new TableBlock(weights, completedHeader, completedRows, style, options);
    }

    /**
     * Reads cells with temporary rich-content placeholders until the full span grid is known.
     *
     * @param row source row
     * @param parent inherited text style
     * @param inherited inherited section appearance, excluding table defaults
     * @param isHeader explicit header section marker
     * @param header output header cells
     * @param body output body rows
     * @param policies output per-body-row controls
     * @param rich identity lookup from rich placeholders to their source element
     */
    private static void addRow(
            Element row,
            Style parent,
            CellAppearance inherited,
            boolean isHeader,
            List<TableCell> header,
            List<List<TableCell>> body,
            List<TableRowPolicy> policies,
            Map<TableCell, Element> rich) {
        MarkupSupport.attributes(row, "row-keep", "keep-with-next");
        MarkupSupport.elementOnlyContent(row);
        ParsedAppearance parsed = appearance(row);
        Style rowStyle = HtmlStyles.inherited(parent).overlay(parsed.style());
        CellAppearance rowAppearance = overlay(inherited, parsed.cells());
        boolean onlyHeaders =
                !row.children().isEmpty()
                        && row.children().stream().allMatch(cell -> cell.normalName().equals("th"));
        boolean headerRow = isHeader || body.isEmpty() && header.isEmpty() && onlyHeaders;
        var cells = new ArrayList<TableCell>();
        for (Element cell : row.children()) {
            MarkupSupport.require(
                    cell.normalName().equals("td") || cell.normalName().equals("th"),
                    cell,
                    "Expected td or th");
            MarkupSupport.attributes(cell, "colspan", "rowspan");
            MarkupSupport.require(
                    cell.select("table,page-break").isEmpty(),
                    cell,
                    "Nested tables and page breaks are not supported in cells");
            ParsedAppearance local = appearance(cell);
            Style cellStyle = HtmlStyles.inherited(rowStyle).overlay(local.style());
            CellAppearance cellAppearance = overlay(rowAppearance, local.cells());
            boolean blockContent =
                    cell.children().stream().anyMatch(child -> !InlineReader.isInline(child));
            TableCell value =
                    new TableCell(
                            blockContent
                                    ? null
                                    : InlineReader.read(cell.childNodes(), Style.plain()),
                            cellStyle,
                            cellAppearance == null ? null : cellAppearance.backgroundColor(),
                            blockContent ? List.of() : null,
                            span(cell, "colspan"),
                            span(cell, "rowspan"),
                            cellAppearance,
                            HtmlStyles.semantic(cell));
            cells.add(value);
            if (blockContent) {
                rich.put(value, cell);
            }
        }
        if (headerRow) {
            MarkupSupport.require(header.isEmpty(), row, "Only one header row is supported");
            MarkupSupport.require(!cells.isEmpty(), row, "Header row must contain cells");
            MarkupSupport.require(
                    !row.hasAttr("row-keep") && !row.hasAttr("keep-with-next"),
                    row,
                    "Header row policies use the table row-keep default");
            header.addAll(cells);
        } else {
            if (row.hasAttr("row-keep") || row.hasAttr("keep-with-next")) {
                policies.add(
                        new TableRowPolicy(
                                body.size(),
                                row.hasAttr("row-keep") ? keep(row.attr("row-keep"), row) : null,
                                row.hasAttr("keep-with-next")
                                        ? keep(row.attr("keep-with-next"), row)
                                        : null));
            }
            body.add(cells);
        }
    }

    /**
     * Resolves rich content only after the validated spans determine its actual inner image width.
     *
     * @param row located cell starts
     * @param rich source lookup for rich placeholders
     * @param table table options and weights
     * @param assets supplied named image bytes
     * @param width full table width in millimeters
     * @param height image-height percentage base in millimeters
     * @param theme optional final document theme
     * @param header whether cells belong to the repeating header
     * @return completed immutable row
     */
    private static List<TableCell> complete(
            List<TableGrid.PlacedCell> row,
            Map<TableCell, Element> rich,
            TableBlock table,
            Map<String, byte[]> assets,
            double width,
            double height,
            LetterTheme theme,
            boolean header) {
        var result = new ArrayList<TableCell>();
        for (var placed : row) {
            TableCell cell = placed.cell();
            Element source = rich.get(cell);
            if (source != null) {
                double innerWidth =
                        TableCells.innerWidth(
                                table.columnWeights(),
                                placed.columnIndex(),
                                cell.columnSpan() == null ? 1 : cell.columnSpan(),
                                width,
                                ThemeTables.appearance(theme, header, table.options(), cell));
                MarkupSupport.require(
                        innerWidth > 0,
                        source,
                        "Padding and borders leave no available cell width");
                cell =
                        new TableCell(
                                null,
                                cell.style(),
                                cell.backgroundColor(),
                                HtmlBlocks.read(
                                        source,
                                        Style.plain(),
                                        assets,
                                        innerWidth,
                                        height,
                                        false,
                                        theme),
                                cell.columnSpan(),
                                cell.rowSpan(),
                                cell.appearance(),
                                cell.semanticStyle(),
                                cell.defaultAppearance());
            }
            result.add(cell);
        }
        return List.copyOf(result);
    }

    /**
     * Text and table-specific properties extracted without mutating the source tree.
     *
     * @param style text properties read by the shared CSS reader
     * @param cells nullable table-specific appearance
     */
    record ParsedAppearance(Style style, CellAppearance cells) {}

    /**
     * Separates finite table CSS from ordinary typography before invoking the shared CSS reader.
     * Padding accepts one, two or four lengths; border accepts none or width/style/color.
     *
     * @param element source table, section, row or cell
     * @return independently inheritable text and cell appearance
     */
    static ParsedAppearance appearance(Element element) {
        Element textOnly = element.clone();
        var remaining = new StringBuilder();
        Border border = null;
        Padding padding = null;
        VerticalAlignment alignment = null;
        for (String declaration : element.attr("style").split(";")) {
            if (declaration.isBlank()) {
                continue;
            }
            int colon = declaration.indexOf(':');
            MarkupSupport.require(colon > 0, element, "Invalid CSS declaration: " + declaration);
            String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = declaration.substring(colon + 1).trim();
            switch (property) {
                case "padding" -> padding = padding(value, element);
                case "border" -> border = border(value, element);
                case "vertical-align" -> {
                    try {
                        alignment = VerticalAlignment.valueOf(value.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException exception) {
                        throw MarkupSupport.error(element, "Unsupported vertical-align: " + value);
                    }
                }
                default -> remaining.append(declaration).append(';');
            }
        }
        textOnly.attr("style", remaining.toString());
        var appearance = HtmlStyles.read(textOnly);
        MarkupSupport.require(
                appearance.width() == null && appearance.height() == null,
                element,
                "Use column-weights for table widths");
        CellAppearance cells =
                border == null
                                && padding == null
                                && alignment == null
                                && appearance.background() == null
                        ? null
                        : new CellAppearance(border, padding, alignment, appearance.background());
        return new ParsedAppearance(appearance.style(), cells);
    }

    /**
     * Parses positive integral spans without accepting fractional or zero values.
     *
     * @param element source cell
     * @param name colspan or rowspan attribute name
     * @return positive explicit span, or null for the ordinary one-cell default
     */
    private static Integer span(Element element, String name) {
        if (!element.hasAttr(name)) {
            return null;
        }
        try {
            int result = Integer.parseInt(element.attr(name));
            MarkupSupport.require(result > 0, element, name + " must be positive");
            return result;
        } catch (NumberFormatException exception) {
            throw MarkupSupport.error(element, name + " must be a positive integer");
        }
    }

    /**
     * Parses the finite auto/prefer/always keep vocabulary.
     *
     * @param value attribute value
     * @param element diagnostic source
     * @return requested keep strength
     */
    private static Keep keep(String value, Element element) {
        try {
            return Keep.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw MarkupSupport.error(element, "Expected auto, prefer or always: " + value);
        }
    }

    /**
     * Parses uniform, vertical/horizontal or top/right/bottom/left millimeter-compatible lengths.
     *
     * @param value finite padding shorthand
     * @param element diagnostic source
     * @return four-edge inner spacing
     */
    private static Padding padding(String value, Element element) {
        String[] parts = value.trim().split("\\s+");
        MarkupSupport.require(
                parts.length == 1 || parts.length == 2 || parts.length == PADDING_EDGE_COUNT,
                element,
                "Padding requires one, two or four lengths");
        double top = HtmlStyles.lengthMm(parts[0], 0, false, element);
        double right = parts.length == 1 ? top : HtmlStyles.lengthMm(parts[1], 0, false, element);
        double bottom =
                parts.length == PADDING_EDGE_COUNT
                        ? HtmlStyles.lengthMm(parts[2], 0, false, element)
                        : top;
        double left =
                parts.length == PADDING_EDGE_COUNT
                        ? HtmlStyles.lengthMm(parts[PADDING_EDGE_COUNT - 1], 0, false, element)
                        : right;
        return new Padding(top, right, bottom, left);
    }

    /**
     * Parses an invisible border or the finite line-width/style/color shorthand.
     *
     * @param value border shorthand
     * @param element diagnostic source
     * @return immutable uniform border
     */
    static Border border(String value, Element element) {
        if (value.equalsIgnoreCase("none")) {
            return Border.NONE;
        }
        String[] parts = value.trim().split("\\s+");
        MarkupSupport.require(
                parts.length == BORDER_TOKEN_COUNT,
                element,
                "Border requires width, style and color");
        try {
            return new Border(
                    Units.millimetersToPoints(HtmlStyles.lengthMm(parts[0], 0, false, element)),
                    BorderStyle.valueOf(parts[1].toUpperCase(Locale.ROOT)),
                    HtmlStyles.color(parts[2], element));
        } catch (IllegalArgumentException exception) {
            throw MarkupSupport.error(element, "Unsupported border style: " + parts[1]);
        }
    }

    /**
     * Composes nullable appearance carriers without introducing default values too early.
     *
     * @param parent optional inherited overrides
     * @param local optional local overrides
     * @return combined nullable overrides
     */
    private static CellAppearance overlay(CellAppearance parent, CellAppearance local) {
        return parent == null ? local : parent.overlay(local);
    }
}
