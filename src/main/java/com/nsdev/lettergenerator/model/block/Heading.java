package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

/**
 * Text heading kept with the following block on the same page when layout permits. Before inherited
 * overrides, headings default to bold 14-point Helvetica; ordinary text uses 11-point regular
 * Helvetica. Generation requires a nonempty list of valid text runs.
 *
 * @param runs ordered styled text; null is preserved for generation-time validation
 * @param style optional block overrides, layered after document and section styles
 * @param semanticStyle optional tag defaults, below theme and explicit local overrides
 */
public record Heading(List<TextRun> runs, Style style, Style semanticStyle) implements Block {
    /**
     * Creates an ordinary heading without additional markup-tag defaults.
     *
     * @param runs ordered inline content
     * @param style nullable explicit overrides
     */
    public Heading(List<TextRun> runs, Style style) {
        this(runs, style, null);
    }

    /**
     * Snapshots the run list without validating text or style. Null lists and entries are preserved
     * for generation-time diagnostics.
     *
     * @param runs runs to copy into an unmodifiable list, or null
     * @param style nullable style overrides
     * @param semanticStyle nullable tag defaults
     */
    public Heading {
        runs = ImmutableLists.copyNullable(runs);
    }
}
