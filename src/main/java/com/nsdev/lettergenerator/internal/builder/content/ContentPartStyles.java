package com.nsdev.lettergenerator.internal.builder.content;

import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.ListBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.style.Style;

/** Applies final region defaults without overwriting explicit styles on supplied blocks. */
final class ContentPartStyles {
    /** Prevents instances of the shared content-styling utility. */
    private ContentPartStyles() {}

    /**
     * Applies region defaults below explicit block style fields without altering the source. Page
     * breaks pass through unchanged because they have no visual text/image style.
     *
     * @param block immutable block to adapt
     * @param defaults inherited defaults, or null to return the block unchanged
     * @return a styled block of the same supported kind, or the unchanged block
     */
    static Block apply(Block block, Style defaults) {
        if (defaults == null) {
            return block;
        }
        Style style = defaults.overlay(block.style());
        if (block instanceof Paragraph paragraph) {
            return new Paragraph(paragraph.runs(), style);
        }
        if (block instanceof Heading heading) {
            return new Heading(heading.runs(), style);
        }
        if (block instanceof ImageBlock image) {
            return new ImageBlock(
                    image.data(), image.mimeType(), image.widthMm(), image.heightMm(), style);
        }
        if (block instanceof TableBlock table) {
            return new TableBlock(table.columnWeights(), table.header(), table.rows(), style);
        }
        if (block instanceof ListBlock list) {
            return new ListBlock(list.items(), list.ordered(), style);
        }
        return block;
    }
}
