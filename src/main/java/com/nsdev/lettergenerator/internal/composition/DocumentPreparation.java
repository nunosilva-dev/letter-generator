package com.nsdev.lettergenerator.internal.composition;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.letter.LetterElement;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ColumnsBlock;
import com.nsdev.lettergenerator.model.block.ComponentBlock;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.LetterPartBlock;
import com.nsdev.lettergenerator.model.block.ListBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Column;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.layout.PositionedContent;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.Zone;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.text.ListItem;
import com.nsdev.lettergenerator.model.text.TextRun;
import com.nsdev.lettergenerator.theme.StyleRole;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/** Shared composition boundary that produces ordinary model values before validation/rendering. */
public final class DocumentPreparation {
    /** Prevents constructing the static preparation entry point. */
    private DocumentPreparation() {}

    /**
     * Resolves role defaults, named styles and reusable references within one document. No
     * registered value is mutated. Legacy documents retain their original model and output; a
     * prepared document has no composition definitions and can safely be prepared again. Null is
     * retained for the ordinary validator's document-required diagnostic.
     *
     * @param document nullable model from any public input path
     * @return ordinary immutable model ready for semantic validation
     * @throws InvalidLetterException for invalid, cyclic or unknown composition references
     */
    public static LetterDocument prepare(LetterDocument document) {
        return document == null ? null : new Preparation(document).prepare();
    }

    /** Per-call state prevents registrations and cycle tracking from leaking between documents. */
    private static final class Preparation {
        /** Caller-owned immutable input. */
        private final LetterDocument document;

        /** Named style resolver scoped to this input. */
        private final NamedStyles styles;

        /** Whether roles or components require materializing inherited styles. */
        private final boolean composing;

        /** Whether semantic letter elements required expansion independent of a theme. */
        private boolean expanded;

        /** Ordered active component references for cycle diagnostics. */
        private final Set<String> activeComponents = new LinkedHashSet<>();

        /**
         * Captures an immutable document and allocates isolated resolution state.
         *
         * @param document nonnull input
         */
        Preparation(LetterDocument document) {
            this.document = StationeryPreparation.prepare(document);
            styles = new NamedStyles(document.theme());
            composing = document.theme() != null || !document.components().isEmpty();
        }

        /**
         * @return fully resolved ordinary document, preserving legacy models when possible
         */
        LetterDocument prepare() {
            styles.validate();
            Style defaults = styles.resolve(document.defaultStyle(), "defaultStyle");
            Context body = new Context(orPlain(defaults), Style.plain(), Style.plain());
            for (var entry : document.components().entrySet()) {
                NamedStyles.requireName(entry.getKey(), "components");
                component(entry.getKey(), body, "components[" + entry.getKey() + "]");
            }
            List<Section> sections =
                    map(
                            document.sections(),
                            "sections",
                            (section, path) -> {
                                if (section == null) {
                                    return null;
                                }
                                Style local = styles.resolve(section.style(), path + ".style");
                                return new Section(
                                        section.name(),
                                        local,
                                        blocks(
                                                section.blocks(),
                                                body.withExplicit(local),
                                                path + ".blocks"));
                            });
            List<FixedElement> fixed =
                    map(
                            document.fixedElements(),
                            "fixedElements",
                            (element, path) -> {
                                if (element == null) {
                                    return null;
                                }
                                StyleRole role =
                                        element.zone() == Zone.TOP
                                                ? StyleRole.HEADER
                                                : StyleRole.FOOTER;
                                return new FixedElement(
                                        element.id(),
                                        element.zone(),
                                        element.pages(),
                                        element.boundsMm(),
                                        blocks(
                                                element.blocks(),
                                                body.withDefaults(styles.role(role)),
                                                path + ".blocks"));
                            });
            Signature signature = document.signature();
            if (signature != null) {
                signature =
                        new Signature(
                                signature.placement(),
                                blocks(
                                        signature.blocks(),
                                        body.withDefaults(styles.role(StyleRole.SIGNATURE)),
                                        "signature.blocks"),
                                signature.boundsMm());
            }
            if (!composing && !expanded && !styles.hasResolvedLayers()) {
                return document;
            }
            return new LetterDocument(
                    document.schemaVersion(),
                    document.page(),
                    defaults,
                    sections,
                    fixed,
                    signature,
                    document.watermark(),
                    null,
                    null,
                    document.settings(),
                    document.attachments());
        }

