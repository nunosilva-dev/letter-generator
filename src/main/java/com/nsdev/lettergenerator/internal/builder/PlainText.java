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
    /** One or more blank lines, with optional spaces/tabs, separating paragraphs. */
    private static final String PARAGRAPH_SEPARATOR = "\\n[ \\t]*\\n(?:[ \\t]*\\n)*";

    /** Prevents construction of this static helper. */
    private PlainText() {}

    /**
     * Converts chunks in order, normalizing CRLF/CR to LF. Blank lines separate paragraphs, single
     * line feeds remain in a paragraph, and each form feed inserts a page-break block. Empty page
     * fragments around form feeds create no paragraph; a lone empty chunk does.
     *
     * @param chunks non-null text chunks without null entries
     * @return immutable ordered paragraphs and explicit page breaks
     * @throws IllegalArgumentException if the array or a chunk is null
     */
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

    /**
     * Converts repeated-region/signature text with the same paragraph rules as body text.
     *
     * @param style paragraph style, or null to inherit document defaults
     * @param chunks non-null chunks without null entries or form feeds
     * @return immutable paragraph list carrying the supplied style
     * @throws IllegalArgumentException if text is null or contains a form feed
     */
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

    /**
     * Splits on blank lines, retaining empty paragraphs and leaving run styles inherited.
     *
     * @param text non-null text with normalized line feeds
     * @param style paragraph overrides, or null to inherit
     * @param output caller-owned output list to append to
     */
    private static void paragraphs(String text, Style style, List<Block> output) {
        for (String paragraph : text.split(PARAGRAPH_SEPARATOR, -1)) {
            output.add(new Paragraph(List.of(new TextRun(paragraph, null, null)), style));
        }
    }

    /**
     * Makes paragraph and page splitting independent of the caller's newline convention.
     *
     * @param text non-null text with any common newline convention
     * @return text with CRLF and CR changed to LF
     */
    private static String normalize(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }
}
