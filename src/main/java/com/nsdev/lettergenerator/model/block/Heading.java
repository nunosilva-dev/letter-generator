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
 */
public record Heading(List<TextRun> runs, Style style) implements Block {
    /**
     * Snapshots the run list without validating text or style. Null lists and entries are preserved
     * for generation-time diagnostics.
     *
     * @param runs runs to copy into an unmodifiable list, or null
     * @param style nullable style overrides
     */
    public Heading {
        runs = ImmutableLists.copyNullable(runs);
    }
}