        /**
         * Resolves ordered blocks while retaining nulls for ordinary field-path validation.
         *
         * @param source nullable block list
         * @param context inherited explicit styles and contextual role defaults
         * @param path list input path
         * @return immutable-compatible transformed values, or null
         */
        private List<Block> blocks(List<Block> source, Context context, String path) {
            return map(source, path, (block, itemPath) -> block(block, context, itemPath));
        }

        /**
         * Rebuilds one block, preserving its content, geometry and pagination metadata.
         *
         * @param block nullable source block
         * @param context containing explicit styles and role defaults
         * @param path block input path
         * @return ordinary block with resolved references, or null
         */
        private Block block(Block block, Context context, String path) {
            if (block == null) {
                return null;
            }
            Style local = styles.resolve(block.style(), path + ".style");
            if (block instanceof ComponentBlock reference) {
                return new GroupBlock(
                        component(
                                reference.name(),
                                context.withExplicit(inheritable(local)),
                                path + ".name"),
                        local);
            }
            if (block instanceof LetterPartBlock part) {
                expanded = true;
                if (part.element() == null) {
                    throw new InvalidLetterException(
                            path + ".element", "Letter element is required");
                }
                StyleRole role = StyleRole.valueOf(part.element().name());
                Style fallback = semanticDefaults(part.element());
                Style semantic = fallback.overlay(styles.role(role));
                return new GroupBlock(
                        blocks(
                                part.blocks(),
                                context.withDefaults(styles.role(role))
                                        .withFallback(fallback)
                                        .withExplicit(inheritable(local)),
                                path + ".blocks"),
                        Style.builder()
                                .keepTogether(semantic.keepTogether())
                                .keepWithNext(semantic.keepWithNext())
                                .build()
                                .overlay(local),
                        semantic);
            }
            if (block instanceof CanvasBlock canvas) {
                Context children = context.withExplicit(inheritable(local));
                return new CanvasBlock(
                        canvas.widthMm(),
                        canvas.heightMm(),
                        map(
                                canvas.elements(),
                                path + ".elements",
                                (element, elementPath) ->
                                        positioned(element, children, elementPath)),
                        local);
            }
            if (block instanceof ColumnsBlock columns) {
                Context children = context.withExplicit(inheritable(local));
                return new ColumnsBlock(
                        map(
                                columns.columns(),
                                path + ".columns",
                                (column, columnPath) -> column(column, children, columnPath)),
                        columns.gapMm(),
                        local);
            }
            if (block instanceof BoxBlock box) {
                return new BoxBlock(
                        blocks(
                                box.blocks(),
                                context.withExplicit(inheritable(local)),
                                path + ".blocks"),
                        box.appearance(),
                        local);
            }
            if (block instanceof Heading heading) {
                Style semantic = styles.resolve(heading.semanticStyle(), path + ".semanticStyle");
                return new Heading(
                        runs(heading.runs(), path + ".runs"),
                        effective(context, StyleRole.HEADING, local, semantic),
                        composing ? null : semantic);
            }
            Style effective =
                    effective(
                            context,
                            block instanceof TableBlock ? StyleRole.TABLE : StyleRole.BODY,
                            local,
                            null);
            if (block instanceof Paragraph paragraph) {
                return new Paragraph(runs(paragraph.runs(), path + ".runs"), effective);
            }
            if (block instanceof ListBlock list) {
                Context children = context.withExplicit(inheritable(local));
                return new ListBlock(
                        map(list.items(), path + ".items", this::runs),
                        list.ordered(),
                        effective,
                        map(
                                list.contentItems(),
                                path + ".contentItems",
                                (item, itemPath) -> listItem(item, children, itemPath)));
            }
            if (block instanceof ImageBlock image) {
                return new ImageBlock(
                        image.data(),
                        image.mimeType(),
                        image.widthMm(),
                        image.heightMm(),
                        effective,
                        image.altText());
            }
            if (block instanceof GroupBlock group) {
                Style fallback = styles.resolve(group.defaultStyle(), path + ".defaultStyle");
                return new GroupBlock(
                        blocks(
                                group.blocks(),
                                context.withFallback(fallback).withExplicit(inheritable(local)),
                                path + ".blocks"),
                        local,
                        composing ? null : fallback);
            }
            if (block instanceof TableBlock table) {
                Context tableContext =
                        context.withExplicit(local).withDefaults(styles.role(StyleRole.TABLE));
                return new TableBlock(
                        table.columnWeights(),
                        cells(table.header(), tableContext, true, path + ".header"),
                        map(
                                table.rows(),
                                path + ".rows",
                                (row, rowPath) -> cells(row, tableContext, false, rowPath)),
                        effective,
                        ThemeTables.options(document.theme(), table.options()));
            }
            if (block instanceof PageBreakBlock) {
                return new PageBreakBlock();
            }
            throw new IllegalStateException(
                    "Unsupported block preparation: " + block.getClass().getName());
        }

