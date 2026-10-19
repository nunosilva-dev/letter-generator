package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.internal.table.TableCells;
import com.nsdev.lettergenerator.internal.table.TableGrid;
import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.TableOptions;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/** Emits searchable native FO tables, including spans and ordinary cell blocks. */
final class FoTableWriter {
    /** Total width when normalizing proportional column weights. */
    private static final double FULL_WIDTH_PERCENT = 100.0;

    /** Zero inherited paragraph gaps keep compact inline cells backward compatible. */
    private static final Style NO_CELL_SPACING =
            new Style(null, null, null, null, null, null, null, null, 0d, 0d);

    /** Shared composition stream. */
    private final XmlBuilder xml;

    /** Native inline serialization callback. */
    private final BiConsumer<List<TextRun>, ResolvedStyle> textWriter;

    /** Block serialization callback retaining the composition's image registry. */
    private final BiConsumer<List<Block>, ResolvedStyle> blockWriter;

    /**
     * Shares native text and block writing with the enclosing block writer.
     *
     * @param xml composition stream
     * @param textWriter inline cell callback
     * @param blockWriter rich cell callback
     */
    FoTableWriter(
            XmlBuilder xml,
            BiConsumer<List<TextRun>, ResolvedStyle> textWriter,
            BiConsumer<List<Block>, ResolvedStyle> blockWriter) {
        this.xml = xml;
        this.textWriter = textWriter;
        this.blockWriter = blockWriter;
    }

    /**
     * Writes proportional columns, an independently spanned repeating header and body rows.
     *
     * @param table validated table
     * @param style resolved table appearance
     */
    void write(TableBlock table, ResolvedStyle style) {
        write(table, style, table.style());
    }

