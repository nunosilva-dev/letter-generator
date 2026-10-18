package com.nsdev.lettergenerator.model.table;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

/**
 * Text-only table cell whose style inherits from its enclosing {@link
 * com.nsdev.lettergenerator.model.block.TableBlock}. The background is independent of text color
 * and is transparent when omitted. Paragraph space before/after is forced to zero inside cells; use
 * the table's style for surrounding spacing.
 *
 * <p>Generation requires a nonempty valid run list and, when present, a background in {@code
 * #RRGGBB} form. The constructor only snapshots content.
 *
 * @param runs ordered cell text; null is retained for generation-time validation
 * @param style nullable text overrides layered after the table style and before run styles
 * @param backgroundColor optional six-digit hexadecimal RGB fill, or null for transparent
 */
public record TableCell(List<TextRun> runs, Style style, String backgroundColor) {
    /**
     * Snapshots runs without checking text, styles, or background color. Null lists and entries are
     * preserved for generation-time diagnostics.
     *
     * @param runs runs to copy into an unmodifiable list, or null
     * @param style nullable cell text overrides
     * @param backgroundColor optional RGB fill
     */
    public TableCell {
        runs = ImmutableLists.copyNullable(runs);
    }
}
