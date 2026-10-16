package com.nsdev.lettergenerator.model.layout;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.Block;
import java.util.List;

/**
 * Positioned content inside a top or bottom zone. Bounds are relative to that zone, in millimeters.
 */
public record FixedElement(
        String id, Zone zone, PageSelector pages, Bounds boundsMm, List<Block> blocks) {
    public FixedElement {
        blocks = ImmutableLists.copyNullable(blocks);
    }
}
