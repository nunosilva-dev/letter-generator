package com.nsdev.lettergenerator.model.table;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

/**
 * Text-only table cell. An omitted background is transparent; text styles inherit from the table.
 * Paragraph spacing is fixed at zero inside cells; table styling controls surrounding spacing.
 */
public record TableCell(List<TextRun> runs, Style style, String backgroundColor) {
    public TableCell {
        runs = ImmutableLists.copyNullable(runs);
    }
}
