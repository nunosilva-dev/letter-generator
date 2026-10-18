package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.TableCell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jsoup.nodes.Element;

/** Maps rectangular text tables, including inherited header/row/cell text and background styles. */
final class HtmlTables {
    /** Prevents construction of this static helper. */
    private HtmlTables() {}

    /**
     * Reads tr children or thead/tbody groups, inheriting text and background through table,
     * section, row and cell. Requires body rows, a rectangular column count, at most one header row
     * and a positive weight for each column. Width/height, spans and block cell content fail.
     *
     * @param element parsed element being read and used for diagnostics
     * @param inherited inherited text style, or null for no overrides
     * @return rectangular table with text cells and relative column weights
     */
    static TableBlock read(Element element, Style inherited) {
        MarkupSupport.elementOnlyContent(element);
        MarkupSupport.attributes(element, "column-weights");
        var appearance = HtmlStyles.read(element);
        rejectDimensions(element, appearance);
        Style style = HtmlStyles.inherited(inherited).overlay(appearance.style());
        var header = new ArrayList<TableCell>();
        var body = new ArrayList<List<TableCell>>();
        for (Element child : element.children()) {
            if (child.normalName().equals("tr")) {
                addRow(child, style, appearance.background(), false, header, body);
            } else {
                MarkupSupport.require(
                        child.normalName().equals("thead") || child.normalName().equals("tbody"),
                        child,
                        "Unsupported table section");
                MarkupSupport.attributes(child);
                MarkupSupport.elementOnlyContent(child);
                var section = HtmlStyles.read(child);
                rejectDimensions(child, section);
                for (Element row : child.children()) {
                    MarkupSupport.require(
                            row.normalName().equals("tr"), row, "Expected a table row");
                    addRow(
                            row,
                            HtmlStyles.inherited(style).overlay(section.style()),
                            section.background() == null
                                    ? appearance.background()
                                    : section.background(),
                            child.normalName().equals("thead"),
                            header,
                            body);
                }
            }
        }
        MarkupSupport.require(!body.isEmpty(), element, "Table requires at least one body row");
        int count = body.get(0).size();
        List<Double> weights = Collections.nCopies(count, 1d);
        if (element.hasAttr("column-weights")) {
            weights = new ArrayList<>();
            for (String value : element.attr("column-weights").split(",", -1)) {
                double weight = HtmlStyles.number(value.trim(), element);
                MarkupSupport.require(weight > 0, element, "Column weights must be positive");
                weights.add(weight);
            }
        }
        MarkupSupport.require(
                weights.size() == count
                        && (header.isEmpty() || header.size() == count)
                        && body.stream().allMatch(row -> row.size() == count),
                element,
                "Every row and column weight must match the column count");
        return new TableBlock(weights, header, body, style);
    }

    /**
     * Appends one nonempty text-only row. A thead row is a header; an initial all-th row is also
     * promoted when no header/body has been recorded. Cell semantics and local styles overlay
     * inherited row text properties, while nearest specified background wins independently.
     *
     * @param row parsed table row
     * @param parent parent text style, with null meaning no overrides
     * @param background inherited background color, or null
     * @param isHeader whether the enclosing section declares a header row
     * @param header caller-owned header cell list
     * @param body caller-owned body row list
     */
    private static void addRow(
            Element row,
            Style parent,
            String background,
            boolean isHeader,
            List<TableCell> header,
            List<List<TableCell>> body) {
        MarkupSupport.attributes(row);
        MarkupSupport.elementOnlyContent(row);
        var appearance = HtmlStyles.read(row);
        rejectDimensions(row, appearance);
        Style rowStyle = HtmlStyles.inherited(parent).overlay(appearance.style());
        String rowBackground =
                appearance.background() == null ? background : appearance.background();
        var cells = new ArrayList<TableCell>();
        boolean onlyHeaders =
                !row.children().isEmpty()
                        && row.children().stream().allMatch(cell -> cell.normalName().equals("th"));
        for (Element cell : row.children()) {
            MarkupSupport.require(
                    cell.normalName().equals("td") || cell.normalName().equals("th"),
                    cell,
                    "Expected td or th");
            MarkupSupport.attributes(cell);
            var cellAppearance = HtmlStyles.read(cell);
            rejectDimensions(cell, cellAppearance);
            Style cellStyle =
                    HtmlStyles.inherited(rowStyle)
                            .overlay(HtmlStyles.semantic(cell))
                            .overlay(cellAppearance.style());
            cells.add(
                    new TableCell(
                            InlineReader.read(cell.childNodes(), cellStyle),
                            cellStyle,
                            cellAppearance.background() == null
                                    ? rowBackground
                                    : cellAppearance.background()));
        }
        MarkupSupport.require(!cells.isEmpty(), row, "Table row must contain cells");
        if (isHeader || (body.isEmpty() && header.isEmpty() && onlyHeaders)) {
            MarkupSupport.require(header.isEmpty(), row, "Only one header row is supported");
            header.addAll(cells);
        } else {
            body.add(cells);
        }
    }

    /**
     * Keeps table geometry weight-based by rejecting CSS width/height at every table level.
     *
     * @param element parsed element being read and used for diagnostics
     * @param appearance parsed CSS appearance to restrict
     */
    private static void rejectDimensions(Element element, HtmlStyles.Appearance appearance) {
        MarkupSupport.require(
                appearance.width() == null && appearance.height() == null,
                element,
                "Use column-weights for table widths");
    }
}
