package com.nsdev.lettergenerator.model;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;

/**
 * Named group of body blocks rendered in list order. Sections do not force page breaks or select
 * physical page regions; the name is a descriptive label. Style properties inherit from the
 * document and are overridden by individual blocks and text runs.
 *
 * @param name section label; generation requires nonblank XML-compatible text
 * @param style optional section overrides, or null to inherit the document style
 * @param blocks body content; null is preserved for generation-time validation
 */
public record Section(String name, Style style, List<Block> blocks) {
    /**
     * Snapshots the block list without validating the section or its content. Null lists and null
     * entries are retained for generation-time diagnostics.
     *
     * @param name descriptive label
     * @param style optional inherited-style overrides
     * @param blocks blocks to copy into an unmodifiable list, or null
     */
    public Section {
        blocks = ImmutableLists.copyNullable(blocks);
    }
}
