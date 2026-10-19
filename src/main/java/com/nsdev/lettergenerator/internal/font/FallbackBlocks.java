package com.nsdev.lettergenerator.internal.font;

import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ColumnsBlock;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ListBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Column;
import com.nsdev.lettergenerator.model.layout.PositionedContent;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.text.ListItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/** Mirrors native block typography inheritance when preparing explicit fallback runs. */
final class FallbackBlocks {
    /** Document-level explicit overrides, below section and block overrides. */
    private final Style documentStyle;

    /** Per-call inline resolver and diagnostics. */
    private final FallbackRuns runs;

    /**
     * Captures the native composition defaults for one immutable document.
     *
     * @param documentStyle nullable document typography
     * @param runs configured inline fallback preparation
     */
    FallbackBlocks(Style documentStyle, FallbackRuns runs) {
        this.documentStyle = documentStyle;
        this.runs = runs;
    }

    /**
     * Visits ordinary blocks with the same typography overlay order as the native FO writer.
     *
     * @param blocks nullable content
     * @param section nullable explicit containing typography
     * @param fallback nullable lower-priority group defaults
     * @param path source path
     * @return immutable-compatible replacement content preserving invalid nulls
     */
    List<Block> prepare(List<Block> blocks, Style section, Style fallback, String path) {
        return map(
                blocks,
                path,
                (block, blockPath) -> {
                    if (block == null) {
                        return null;
                    }
                    ResolvedStyle style =
                            ResolvedStyle.defaults(block instanceof Heading)
                                    .overlay(fallback)
                                    .overlay(documentStyle)
                                    .overlay(section)
                                    .overlay(
                                            block instanceof Heading heading
                                                    ? heading.semanticStyle()
                                                    : null)
                                    .overlay(block.style());
                    if (block instanceof Paragraph paragraph) {
                        return new Paragraph(
                                runs.prepare(paragraph.runs(), style, blockPath + ".runs"),
                                paragraph.style());
                    }
                    if (block instanceof Heading heading) {
                        return new Heading(
                                runs.prepare(heading.runs(), style, blockPath + ".runs"),
                                heading.style(),
                                heading.semanticStyle());
                    }
                    if (block instanceof ListBlock list) {
                        return list(list, style, section, fallback, blockPath);
                    }
                    if (block instanceof GroupBlock group) {
                        Style parent = overlay(section, typography(group.style()));
                        Style defaults = overlay(fallback, group.defaultStyle());
                        return new GroupBlock(
                                prepare(group.blocks(), parent, defaults, blockPath + ".blocks"),
                                group.style(),
                                group.defaultStyle());
                    }
                    if (block instanceof CanvasBlock canvas) {
                        Style parent = overlay(section, typography(canvas.style()));
                        return new CanvasBlock(
                                canvas.widthMm(),
                                canvas.heightMm(),
                                map(
                                        canvas.elements(),
                                        blockPath + ".elements",
                                        (element, p) ->
                                                element == null
                                                        ? null
                                                        : new PositionedContent(
                                                                element.id(),
                                                                element.boundsMm(),
                                                                prepare(
                                                                        element.blocks(),
                                                                        overlay(
                                                                                parent,
                                                                                typography(
                                                                                        element
                                                                                                .style())),
                                                                        fallback,
                                                                        p + ".blocks"),
                                                                element.style())),
                                canvas.style());
                    }
                    if (block instanceof ColumnsBlock columns) {
                        Style parent = overlay(section, typography(columns.style()));
                        return new ColumnsBlock(
                                map(
                                        columns.columns(),
                                        blockPath + ".columns",
                                        (column, p) ->
                                                column == null
                                                        ? null
                                                        : new Column(
                                                                column.weight(),
                                                                prepare(
                                                                        column.blocks(),
                                                                        overlay(
                                                                                parent,
                                                                                typography(
                                                                                        column
                                                                                                .style())),
                                                                        fallback,
                                                                        p + ".blocks"),
                                                                column.style())),
                                columns.gapMm(),
                                columns.style());
                    }
                    if (block instanceof BoxBlock box) {
                        return new BoxBlock(
                                prepare(
                                        box.blocks(),
                                        overlay(section, typography(box.style())),
                                        fallback,
                                        blockPath + ".blocks"),
                                box.appearance(),
                                box.style());
                    }
                    if (block instanceof TableBlock table) {
                        return new TableBlock(
                                table.columnWeights(),
                                cells(table.header(), style, blockPath + ".header"),
                                map(
                                        table.rows(),
                                        blockPath + ".rows",
                                        (row, rowPath) -> cells(row, style, rowPath)),
                                table.style(),
                                table.options());
                    }
                    return block;
                });
    }

