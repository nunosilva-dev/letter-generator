package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.layout.BoxAppearance;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;

/**
 * Rich flowing content with optional independent borders, padding and background color.
 *
 * @param blocks owned nonempty content list, validated during generation
 * @param appearance nullable physical decoration; null means undecorated
 * @param style nullable inherited typography and outer spacing/pagination overrides
 */
public record BoxBlock(List<Block> blocks, BoxAppearance appearance, Style style) implements Block {
    /**
     * Snapshots content without validating nullable authoring values.
     *
     * @param blocks source content list
     * @param appearance nullable box decoration
     * @param style nullable block overrides
     */
    public BoxBlock {
        blocks = ImmutableLists.copyNullable(blocks);
    }
}
