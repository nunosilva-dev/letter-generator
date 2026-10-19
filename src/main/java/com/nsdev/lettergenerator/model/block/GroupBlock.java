package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.Arrays;
import java.util.List;

/**
 * Ordered content grouped under common typography and a soft page-keep preference. An explicit
 * keepTogether style overrides that preference. Group spacing applies around the container;
 * pagination controls remain local. Explicit AUTO groups in body flow may contain page breaks.
 *
 * @param blocks child blocks, snapshotted while preserving nulls for field-path validation
 * @param style nullable appearance and pagination overrides
 * @param defaultStyle nullable typography fallback below theme and explicit overrides
 */
public record GroupBlock(List<Block> blocks, Style style, Style defaultStyle) implements Block {
    /**
     * Creates an ordinary group without fallback typography.
     *
     * @param blocks ordered children
     * @param style optional explicit overrides
     */
    public GroupBlock(List<Block> blocks, Style style) {
        this(blocks, style, null);
    }

    /**
     * Snapshots the child list without checking its content or renderability.
     *
     * @param blocks child blocks, or null for generation-time validation
     * @param style nullable group overrides
     * @param defaultStyle optional fallback typography
     */
    public GroupBlock {
        blocks = ImmutableLists.copyNullable(blocks);
    }

    /**
     * Creates a group using inherited appearance and the ordinary soft keep.
     *
     * @param blocks ordered child blocks
     * @return immutable group
     */
    public static GroupBlock of(Block... blocks) {
        return new GroupBlock(Arrays.asList(blocks), null);
    }
}
