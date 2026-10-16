package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

/** Text heading kept with the following block when pagination permits. */
public record Heading(List<TextRun> runs, Style style) implements Block {
    public Heading {
        runs = ImmutableLists.copyNullable(runs);
    }
}
