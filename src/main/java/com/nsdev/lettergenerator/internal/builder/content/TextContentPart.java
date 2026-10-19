package com.nsdev.lettergenerator.internal.builder.content;

import com.nsdev.lettergenerator.internal.content.PlainText;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;
import java.util.Map;

/**
 * Region text retained until the final header/footer style is known.
 *
 * @param chunks non-null plain-text chunks, with no null elements
 */
record TextContentPart(List<String> chunks) implements ContentPart {
    /**
     * Copies the supplied text chunks before they are retained by the mutable builder.
     *
     * @param chunks non-null chunk list with no null elements
     * @throws NullPointerException if the list or any element is null
     */
    TextContentPart {
        chunks = List.copyOf(chunks);
    }

    /** {@inheritDoc} */
    @Override
    public List<Block> resolve(
            Map<String, byte[]> assets, double width, double height, boolean body, Style defaults) {
        return PlainText.region(defaults, chunks.toArray(String[]::new));
    }
}
