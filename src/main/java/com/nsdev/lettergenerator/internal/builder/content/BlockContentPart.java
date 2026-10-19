package com.nsdev.lettergenerator.internal.builder.content;

import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;
import java.util.Map;

/**
 * Advanced model blocks, snapshotted immediately and styled when the document is built.
 *
 * @param blocks non-null ordered blocks, with no null elements
 */
record BlockContentPart(List<Block> blocks) implements ContentPart {
    /**
     * Copies the supplied block list so later list changes cannot affect this content part.
     *
     * @param blocks non-null block list with no null elements
     * @throws NullPointerException if the list or any element is null
     */
    BlockContentPart {
        blocks = List.copyOf(blocks);
    }

    /** {@inheritDoc} */
    @Override
    public List<Block> resolve(
            Map<String, byte[]> assets, double width, double height, boolean body, Style defaults) {
        return blocks.stream().map(block -> ContentPartStyles.apply(block, defaults)).toList();
    }
}
