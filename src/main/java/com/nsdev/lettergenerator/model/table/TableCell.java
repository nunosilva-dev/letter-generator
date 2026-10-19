package com.nsdev.lettergenerator.model.table;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

/**
 * Cell containing either inline runs or ordinary blocks. Runs retain the compact legacy form;
 * blocks support paragraphs, headings, lists and images, with no nested tables or page breaks.
 * Exactly one form must be supplied. An empty block list explicitly represents an empty visual
 * cell. Span and appearance checks occur during generation.
 *
 * @param runs optional nonempty ordered inline content; exclusive with blocks
 * @param style nullable typography layered after the table style
 * @param backgroundColor legacy optional six-digit RGB fill, overriding appearance fill
 * @param blocks optional block content; an empty list creates an empty cell
 * @param columnSpan optional positive column span, null means one
 * @param rowSpan optional positive row span, null means one
 * @param appearance optional cell appearance overrides
 * @param semanticStyle optional tag defaults below themes and explicit cell styles
 * @param defaultAppearance optional semantic appearance below theme and explicit table defaults
 */
public record TableCell(
        List<TextRun> runs,
        Style style,
        String backgroundColor,
        List<Block> blocks,
        Integer columnSpan,
        Integer rowSpan,
        CellAppearance appearance,
        Style semanticStyle,
        CellAppearance defaultAppearance) {
    /**
     * Preserves cells with tag-specific typography and no appearance fallback.
     *
     * @param runs inline content
     * @param style local typography
     * @param backgroundColor explicit fill
     * @param blocks rich content
     * @param columnSpan occupied columns
     * @param rowSpan occupied rows
     * @param appearance explicit appearance
     * @param semanticStyle default typography
     */
    public TableCell(
            List<TextRun> runs,
            Style style,
            String backgroundColor,
            List<Block> blocks,
            Integer columnSpan,
            Integer rowSpan,
            CellAppearance appearance,
            Style semanticStyle) {
        this(
                runs,
                style,
                backgroundColor,
                blocks,
                columnSpan,
                rowSpan,
                appearance,
                semanticStyle,
                null);
    }

    /**
     * Creates a rich cell without tag-specific semantic defaults.
     *
     * @param runs nullable inline content
     * @param style nullable explicit cell typography
     * @param backgroundColor optional fill
     * @param blocks nullable rich content
     * @param columnSpan nullable positive column span
     * @param rowSpan nullable positive row span
     * @param appearance optional border/padding/alignment/fill
     */
    public TableCell(
            List<TextRun> runs,
            Style style,
            String backgroundColor,
            List<Block> blocks,
            Integer columnSpan,
            Integer rowSpan,
            CellAppearance appearance) {
        this(runs, style, backgroundColor, blocks, columnSpan, rowSpan, appearance, null);
    }

    /**
     * Copies both content lists without validating their entries or exclusive content forms.
     *
     * @param runs nullable inline runs
     * @param style nullable cell style
     * @param backgroundColor nullable legacy cell fill
     * @param blocks nullable block list
     * @param columnSpan nullable column span
     * @param rowSpan nullable row span
     * @param appearance nullable local appearance
     * @param semanticStyle nullable tag defaults
     * @param defaultAppearance nullable builtin appearance
     */
    public TableCell {
        runs = ImmutableLists.copyNullable(runs);
        blocks = ImmutableLists.copyNullable(blocks);
    }

    /**
     * Creates a legacy text-only cell with one-column/one-row spans and default appearance.
     *
     * @param runs ordered cell runs
     * @param style nullable text overrides
     * @param backgroundColor nullable six-digit RGB fill
     */
    public TableCell(List<TextRun> runs, Style style, String backgroundColor) {
        this(runs, style, backgroundColor, null, null, null, null);
    }

    /**
     * Wraps plain text, including an empty string, in a compact cell.
     *
     * @param text cell text, validated during generation
     * @return immutable cell with a single plain run
     */
    public static TableCell text(String text) {
        return new TableCell(List.of(new TextRun(text, null, null)), null, null);
    }

    /**
     * Creates a block-content cell; an empty list creates an explicit empty cell.
     *
     * @param content cell blocks, snapshotted and validated during generation
     * @return immutable rich cell
     */
    public static TableCell content(List<Block> content) {
        return new TableCell(null, null, null, content, null, null, null);
    }

    /**
     * Returns this cell with explicit spans, preserving content and appearance.
     *
     * @param columns number of occupied columns, positive at generation
     * @param rows number of occupied rows, positive at generation
     * @return immutable spanned copy
     */
    public TableCell span(int columns, int rows) {
        return new TableCell(
                runs,
                style,
                backgroundColor,
                blocks,
                columns,
                rows,
                appearance,
                semanticStyle,
                defaultAppearance);
    }

    /**
     * Returns a copy with replacement typography and all geometry/content retained.
     *
     * @param value nullable cell typography
     * @return immutable restyled cell
     */
    public TableCell withStyle(Style value) {
        return new TableCell(
                runs,
                value,
                backgroundColor,
                blocks,
                columnSpan,
                rowSpan,
                appearance,
                semanticStyle,
                defaultAppearance);
    }

    /**
     * Returns a copy with replacement local appearance and all other properties retained.
     *
     * @param value nullable local appearance
     * @return immutable copy
     */
    public TableCell withAppearance(CellAppearance value) {
        return new TableCell(
                runs,
                style,
                backgroundColor,
                blocks,
                columnSpan,
                rowSpan,
                value,
                semanticStyle,
                defaultAppearance);
    }
}
