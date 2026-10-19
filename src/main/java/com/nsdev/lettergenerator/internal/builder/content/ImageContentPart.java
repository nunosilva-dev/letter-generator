package com.nsdev.lettergenerator.internal.builder.content;

import com.nsdev.lettergenerator.internal.content.ImageBlocks;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;
import java.util.Map;

/**
 * Image content decoded during registration and refitted against final region dimensions.
 *
 * @param image image snapshot whose original bytes supply the final aspect-ratio fit
 */
record ImageContentPart(ImageBlock image) implements ContentPart {
    /** {@inheritDoc} */
    @Override
    public List<Block> resolve(
            Map<String, byte[]> assets, double width, double height, boolean body, Style defaults) {
        return List.of(ContentPartStyles.apply(ImageBlocks.fit(image, width, height), defaults));
    }
}
