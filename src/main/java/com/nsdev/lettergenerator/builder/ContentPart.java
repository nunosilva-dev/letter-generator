package com.nsdev.lettergenerator.builder;

import com.nsdev.lettergenerator.internal.builder.ImageBlocks;
import com.nsdev.lettergenerator.internal.builder.PlainText;
import com.nsdev.lettergenerator.internal.markup.HtmlBlocks;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.ListBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;
import java.util.Map;

/**
 * Deferred builder content resolved against the final region geometry, asset map and style. This
 * lets HTML reference assets registered later and lets region configuration be changed before
 * building. Resolution creates blocks for the immutable document snapshot.
 */
sealed interface ContentPart {
    /**
     * Resolves one stored content part for the destination region.
     *
     * @param assets final named image assets; implementations only read them
     * @param width available region width in millimeters
     * @param height available region height in millimeters
     * @param body true for body content, where explicit page breaks are allowed
     * @param defaults inherited region style, or null to preserve block styles unchanged
     * @return immutable ordered blocks for the region
     */
    List<Block> resolve(
            Map<String, byte[]> assets, double width, double height, boolean body, Style defaults);

    /**
     * Image content decoded during registration and refitted against final region dimensions.
     *
     * @param image image snapshot whose original bytes supply the final aspect-ratio fit
     */
    record Image(ImageBlock image) implements ContentPart {
        /** {@inheritDoc} */
        @Override
        public List<Block> resolve(
                Map<String, byte[]> assets,
                double width,
                double height,
                boolean body,
                Style defaults) {
            return List.of(styled(ImageBlocks.fit(image, width, height), defaults));
        }
    }

    /**
     * Advanced model blocks, snapshotted immediately and styled when the document is built.
     *
     * @param blocks non-null ordered blocks, with no null elements
     */
    record Blocks(List<Block> blocks) implements ContentPart {
        /**
         * Copies the supplied block list so later list changes cannot affect this content part.
         *
         * @param blocks non-null block list with no null elements
         * @throws NullPointerException if the list or any element is null
         */
        public Blocks {
            blocks = List.copyOf(blocks);
        }

        /** {@inheritDoc} */
        @Override
        public List<Block> resolve(
                Map<String, byte[]> assets,
                double width,
                double height,
                boolean body,
                Style defaults) {
            return blocks.stream().map(block -> styled(block, defaults)).toList();
        }
    }

    /**
     * Region text retained until the final header/footer style is known.
     *
     * @param chunks non-null plain-text chunks, with no null elements
     */
    record Text(List<String> chunks) implements ContentPart {
        /**
         * Copies the supplied text chunks before they are retained by the mutable builder.
         *
         * @param chunks non-null chunk list with no null elements
         * @throws NullPointerException if the list or any element is null
         */
        public Text {
            chunks = List.copyOf(chunks);
        }

        /** {@inheritDoc} */
        @Override
        public List<Block> resolve(
                Map<String, byte[]> assets,
                double width,
                double height,
                boolean body,
                Style defaults) {
            return PlainText.region(defaults, chunks.toArray(String[]::new));
        }
    }

    /**
     * Supported HTML fragment parsed with the final assets and region dimensions.
     *
     * @param markup fragment already checked for null by the owning builder
     */
    record Html(String markup) implements ContentPart {
        /** {@inheritDoc} */
        @Override
        public List<Block> resolve(
                Map<String, byte[]> assets,
                double width,
                double height,
                boolean body,
                Style defaults) {
            return HtmlBlocks.fragment(markup, assets, width, height, body).stream()
                    .map(block -> styled(block, defaults))
                    .toList();
        }
    }

    /**
     * Applies region defaults below explicit block style fields without altering the source. Page
     * breaks pass through unchanged because they have no visual text/image style.
     *
     * @param block immutable block to adapt
     * @param defaults inherited defaults, or null to return the block unchanged
     * @return a styled block of the same supported kind, or the unchanged block
     */
    private static Block styled(Block block, Style defaults) {
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
