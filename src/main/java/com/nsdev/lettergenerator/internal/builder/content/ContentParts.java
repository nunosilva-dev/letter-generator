package com.nsdev.lettergenerator.internal.builder.content;

import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import java.util.List;

/**
 * Creates deferred content for the fluent builder while hiding its concrete resolution strategies.
 * Public only for use across library packages; applications should use the public letter builders.
 */
public final class ContentParts {
    /** Prevents instances of this factory utility. */
    private ContentParts() {}

    /**
     * Retains a snapshot of plain-text chunks for resolution with the final region style.
     *
     * @param chunks non-null plain-text chunks, with no null elements
     * @return deferred text content independent of later changes to the supplied list
     * @throws NullPointerException if the list or any element is null
     */
    public static ContentPart text(List<String> chunks) {
        return new TextContentPart(chunks);
    }

    /**
     * Retains a snapshot of model blocks for application of the final region style.
     *
     * @param blocks non-null ordered blocks, with no null elements
     * @return deferred block content independent of later changes to the supplied list
     * @throws NullPointerException if the list or any element is null
     */
    public static ContentPart blocks(List<Block> blocks) {
        return new BlockContentPart(blocks);
    }

    /**
     * Retains an immutable image block for fitting against the final region geometry.
     *
     * @param image non-null decoded image snapshot checked by the owning builder
     * @return deferred image content
     */
    public static ContentPart image(ImageBlock image) {
        return new ImageContentPart(image);
    }

    /**
     * Defers fragment parsing until final assets, geometry and region style are available.
     *
     * @param markup non-null supported HTML fragment checked by the owning builder
     * @return deferred markup content; parsing errors are reported during resolution
     */
    public static ContentPart html(String markup) {
        return new HtmlContentPart(markup);
    }
}
