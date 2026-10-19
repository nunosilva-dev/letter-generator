package com.nsdev.lettergenerator.internal.builder.content;

import com.nsdev.lettergenerator.internal.markup.HtmlBlocks;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;
import java.util.Map;

/**
 * Supported HTML fragment parsed with the final assets and region dimensions.
 *
 * @param markup fragment already checked for null by the owning builder
 */
record HtmlContentPart(String markup) implements ContentPart {
    /** {@inheritDoc} */
    @Override
    public List<Block> resolve(
            Map<String, byte[]> assets, double width, double height, boolean body, Style defaults) {
        return HtmlBlocks.fragment(markup, assets, width, height, body).stream()
                .map(block -> ContentPartStyles.apply(block, defaults))
                .toList();
    }
}
