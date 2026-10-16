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

final class BlockValidator {
    private final FontRegistry fonts;
    private final StyleValidator styles;

    BlockValidator(FontRegistry fonts, StyleValidator styles) {
        this.fonts = fonts;
        this.styles = styles;
    }

    void validate(List<Block> blocks, String path, Style defaults, Style section, double width) {
        validate(blocks, path, defaults, section, width, false);
    }

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

    private void list(ListBlock list, String path, ResolvedStyle resolved) {
        ValidationChecks.required(list.items(), path + ".items");
        ValidationChecks.require(!list.items().isEmpty(), path + ".items", "must not be empty");
        for (int index = 0; index < list.items().size(); index++) {
            runs(list.items().get(index), path + ".items[" + index + "]", resolved);
        }
    }

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