        /**
         * Resolves one positioned child's independent style scope.
         *
         * @param element nullable source child
         * @param context inherited container scope
         * @param path source location
         * @return resolved positioned value, retaining null for validation
         */
        private PositionedContent positioned(
                PositionedContent element, Context context, String path) {
            if (element == null) {
                return null;
            }
            Style local = styles.resolve(element.style(), path + ".style");
            return new PositionedContent(
                    element.id(),
                    element.boundsMm(),
                    blocks(
                            element.blocks(),
                            context.withExplicit(inheritable(local)),
                            path + ".blocks"),
                    local);
        }

        /**
         * Resolves one independent weighted column.
         *
         * @param column nullable source column
         * @param context inherited container scope
         * @param path source location
         * @return resolved column or null
         */
        private Column column(Column column, Context context, String path) {
            if (column == null) {
                return null;
            }
            Style local = styles.resolve(column.style(), path + ".style");
            return new Column(
                    column.weight(),
                    blocks(
                            column.blocks(),
                            context.withExplicit(inheritable(local)),
                            path + ".blocks"),
                    local);
        }

        /**
         * Resolves inline styles and nested list scopes without converting legacy flat lists.
         *
         * @param item nullable hierarchical item
         * @param context parent list inheritance
         * @param path source location
         * @return resolved item or null
         */
        private ListItem listItem(ListItem item, Context context, String path) {
            return item == null
                    ? null
                    : new ListItem(
                            runs(item.runs(), path + ".runs"),
                            map(
                                    item.children(),
                                    path + ".children",
                                    (child, childPath) ->
                                            child == null
                                                    ? null
                                                    : (ListBlock)
                                                            block(child, context, childPath)));
        }

        /**
         * Resolves a component definition with a bounded active-reference stack.
         *
         * @param name requested document-local component
         * @param context inheritance at the reference site
         * @param path referring input location
         * @return ordinary included blocks
         */
        private List<Block> component(String name, Context context, String path) {
            NamedStyles.requireName(name, path);
            if (!document.components().containsKey(name)) {
                throw new InvalidLetterException(path, "Unknown component '" + name + "'");
            }
            var component = document.components().get(name);
            if (component == null || component.blocks() == null) {
                throw new InvalidLetterException(
                        "components[" + name + "]", "Component blocks are required");
            }
            if (!activeComponents.add(name)) {
                throw new InvalidLetterException(
                        path,
                        "Component cycle: "
                                + String.join(" -> ", activeComponents)
                                + " -> "
                                + name);
            }
            try {
                return blocks(component.blocks(), context, "components[" + name + "].blocks");
            } finally {
                activeComponents.remove(name);
            }
        }

        /**
         * Preserves all span, appearance and content alternatives while resolving cell styles.
         *
         * @param source nullable cells
         * @param context explicit table inheritance
         * @param header whether these cells repeat as a header
         * @param path cell-list location
         * @return transformed cell list, or null
         */
        private List<TableCell> cells(
                List<TableCell> source, Context context, boolean header, String path) {
            StyleRole role = header ? StyleRole.TABLE_HEADER : StyleRole.TABLE_CELL;
            return map(
                    source,
                    path,
                    (cell, cellPath) -> {
                        if (cell == null) {
                            return null;
                        }
                        Style local = styles.resolve(cell.style(), cellPath + ".style");
                        Style semantic =
                                styles.resolve(cell.semanticStyle(), cellPath + ".semanticStyle");
                        Context cellContext = context.withDefaults(styles.role(role));
                        return new TableCell(
                                runs(cell.runs(), cellPath + ".runs"),
                                effective(cellContext, StyleRole.BODY, local, semantic),
                                cell.backgroundColor(),
                                blocks(
                                        cell.blocks(),
                                        cellContext.withFallback(semantic).withExplicit(local),
                                        cellPath + ".blocks"),
                                cell.columnSpan(),
                                cell.rowSpan(),
                                cell.appearance(),
                                composing ? null : semantic,
                                ThemeTables.cell(
                                        document.theme(), header, cell.defaultAppearance()));
                    });
        }

