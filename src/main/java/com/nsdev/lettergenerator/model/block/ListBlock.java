package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.ListItem;
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
 * @param contentItems optional hierarchical items; supply this or legacy items, never both
 */
public record ListBlock(
        List<List<TextRun>> items, boolean ordered, Style style, List<ListItem> contentItems)
        implements Block {
    /**
     * Snapshots the outer list and every nonnull item list into unmodifiable lists. Null lists and
     * run entries remain available for generation-time validation.
     *
     * @param items item lists to snapshot, or null
     * @param ordered whether to number the items
     * @param style optional inherited-style overrides
     * @param contentItems optional hierarchical item source
     */
    public ListBlock {
        items =
                items == null
                        ? null
                        : ImmutableLists.copyNullable(
                                items.stream().map(ImmutableLists::copyNullable).toList());
        contentItems = ImmutableLists.copyNullable(contentItems);
    }

    /**
     * Preserves the original flat-list constructor and its unchanged inline-item semantics.
     *
     * @param items owned flat inline items
     * @param ordered whether labels are numbered
     * @param style nullable list typography and spacing
     */
    public ListBlock(List<List<TextRun>> items, boolean ordered, Style style) {
        this(items, ordered, style, null);
    }

    /**
     * Creates hierarchical items whose child lists follow their inline content.
     *
     * @param items structured items, snapshotted immediately
     * @param ordered whether labels are numbered
     * @param style nullable list typography and spacing
     * @return immutable hierarchical list
     */
    public static ListBlock nested(List<ListItem> items, boolean ordered, Style style) {
        return new ListBlock(null, ordered, style, items);
    }
}
