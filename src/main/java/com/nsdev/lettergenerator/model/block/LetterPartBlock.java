package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.letter.LetterElement;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;

/**
 * Semantic letter content that receives element-specific theme defaults before ordinary layout.
 *
 * @param element semantic role
 * @param blocks ordered content, snapshotted
 * @param style optional local overrides
 */
public record LetterPartBlock(LetterElement element, List<Block> blocks, Style style)
        implements Block {
    /**
     * Snapshots content for shared semantic preparation.
     *
     * @param element semantic element
     * @param blocks ordered content
     * @param style optional local overrides
     */
    public LetterPartBlock {
        blocks = ImmutableLists.copyNullable(blocks);
    }
}