        /**
         * Resolves run-local named styles without flattening the containing block into every run.
         *
         * @param source nullable runs
         * @param path list input path
         * @return transformed runs retaining links and dynamic page fields
         */
        private List<TextRun> runs(List<TextRun> source, String path) {
            return map(
                    source,
                    path,
                    (run, runPath) ->
                            run == null
                                    ? null
                                    : new TextRun(
                                            run.text(),
                                            styles.resolve(run.style(), runPath + ".style"),
                                            run.href(),
                                            run.pageNumber()));
        }

        /**
         * Layers semantic defaults, roles, explicit ancestors and local overrides in priority
         * order.
         *
         * @param context containing inheritance
         * @param role current block role
         * @param local resolved local overrides
         * @param semantic nullable HTML heading defaults
         * @return block style with all effective overrides, or original local style for legacy
         *     input
         */
        private Style effective(Context context, StyleRole role, Style local, Style semantic) {
            if (!composing) {
                return local;
            }
            Style result;
            if (document.theme() == null) {
                result = context.fallback().overlay(context.explicit()).overlay(semantic);
            } else {
                result =
                        context.fallback()
                                .overlay(semantic)
                                .overlay(styles.role(StyleRole.BODY))
                                .overlay(styles.role(role))
                                .overlay(context.defaults())
                                .overlay(context.explicit());
            }
            return result.overlay(local);
        }
    }

    /** Default subject size in typographic points. */
    private static final double SUBJECT_SIZE_PT = 14;

    /**
     * Supplies conventional letter typography beneath theme roles and local overrides.
     *
     * @param element semantic letter element
     * @return optional conventional style
     */
    private static Style semanticDefaults(LetterElement element) {
        return switch (element) {
            case SUBJECT ->
                    Style.builder()
                            .bold()
                            .fontSizePt(SUBJECT_SIZE_PT)
                            .keepWithNext(Keep.ALWAYS)
                            .build();
            case DATE -> Style.builder().textAlign(TextAlign.RIGHT).build();
            default -> Style.plain();
        };
    }

    /**
     * Separate inheritance channels keep role defaults below explicit ancestor overrides.
     *
     * @param explicit document, section and containing-block overrides
     * @param defaults contextual region/table-role defaults
     * @param fallback builtin typography below roles
     */
    private record Context(Style explicit, Style defaults, Style fallback) {
        /**
         * Adds explicit ancestor overrides.
         *
         * @param local nullable containing style
         * @return independent inheritance context
         */
        Context withExplicit(Style local) {
            return new Context(explicit.overlay(local), defaults, fallback);
        }

        /**
         * Adds fallback typography beneath role defaults.
         *
         * @param local nullable defaults
         * @return independent context
         */
        Context withFallback(Style local) {
            return new Context(explicit, defaults, fallback.overlay(local));
        }

        /**
         * Adds contextual role defaults beneath explicit values.
         *
         * @param local nullable role defaults
         * @return independent inheritance context
         */
        Context withDefaults(Style local) {
            return new Context(explicit, defaults.overlay(local), fallback);
        }
    }

    /**
     * Supplies an empty override set for convenient null-safe layering.
     *
     * @param value nullable overrides
     * @return supplied style or an empty style
     */
    private static Style orPlain(Style value) {
        return value == null ? Style.plain() : value;
    }

    /**
     * Retains a group's typography while keeping its pagination controls on the group itself.
     *
     * @param value nullable local group style
     * @return typography and spacing only, or null
     */
    private static Style inheritable(Style value) {
        return value == null
                ? null
                : new Style(
                        value.fontFamily(),
                        value.fontSizePt(),
                        value.color(),
                        value.bold(),
                        value.italic(),
                        value.underline(),
                        value.textAlign(),
                        value.lineHeight(),
                        null,
                        null);
    }

    /**
     * Maps lists while preserving invalid null lists/items for subsequent ordinary validation.
     *
     * @param <T> source item type
     * @param <R> transformed item type
     * @param source nullable source list
     * @param path input location
     * @param transform transformation receiving an indexed input path
     * @return ordered transformed list, or null
     */
    private static <T, R> List<R> map(
            List<T> source, String path, BiFunction<T, String, R> transform) {
        if (source == null) {
            return null;
        }
        List<R> result = new ArrayList<>(source.size());
        for (int index = 0; index < source.size(); index++) {
            result.add(transform.apply(source.get(index), path + "[" + index + "]"));
        }
        return result;
    }
}
