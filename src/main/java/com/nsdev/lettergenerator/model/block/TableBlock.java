package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.TableOptions;
import java.util.List;

/**
 * Full-width native table with positive proportional column weights, an optional repeating header
 * and ordered body rows. Cells can span columns/rows; each section must cover its complete grid
 * without holes or overlap, and header spans cannot enter the body. Cell text inherits table, cell
 * and run styles in that order. Rich cells also support ordinary paragraphs, lists and images.
 *
 * @param columnWeights positive relative weights, one per physical column
 * @param header optional single repeating header row, null becomes empty
 * @param rows ordered body rows containing cells at their first unoccupied column
 * @param style nullable table typography and spacing
 * @param options optional appearance and row policies; null preserves legacy defaults
 */
public record TableBlock(
        List<Double> columnWeights,
        List<TableCell> header,
        List<List<TableCell>> rows,
        Style style,
        TableOptions options)
        implements Block {
    /**
     * Snapshots all supplied collections, retaining null rows and entries for field-path errors.
     *
     * @param columnWeights nullable relative column widths
     * @param header nullable repeating header
     * @param rows nullable body rows
     * @param style nullable table style
     * @param options nullable appearance and row policies
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

    /**
     * Preserves the original constructor with legacy cell appearance and row keeps.
     *
     * @param columnWeights relative positive weights
     * @param header optional repeating header
     * @param rows ordered body rows
     * @param style nullable table style
     */
    public TableBlock(
            List<Double> columnWeights,
            List<TableCell> header,
            List<List<TableCell>> rows,
            Style style) {
        this(columnWeights, header, rows, style, null);
    }

    /**
     * Replaces typography while preserving spans, content, appearance and row metadata.
     *
     * @param value nullable replacement table style
     * @return immutable restyled table
     */
    public TableBlock withStyle(Style value) {
        return new TableBlock(columnWeights, header, rows, value, options);
    }
}
