package com.nsdev.lettergenerator.model.layout;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;

/**
 * One stable, bounded canvas element. Coordinates are millimeters from its canvas's top-left;
 * content order controls painting order, so later elements may overlap earlier ones.
 *
 * @param id nonblank identifier unique within its canvas, checked at generation
 * @param boundsMm positive rectangle wholly inside the canvas, checked at generation
 * @param blocks owned ordered content; page breaks are forbidden at any nesting level
 * @param style nullable inherited typography overrides
 */
public record PositionedContent(String id, Bounds boundsMm, List<Block> blocks, Style style) {
    /**
     * Snapshots content while retaining invalid nulls for path-aware generation validation.
     *
     * @param id stable element identifier
     * @param boundsMm local rectangle
     * @param blocks content snapshot source
     * @param style nullable typography overrides
     */
    public PositionedContent {
        blocks = ImmutableLists.copyNullable(blocks);
    }
}
