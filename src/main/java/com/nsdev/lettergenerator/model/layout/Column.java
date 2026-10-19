package com.nsdev.lettergenerator.model.layout;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;

/**
 * Independent flowing content occupying a weighted share of its parent columns block.
 *
 * @param weight positive finite proportional width, validated during generation
 * @param blocks ordered owned content, required and nonempty during generation
 * @param style nullable inherited typography overrides
 */
public record Column(double weight, List<Block> blocks, Style style) {
    /**
     * Snapshots content without replacing null values needed by path-aware validation.
     *
     * @param weight proportional width
     * @param blocks content snapshot source
     * @param style nullable typography overrides
     */
    public Column {
        blocks = ImmutableLists.copyNullable(blocks);
    }
}
