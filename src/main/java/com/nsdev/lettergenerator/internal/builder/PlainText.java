package com.nsdev.lettergenerator.internal.builder;

import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.ArrayList;
import java.util.List;

/** Converts application text to blocks without exposing paragraph construction to the caller. */
public final class PlainText {
    private static final String PARAGRAPH_SEPARATOR = "\\n[ \\t]*\\n(?:[ \\t]*\\n)*";

    private PlainText() {}

    public static List<Block> body(String... chunks) {
        BuilderChecks.required(chunks, "body");
        var blocks = new ArrayList<Block>();
        for (String chunk : chunks) {
            String[] pages = normalize(BuilderChecks.required(chunk, "body text")).split("\\f", -1);
            for (int index = 0; index < pages.length; index++) {
                if (index > 0) {
                    blocks.add(new PageBreakBlock());
                }
                if (!pages[index].isEmpty() || pages.length == 1) {
                    paragraphs(pages[index], null, blocks);
                }
            }
        }
        return List.copyOf(blocks);
    }

    public static List<Block> region(Style style, String... chunks) {
        BuilderChecks.required(chunks, "region text");
        var blocks = new ArrayList<Block>();
        for (String chunk : chunks) {
            String value = normalize(BuilderChecks.required(chunk, "region text"));
            BuilderChecks.require(
                    !value.contains("\f"), "Page breaks are only supported in body text");
            paragraphs(value, style, blocks);
        }
        return List.copyOf(blocks);
    }

    private static void paragraphs(String text, Style style, List<Block> output) {
        for (String paragraph : text.split(PARAGRAPH_SEPARATOR, -1)) {
            output.add(new Paragraph(List.of(new TextRun(paragraph, null, null)), style));
        }
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }
}
