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
    private static final double FULL_WIDTH_PERCENT = 100.0;
    private static final double CELL_PADDING_MM = 1.0;
    private static final String CELL_BORDER = "0.25pt solid #FFFFFF";
    private static final int ROW_KEEP_PRIORITY = 1;
    private static final Style NO_CELL_SPACING =
            new Style(null, null, null, null, null, null, null, null, 0d, 0d);
    private final XmlBuilder xml;
    private final BiConsumer<List<TextRun>, ResolvedStyle> textWriter;

    FoTableWriter(XmlBuilder xml, BiConsumer<List<TextRun>, ResolvedStyle> textWriter) {
        this.xml = xml;
        this.textWriter = textWriter;
    }

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
