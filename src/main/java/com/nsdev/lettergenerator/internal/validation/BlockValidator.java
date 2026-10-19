package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ColumnsBlock;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.ListBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

/**
 * Walks model blocks and inline runs using the same style overlay order as FO rendering. Checks
 * font variants, text/link validity, table shape and image widths before composition.
 */
final class BlockValidator {
    /** Generator font snapshot used to check resolved family/weight/slant combinations. */
    private final FontRegistry fonts;

    /** Shared validator for explicitly supplied typography and spacing overrides. */
    private final StyleValidator styles;

    /**
     * Binds block validation to the generator font snapshot and shared style validator.
     *
     * @param fonts generator font snapshot
     * @param styles validator for unresolved nullable style properties
     */
    BlockValidator(FontRegistry fonts, StyleValidator styles) {
        this.fonts = fonts;
        this.styles = styles;
    }

    /**
     * Validates fixed-region or signature blocks at the supplied width, forbidding page breaks.
     * Optional styles inherit from document to section to block to run.
     *
     * @param blocks ordered content list; required, though an empty list is allowed here
     * @param path model-field path used to locate any validation error
     * @param defaults nullable document-wide style overrides
     * @param section nullable enclosing-section overrides
     * @param width available content width in millimeters
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if blocks or their content
     *     are invalid, including any page break
     */
    void validate(List<Block> blocks, String path, Style defaults, Style section, double width) {
        validate(blocks, path, defaults, section, width, false);
    }

    /**
     * Validates ordered blocks; only body callers enable page breaks. Width limits are in
     * millimeters, while font sizes are resolved in points.
     *
     * @param blocks ordered content list; required, though an empty list is allowed here
     * @param path model-field path used to locate any validation error
     * @param defaults nullable document-wide style overrides
     * @param section nullable enclosing-section overrides
     * @param width available content width in millimeters
     * @param pageBreaksAllowed true only for body-section content
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if block structure, text,
     *     styles, images, or page-break placement is invalid
     */
    void validate(
            List<Block> blocks,
            String path,
            Style defaults,
            Style section,
            double width,
            boolean pageBreaksAllowed) {
        ValidationChecks.required(blocks, path);
        for (int index = 0; index < blocks.size(); index++) {
            String blockPath = path + "[" + index + "]";
            Block block = blocks.get(index);
            ValidationChecks.required(block, blockPath);
            if (block instanceof PageBreakBlock) {
                ValidationChecks.require(
                        pageBreaksAllowed,
                        blockPath,
                        "page breaks are only allowed in body sections");
                continue;
            }
            styles.validate(block.style(), blockPath + ".style");
            if (block instanceof Heading heading) {
                styles.validate(heading.semanticStyle(), blockPath + ".semanticStyle");
            }
            ResolvedStyle resolved =
                    ResolvedStyle.defaults(block instanceof Heading)
                            .overlay(defaults)
                            .overlay(section)
                            .overlay(
                                    block instanceof Heading heading
                                            ? heading.semanticStyle()
                                            : null)
                            .overlay(block.style());
            fonts.requireVariant(
                    resolved.fontFamily(), resolved.variant(), blockPath + ".style.fontFamily");
            if (block instanceof GroupBlock group) {
                ValidationChecks.required(group.blocks(), blockPath + ".blocks");
                ValidationChecks.require(
                        !group.blocks().isEmpty(), blockPath + ".blocks", "must not be empty");
                styles.validate(group.defaultStyle(), blockPath + ".defaultStyle");
                Style parent =
                        (group.defaultStyle() == null ? Style.plain() : group.defaultStyle())
                                .overlay(defaults)
                                .overlay(section)
                                .overlay(group.style());
                validate(
                        group.blocks(),
                        blockPath + ".blocks",
                        parent,
                        null,
                        width,
                        pageBreaksAllowed
                                && group.style() != null
                                && group.style().keepTogether() == Keep.AUTO);
            } else if (block instanceof CanvasBlock canvas) {
                new LayoutValidator(this, styles)
                        .canvas(
                                canvas,
                                blockPath,
                                containerStyle(defaults, section, canvas.style()),
                                width);
            } else if (block instanceof ColumnsBlock columns) {
                new LayoutValidator(this, styles)
                        .columns(
                                columns,
                                blockPath,
                                containerStyle(defaults, section, columns.style()),
                                width);
            } else if (block instanceof BoxBlock box) {
                new LayoutValidator(this, styles)
                        .box(
                                box,
                                blockPath,
                                containerStyle(defaults, section, box.style()),
                                width,
                                pageBreaksAllowed);
            } else if (block instanceof Paragraph paragraph) {
                runs(paragraph.runs(), blockPath + ".runs", resolved);
            } else if (block instanceof Heading heading) {
                runs(heading.runs(), blockPath + ".runs", resolved);
            } else if (block instanceof ListBlock list) {
                list(list, blockPath, resolved);
            } else if (block instanceof TableBlock table) {
                new TableValidator(styles, this).validate(table, blockPath, resolved, width);
            } else if (block instanceof ImageBlock image) {
                if (image.altText() != null) {
                    ValidationChecks.text(image.altText(), blockPath + ".altText");
                }
                ImageValidator.validate(
                        image.data(),
                        image.mimeType(),
                        image.widthMm(),
                        image.heightMm(),
                        blockPath);
                ValidationChecks.require(
                        image.widthMm() <= width + PageGeometry.GEOMETRY_TOLERANCE_MM,
                        blockPath + ".widthMm",
                        "image exceeds available width");
            }
        }
    }