    /**
     * Preserves either list form and recursively resolves nested child faces.
     *
     * @param list source list
     * @param style resolved list typography
     * @param section parent explicit inheritance
     * @param fallback lower-priority group defaults
     * @param path exact model location
     * @return list retaining its original content form
     */
    private ListBlock list(
            ListBlock list, ResolvedStyle style, Style section, Style fallback, String path) {
        Style parent = overlay(section, typography(list.style()));
        return new ListBlock(
                map(
                        list.items(),
                        path + ".items",
                        (item, itemPath) -> runs.prepare(item, style, itemPath)),
                list.ordered(),
                list.style(),
                map(
                        list.contentItems(),
                        path + ".contentItems",
                        (item, itemPath) ->
                                item == null
                                        ? null
                                        : new ListItem(
                                                runs.prepare(
                                                        item.runs(), style, itemPath + ".runs"),
                                                map(
                                                        item.children(),
                                                        itemPath + ".children",
                                                        (child, childPath) ->
                                                                nested(
                                                                        child, parent, fallback,
                                                                        childPath)))));
    }

    /**
     * Resolves an exact nested-list location with ordinary list typography precedence.
     *
     * @param child nullable child list
     * @param section inherited explicit typography
     * @param fallback lower-priority defaults
     * @param path exact child location
     * @return prepared child or null
     */
    private ListBlock nested(ListBlock child, Style section, Style fallback, String path) {
        if (child == null) {
            return null;
        }
        ResolvedStyle resolved =
                ResolvedStyle.defaults(false)
                        .overlay(fallback)
                        .overlay(documentStyle)
                        .overlay(section)
                        .overlay(child.style());
        return list(child, resolved, section, fallback, path);
    }

    /**
     * Preserves spans and appearance while applying native cell typography to text or rich blocks.
     *
     * @param cells nullable ordered cells
     * @param table inherited table typography
     * @param path source cell list
     * @return replacement cells
     */
    private List<TableCell> cells(List<TableCell> cells, ResolvedStyle table, String path) {
        return map(
                cells,
                path,
                (cell, cellPath) -> {
                    if (cell == null) {
                        return null;
                    }
                    ResolvedStyle style = table.overlay(cell.semanticStyle()).overlay(cell.style());
                    return new TableCell(
                            runs.prepare(cell.runs(), style, cellPath + ".runs"),
                            cell.style(),
                            cell.backgroundColor(),
                            prepare(cell.blocks(), style.toStyle(), null, cellPath + ".blocks"),
                            cell.columnSpan(),
                            cell.rowSpan(),
                            cell.appearance(),
                            cell.semanticStyle(),
                            cell.defaultAppearance());
                });
    }

    /**
     * Excludes pagination policies when inheriting group typography, matching native composition.
     *
     * @param style optional local scope
     * @return inheritable typography only
     */
    private static Style typography(Style style) {
        return style == null
                ? null
                : new Style(
                        style.fontFamily(),
                        style.fontSizePt(),
                        style.color(),
                        style.bold(),
                        style.italic(),
                        style.underline(),
                        style.textAlign(),
                        style.lineHeight(),
                        style.spaceBeforeMm(),
                        style.spaceAfterMm());
    }

    /**
     * Joins nullable explicit typography layers.
     *
     * @param base lower-priority scope
     * @param local higher-priority scope
     * @return merged overrides
     */
    static Style overlay(Style base, Style local) {
        return (base == null ? Style.plain() : base).overlay(local);
    }

    /**
     * Maps optional model lists while preserving invalid entries for shared validation.
     *
     * @param <T> list element type
     * @param source nullable source list
     * @param path list model path
     * @param mapper entry transformation receiving its indexed path
     * @return transformed list or null
     */
    static <T> List<T> map(List<T> source, String path, BiFunction<T, String, T> mapper) {
        if (source == null) {
            return null;
        }
        List<T> result = new ArrayList<>();
        for (int index = 0; index < source.size(); index++) {
            result.add(mapper.apply(source.get(index), path + "[" + index + "]"));
        }
        return result;
    }
}
