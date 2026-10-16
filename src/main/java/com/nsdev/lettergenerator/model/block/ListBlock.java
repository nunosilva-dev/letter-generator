package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

/** Ordered or bulleted list; each item is an immutable sequence of text runs. */
public record ListBlock(List<List<TextRun>> items, boolean ordered, Style style) implements Block {
    public ListBlock {
        items =
                items == null
                        ? null
                        : ImmutableLists.copyNullable(
                                items.stream().map(ImmutableLists::copyNullable).toList());
    }
}