    /**
     * Retains only declared ancestor values so native heading defaults remain visible to children.
     * Materializing ordinary body defaults here would hide required bold heading faces.
     *
     * @param defaults nullable document or group defaults
     * @param section nullable containing typography
     * @param local nullable container style
     * @return declared inheritance without synthesized body typography
     */
    private static Style containerStyle(Style defaults, Style section, Style local) {
        return (defaults == null ? Style.plain() : defaults).overlay(section).overlay(local);
    }

    /**
     * Requires at least one nonempty list item and validates each item as styled inline runs.
     *
     * @param list list block being checked
     * @param path model-field path used to locate any validation error
     * @param resolved effective block style inherited by item runs
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if items or their
     *     text/style/link content are invalid
     */
    private void list(ListBlock list, String path, ResolvedStyle resolved) {
        ValidationChecks.require(
                (list.items() == null) != (list.contentItems() == null),
                path,
                "exactly one of items or contentItems is required");
        if (list.contentItems() != null) {
            ValidationChecks.require(
                    !list.contentItems().isEmpty(), path + ".contentItems", "must not be empty");
            for (int index = 0; index < list.contentItems().size(); index++) {
                var item = list.contentItems().get(index);
                String itemPath = path + ".contentItems[" + index + "]";
                ValidationChecks.required(item, itemPath);
                runs(item.runs(), itemPath + ".runs", resolved);
                for (int child = 0; child < item.children().size(); child++) {
                    var nested = item.children().get(child);
                    String nestedPath = itemPath + ".children[" + child + "]";
                    ValidationChecks.required(nested, nestedPath);
                    styles.validate(nested.style(), nestedPath + ".style");
                    list(nested, nestedPath, resolved.overlay(nested.style()));
                }
            }
            return;
        }
        ValidationChecks.require(!list.items().isEmpty(), path + ".items", "must not be empty");
        for (int index = 0; index < list.items().size(); index++) {
            runs(list.items().get(index), path + ".items[" + index + "]", resolved);
        }
    }

    /**
     * Checks XML-safe text, inherited font variants and allowed link schemes for each run; empty
     * text is permitted but a run list must not be empty.
     *
     * @param runs nonempty inline content to check
     * @param path model-field path used to locate any validation error
     * @param inherited effective block or cell style, before run overrides
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if a run, its text, its
     *     resolved font variant, or its link is invalid
     */
    void runs(List<TextRun> runs, String path, ResolvedStyle inherited) {
        ValidationChecks.required(runs, path);
        ValidationChecks.require(!runs.isEmpty(), path, "must not be empty");
        for (int index = 0; index < runs.size(); index++) {
            String runPath = path + "[" + index + "]";
            TextRun run = runs.get(index);
            ValidationChecks.required(run, runPath);
            ValidationChecks.text(run.text(), runPath + ".text");
            ValidationChecks.require(
                    run.pageNumber() == null || run.text().isEmpty(),
                    runPath + ".text",
                    "page fields must have empty literal text");
            styles.validate(run.style(), runPath + ".style");
            ResolvedStyle resolved = inherited.overlay(run.style());
            fonts.requireVariant(
                    resolved.fontFamily(), resolved.variant(), runPath + ".style.fontFamily");
            LinkValidator.validate(run.href(), runPath + ".href");
        }
    }
}
