package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;
import java.util.function.BiConsumer;

/** Emits native FO tables so cell text stays searchable and flows with the surrounding content. */
final class FoTableWriter {
    /** Total table width used when normalizing column weights to percentages. */
    private static final double FULL_WIDTH_PERCENT = 100.0;

    /** Uniform padding inside each table cell, in millimeters. */
    private static final double CELL_PADDING_MM = 1.0;

    /** Default white separator between native table cells. */
    private static final String CELL_BORDER = "0.25pt solid #FFFFFF";

    /** Soft preference to keep each row together without forbidding necessary page breaks. */
    private static final int ROW_KEEP_PRIORITY = 1;

    /** Zero paragraph gaps overlaid inside cells so body spacing does not inflate table rows. */
    private static final Style NO_CELL_SPACING =
            new Style(null, null, null, null, null, null, null, null, 0d, 0d);

    /** Shared composition XML stream. */
    private final XmlBuilder xml;

    /** Cell-text callback supplied by the block writer for consistent inline serialization. */
    private final BiConsumer<List<TextRun>, ResolvedStyle> textWriter;

    /**
     * Shares the XML stream and delegates cell text to the block writer so run/font/link rules stay
     * consistent.
     *
     * @param xml current composition stream
     * @param textWriter callback that emits text runs with a fully resolved cell style
     */
    FoTableWriter(XmlBuilder xml, BiConsumer<List<TextRun>, ResolvedStyle> textWriter) {
        this.xml = xml;
        this.textWriter = textWriter;
    }

    /**
     * Writes a full-width fixed-layout table, normalizing positive column weights to percentages.
     * Native table-header rows can repeat when a table spans pages.
     *
     * @param table validated rectangular table with positive column weights
     * @param style resolved table appearance before cell overrides
     */
    void write(TableBlock table, ResolvedStyle style) {
        xml.start("table")
                .attribute("table-layout", "fixed")
                .attribute("width", "100%")
                .attribute("border-collapse", "collapse");
        FoStyleWriter.block(xml, style);
        double sum = table.columnWeights().stream().mapToDouble(Double::doubleValue).sum();
        for (double weight : table.columnWeights()) {
            xml.start("table-column")
                    .attribute("column-width", (weight / sum * FULL_WIDTH_PERCENT) + "%")
                    .end();
        }
        if (!table.header().isEmpty()) {
            xml.start("table-header");
            row(table.header(), style);
            xml.end();
        }
        xml.start("table-body");
        for (List<TableCell> row : table.rows()) {
            row(row, style);
        }
        xml.end().end();
    }

    /**
     * Applies cell backgrounds, padding and soft row keeps; suppresses inherited paragraph gaps
     * inside cells while retaining their text styles.
     *
     * @param cells one validated header/body row
     * @param style resolved table appearance inherited by each cell
     */
    private void row(List<TableCell> cells, ResolvedStyle style) {
        xml.start("table-row").attribute("keep-together.within-page", ROW_KEEP_PRIORITY);
        for (TableCell cell : cells) {
            xml.start("table-cell")
                    .attribute("padding", FoLengths.mm(CELL_PADDING_MM))
                    .attribute("border", CELL_BORDER)
                    .attribute("display-align", "center");
            if (cell.backgroundColor() != null) {
                xml.attribute("background-color", cell.backgroundColor());
            }
            textWriter.accept(cell.runs(), style.overlay(cell.style()).overlay(NO_CELL_SPACING));
            xml.end();
        }
        xml.end();
    }
}
