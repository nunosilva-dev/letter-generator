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

/** Deferred content keeps late asset registration independent of fluent call order. */
sealed interface ContentPart {
    List<Block> resolve(
            Map<String, byte[]> assets, double width, double height, boolean body, Style defaults);

    record Image(ImageBlock image) implements ContentPart {
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

    record Blocks(List<Block> blocks) implements ContentPart {
        public Blocks {
            blocks = List.copyOf(blocks);
        }

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

    record Text(List<String> chunks) implements ContentPart {
        public Text {
            chunks = List.copyOf(chunks);
        }

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

    record Html(String markup) implements ContentPart {
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
