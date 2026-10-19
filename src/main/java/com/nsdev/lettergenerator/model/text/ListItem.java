package com.nsdev.lettergenerator.model.text;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.ListBlock;
import java.util.List;

/**
 * A list item's literal inline content followed by zero or more nested ordered/bulleted lists.
 * Child lists inherit the containing list's typography and indent relative to this item's body.
 *
 * @param runs owned inline content, required and nonempty during generation
 * @param children owned nested lists; null means no child lists
 */
public record ListItem(List<TextRun> runs, List<ListBlock> children) {
    /**
     * Snapshots both collections without validating text or nested list content.
     *
     * @param runs source inline runs, retaining null for path-aware validation
     * @param children source nested lists, or null for an empty list
     */
    public ListItem {
        runs = ImmutableLists.copyNullable(runs);
        children = children == null ? List.of() : ImmutableLists.copyNullable(children);
    }
}
