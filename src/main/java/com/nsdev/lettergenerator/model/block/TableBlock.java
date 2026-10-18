package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.TableCell;
import java.util.List;

/**
 * Full-width text table with proportional column widths, an optional repeating header, and one or
 * more body rows. A column's fraction of the available width is its weight divided by the sum of
 * all weights; weights are not millimeter dimensions. Rows stay together when possible, while a
 * body table can continue across pages and repeat its header.
 *
 * <p>Generation requires positive finite weights with a finite sum, at least one column and body
 * row, and exactly one nonnull cell per column in each header/body row. Text styles inherit from
 * the table, then the cell, then the run. Cell paragraph spacing is forced to zero; table spacing
 * controls the space around the table.
 *
 * @param columnWeights relative positive column weights, one per column
 * @param header optional header cells; null becomes an empty list
 * @param rows ordered body rows; null outer/inner lists are retained for validation
 * @param style nullable table overrides, inherited by cell text
 */
public record TableBlock(
        List<Double> columnWeights, List<TableCell> header, List<List<TableCell>> rows, Style style)
        implements Block {
    /**
     * Snapshots weights, header, and each body row into unmodifiable lists. A null header becomes
     * empty; null weights, rows, inner rows, and entries remain for generation-time validation. No
     * table-shape or style checks occur here.
     *
     * @param columnWeights relative widths to copy, or null
     * @param header header to copy, or null for no header
     * @param rows rows to snapshot, or null
     * @param style optional table style overrides
     */
    public TableBlock {
        columnWeights = ImmutableLists.copyNullable(columnWeights);
        header = header == null ? List.of() : ImmutableLists.copyNullable(header);
        rows =
                rows == null
                        ? null
                        : ImmutableLists.copyNullable(
                                rows.stream().map(ImmutableLists::copyNullable).toList());
    }
}
