package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

/**
 * Flowing literal text that can wrap and continue across pages. Text runs retain their order,
 * spaces, and line feeds; they are not parsed as HTML. Generation requires a nonempty run list with
 * nonnull text and supported style/font values.
 *
 * @param runs ordered styled text; null is preserved for generation-time validation
 * @param style optional block overrides, layered after document and section styles
 */
public record Paragraph(List<TextRun> runs, Style style) implements Block {
    /**
     * Snapshots the run list without validating text or style. Null lists and entries are preserved
     * for generation-time diagnostics.
     *
     * @param runs runs to copy into an unmodifiable list, or null
     * @param style nullable style overrides
     */
    public Paragraph {
        runs = ImmutableLists.copyNullable(runs);
    }
}
