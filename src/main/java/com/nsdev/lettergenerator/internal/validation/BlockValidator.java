package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.ListBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.TableCell;
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
            ResolvedStyle resolved =
                    ResolvedStyle.defaults(block instanceof Heading)
                            .overlay(defaults)
                            .overlay(section)
                            .overlay(block.style());
            fonts.requireVariant(
                    resolved.fontFamily(), resolved.variant(), blockPath + ".style.fontFamily");
            if (block instanceof Paragraph paragraph) {
                runs(paragraph.runs(), blockPath + ".runs", resolved);
            } else if (block instanceof Heading heading) {
                runs(heading.runs(), blockPath + ".runs", resolved);
            } else if (block instanceof ListBlock list) {
                list(list, blockPath, resolved);
            } else if (block instanceof TableBlock table) {
                table(table, blockPath, resolved);
            } else if (block instanceof ImageBlock image) {
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
     * Requires at least one nonempty list item and validates each item as styled inline runs.
     *
     * @param list list block being checked
     * @param path model-field path used to locate any validation error
     * @param resolved effective block style inherited by item runs
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if items or their
     *     text/style/link content are invalid
     */
    private void list(ListBlock list, String path, ResolvedStyle resolved) {
        ValidationChecks.required(list.items(), path + ".items");
        ValidationChecks.require(!list.items().isEmpty(), path + ".items", "must not be empty");
        for (int index = 0; index < list.items().size(); index++) {
            runs(list.items().get(index), path + ".items[" + index + "]", resolved);
        }
    }

    /**
     * Requires finite positive column weights, a nonempty body and rectangular header/body rows
     * matching the column count.
     *
     * @param table table block being checked
     * @param path model-field path used to locate any validation error
     * @param resolved effective table style inherited by cell text
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if weights, row shape,
     *     cells, or cell content are invalid
     */
    private void table(TableBlock table, String path, ResolvedStyle resolved) {
        ValidationChecks.required(table.columnWeights(), path + ".columnWeights");
        ValidationChecks.require(
                !table.columnWeights().isEmpty(), path + ".columnWeights", "must not be empty");
        double sum = 0;
        for (int index = 0; index < table.columnWeights().size(); index++) {
            Double weight = table.columnWeights().get(index);
            ValidationChecks.positive(weight, path + ".columnWeights[" + index + "]");
            sum += weight;
        }
        ValidationChecks.require(
                Double.isFinite(sum), path + ".columnWeights", "sum must be finite");
        int columns = table.columnWeights().size();
        if (!table.header().isEmpty()) {
            cells(table.header(), columns, path + ".header", resolved);
        }
        ValidationChecks.required(table.rows(), path + ".rows");
        ValidationChecks.require(!table.rows().isEmpty(), path + ".rows", "must not be empty");
        for (int index = 0; index < table.rows().size(); index++) {
            cells(table.rows().get(index), columns, path + ".rows[" + index + "]", resolved);
        }
    }

    /**
     * Checks one rectangular row and overlays cell style before validating its text runs and
     * optional background color.
     *
     * @param cells header or body-row cells, required to match the column count
     * @param columns expected number of cells
     * @param path model-field path used to locate any validation error
     * @param resolved effective table style, before cell and run overrides
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if row shape, a cell, its
     *     fill, or its text/style/link content is invalid
     */
    private void cells(List<TableCell> cells, int columns, String path, ResolvedStyle resolved) {
        ValidationChecks.required(cells, path);
        ValidationChecks.require(cells.size() == columns, path, "must match the column count");
        for (int index = 0; index < cells.size(); index++) {
            String cellPath = path + "[" + index + "]";
            TableCell cell = cells.get(index);
            ValidationChecks.required(cell, cellPath);
            styles.validate(cell.style(), cellPath + ".style");
            if (cell.backgroundColor() != null) {
                StyleValidator.color(cell.backgroundColor(), cellPath + ".backgroundColor");
            }
            runs(cell.runs(), cellPath + ".runs", resolved.overlay(cell.style()));
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
    private void runs(List<TextRun> runs, String path, ResolvedStyle inherited) {
        ValidationChecks.required(runs, path);
        ValidationChecks.require(!runs.isEmpty(), path, "must not be empty");
        for (int index = 0; index < runs.size(); index++) {
            String runPath = path + "[" + index + "]";
            TextRun run = runs.get(index);
            ValidationChecks.required(run, runPath);
            ValidationChecks.text(run.text(), runPath + ".text");
            styles.validate(run.style(), runPath + ".style");
            ResolvedStyle resolved = inherited.overlay(run.style());
            fonts.requireVariant(
                    resolved.fontFamily(), resolved.variant(), runPath + ".style.fontFamily");
            LinkValidator.validate(run.href(), runPath + ".href");
        }
    }
}
