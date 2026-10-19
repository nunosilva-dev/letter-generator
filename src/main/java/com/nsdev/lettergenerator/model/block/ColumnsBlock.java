package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.layout.Column;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;

/**
 * Side-by-side independent rich-content columns, using proportional widths after physical gaps. The
 * block flows to the height of its tallest column; content does not automatically continue from one
 * column into another.
 *
 * @param columns owned nonempty column list, checked during generation
 * @param gapMm finite nonnegative gap between adjacent columns
 * @param style nullable inherited typography and outer spacing/pagination overrides
 */
public record ColumnsBlock(List<Column> columns, double gapMm, Style style) implements Block {
    /**
     * Snapshots columns without prematurely validating document geometry.
     *
     * @param columns source column list
     * @param gapMm physical intercolumn gap
     * @param style nullable block overrides
     */
    public ColumnsBlock {
        columns = ImmutableLists.copyNullable(columns);
    }
}
