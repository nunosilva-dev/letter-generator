package com.nsdev.lettergenerator.model;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;

/**
 * Ordered body blocks with an optional inherited style. The name is a label, not a placement
 * instruction.
 */
public record Section(String name, Style style, List<Block> blocks) {
    public Section {
        blocks = ImmutableLists.copyNullable(blocks);
    }
}