    /**
     * Writes a table with explicit inherited pagination overrides supplied by the block writer.
     *
     * @param table validated table
     * @param style resolved table appearance
     * @param controls nullable inherited and local pagination controls
     */
    void write(TableBlock table, ResolvedStyle style, Style controls) {
        xml.start("table")
                .attribute("table-layout", "fixed")
                .attribute("width", "100%")
                .attribute("border-collapse", "collapse");
        FoStyleWriter.block(xml, style);
        FoPaginationWriter.write(xml, controls);
        double sum = table.columnWeights().stream().mapToDouble(Double::doubleValue).sum();
        for (double weight : table.columnWeights()) {
            xml.start("table-column")
                    .attribute("column-width", (weight / sum * FULL_WIDTH_PERCENT) + "%")
                    .end();
        }
        if (!table.header().isEmpty()) {
            xml.start("table-header");
            row(
                    TableGrid.layout(
                                    List.of(table.header()),
                                    table.columnWeights().size(),
                                    "header",
                                    true)
                            .get(0),
                    table.options(),
                    -1,
                    -1,
                    style);
            xml.end();
        }
        xml.start("table-body");
        var rows = TableGrid.layout(table.rows(), table.columnWeights().size(), "rows", false);
        int[] emittedBefore = new int[rows.size() + 1];
        for (int index = 0; index < rows.size(); index++) {
            emittedBefore[index + 1] = emittedBefore[index] + (rows.get(index).isEmpty() ? 0 : 1);
        }
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).isEmpty()) {
                continue;
            }
            int next = index + 1;
            while (next < rows.size() && rows.get(next).isEmpty()) {
                next++;
            }
            row(
                    nativeCells(rows.get(index), index, emittedBefore),
                    table.options(),
                    index,
                    next,
                    style);
        }
        xml.end().end();
    }

    /**
     * Collapses fully occupied continuation rows, which have no cells or intrinsic height. Native
     * FO forbids empty table-row elements, so spans count only rows that actually start a cell.
     *
     * @param cells starting cells in a nonempty source row
     * @param rowIndex source body-row index
     * @param emittedBefore prefix count of rows with starting cells
     * @return equivalent native cell starts with adjusted row spans
     */
    private static List<TableGrid.PlacedCell> nativeCells(
            List<TableGrid.PlacedCell> cells, int rowIndex, int[] emittedBefore) {
        var result = new ArrayList<TableGrid.PlacedCell>();
        for (var placed : cells) {
            TableCell cell = placed.cell();
            if (cell.rowSpan() != null) {
                int span = emittedBefore[rowIndex + cell.rowSpan()] - emittedBefore[rowIndex];
                cell = cell.span(cell.columnSpan() == null ? 1 : cell.columnSpan(), span);
            }
            result.add(new TableGrid.PlacedCell(cell, placed.columnIndex(), placed.sourceIndex()));
        }
        return result;
    }

    /**
     * Writes row policies and starting cells; occupied cells from earlier row spans are omitted.
     *
     * @param cells located cell starts in this row
     * @param options nullable table options
     * @param rowIndex body index, or minus one for the header
     * @param nextRowIndex next emitted body index, or minus one for the header
     * @param style resolved table typography
     */
    private void row(
            List<TableGrid.PlacedCell> cells,
            TableOptions options,
            int rowIndex,
            int nextRowIndex,
            ResolvedStyle style) {
        Keep together =
                options == null || options.rowKeepTogether() == null
                        ? Keep.PREFER
                        : options.rowKeepTogether();
        Keep next = null;
        if (options != null && rowIndex >= 0) {
            for (var policy : options.rowPolicies()) {
                if (policy.rowIndex() == rowIndex) {
                    together = policy.keepTogether() == null ? together : policy.keepTogether();
                    next = policy.keepWithNext();
                }
            }
            for (var group : options.rowGroups()) {
                int end = group.startRow() + group.rowCount();
                if (rowIndex >= group.startRow() && rowIndex < end) {
                    together = stronger(together, group.keepTogether());
                    if (nextRowIndex < end) {
                        next = stronger(next, group.keepTogether());
                    }
                }
            }
        }
        xml.start("table-row")
                .attribute("keep-together.within-page", FoPaginationWriter.strength(together));
        if (next != null) {
            xml.attribute("keep-with-next.within-page", FoPaginationWriter.strength(next));
        }
        for (var placed : cells) {
            TableCell cell = placed.cell();
            CellAppearance appearance = TableCells.appearance(options, cell);
            xml.start("table-cell");
            cellAppearance(appearance);
            FoPaginationWriter.write(xml, cell.style());
            if (cell.columnSpan() != null && cell.columnSpan() != 1) {
                xml.attribute("number-columns-spanned", cell.columnSpan());
            }
            if (cell.rowSpan() != null && cell.rowSpan() != 1) {
                xml.attribute("number-rows-spanned", cell.rowSpan());
            }
            if (placed.columnIndex() != placed.sourceIndex()) {
                xml.attribute("column-number", placed.columnIndex() + 1);
            }
            ResolvedStyle resolved =
                    style.overlay(cell.semanticStyle())
                            .overlay(cell.style())
                            .overlay(NO_CELL_SPACING);
            if (cell.blocks() == null) {
                textWriter.accept(cell.runs(), resolved);
            } else if (cell.blocks().isEmpty()) {
                xml.start("block").end();
            } else {
                blockWriter.accept(cell.blocks(), resolved);
            }
            xml.end();
        }
        xml.end();
    }

    /**
     * Emits resolved padding, border, alignment and fill, retaining the legacy uniform padding
     * form.
     *
     * @param appearance complete validated cell appearance
     */
    private void cellAppearance(CellAppearance appearance) {
        var padding = appearance.padding();
        if (padding.topMm() == padding.rightMm()
                && padding.topMm() == padding.bottomMm()
                && padding.topMm() == padding.leftMm()) {
            xml.attribute("padding", FoLengths.mm(padding.topMm()));
        } else {
            xml.attribute("padding-top", FoLengths.mm(padding.topMm()))
                    .attribute("padding-right", FoLengths.mm(padding.rightMm()))
                    .attribute("padding-bottom", FoLengths.mm(padding.bottomMm()))
                    .attribute("padding-left", FoLengths.mm(padding.leftMm()));
        }
        var border = appearance.border();
        xml.attribute(
                "border",
                FoLengths.pt(border.widthPt())
                        + " "
                        + border.style().name().toLowerCase(Locale.ROOT)
                        + " "
                        + border.color());
        xml.attribute(
                "display-align",
                switch (appearance.verticalAlignment()) {
                    case TOP -> "before";
                    case MIDDLE -> "center";
                    case BOTTOM -> "after";
                });
        if (appearance.backgroundColor() != null) {
            xml.attribute("background-color", appearance.backgroundColor());
        }
    }

    /**
     * Chooses the stronger nonnull keep so row groups retain their declared whole-group policy.
     *
     * @param first nullable existing row policy
     * @param second required group policy
     * @return stronger keep strength
     */
    private static Keep stronger(Keep first, Keep second) {
        return first == null || second.ordinal() > first.ordinal() ? second : first;
    }
}
