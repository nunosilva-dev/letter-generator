package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

/** Flowing text that may continue across pages. */
public record Paragraph(List<TextRun> runs, Style style) implements Block {
    public Paragraph {
        runs = ImmutableLists.copyNullable(runs);
    }
}
