package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

/**
 * Ordered or bulleted list with one sequence of text runs per item. Ordered labels start at 1;
 * unordered labels are bullets. Items are kept together on a page when possible. Generation
 * requires at least one item and at least one valid text run in every item.
 *
 * @param items item content in display order; null outer/inner lists are preserved for validation
 * @param ordered true for numbered labels, false for bullets
 * @param style optional list style inherited by each item and its runs
 */
public record ListBlock(List<List<TextRun>> items, boolean ordered, Style style) implements Block {
    /**
     * Snapshots the outer list and every nonnull item list into unmodifiable lists. Null lists and
     * run entries remain available for generation-time validation.
     *
     * @param items item lists to snapshot, or null
     * @param ordered whether to number the items
     * @param style optional inherited-style overrides
     */
    public ListBlock {
        items =
                items == null
                        ? null
                        : ImmutableLists.copyNullable(
                                items.stream().map(ImmutableLists::copyNullable).toList());
    }
}
