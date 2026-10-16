package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.TableCell;
import java.util.List;

/**
 * Full-width text table with positive relative column weights, an optional repeating header, and at
 * least one body row. Rows stay together when possible; tables may continue across pages.
 */
public record TableBlock(
        List<Double> columnWeights, List<TableCell> header, List<List<TableCell>> rows, Style style)
        implements Block {
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
