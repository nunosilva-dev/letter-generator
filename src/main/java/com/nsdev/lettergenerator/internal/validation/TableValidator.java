package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.internal.table.TableCells;
import com.nsdev.lettergenerator.internal.table.TableGrid;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ColumnsBlock;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.TableOptions;
import java.util.HashSet;
import java.util.List;

/** Validates table grids, appearance and cell content using the same widths as native layout. */
final class TableValidator {
    /** Zero inherited paragraph gaps retain legacy compact inline cells. */
    private static final Style NO_CELL_SPACING =
            new Style(null, null, null, null, null, null, null, null, 0d, 0d);

    /** Shared style override validation. */
    private final StyleValidator styles;

    /** Shared block/run validation, including registered font faces and image widths. */
    private final BlockValidator blocks;

    /**
     * Binds table validation to its owning block and style validators.
     *
     * @param styles shared nullable-style validator
     * @param blocks shared recursive block validator
     */
    TableValidator(StyleValidator styles, BlockValidator blocks) {
        this.styles = styles;
        this.blocks = blocks;
    }

    /**
     * Validates column weights, independent header/body grids and local cell appearance/content.
     *
     * @param table table being validated
     * @param path model field path for diagnostics
     * @param inherited resolved table typography
     * @param width available full table width in millimeters
     */
    void validate(TableBlock table, String path, ResolvedStyle inherited, double width) {
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
        ValidationChecks.required(table.rows(), path + ".rows");
        ValidationChecks.require(!table.rows().isEmpty(), path + ".rows", "must not be empty");
        int columns = table.columnWeights().size();
        options(table.options(), table.rows().size(), path + ".options");
        if (!table.header().isEmpty()) {
            var header = TableGrid.layout(List.of(table.header()), columns, path + ".header", true);
            row(header.get(0), table, path + ".header", inherited, width);
        }
        var rows = TableGrid.layout(table.rows(), columns, path + ".rows", false);
        for (int index = 0; index < rows.size(); index++) {
            row(rows.get(index), table, path + ".rows[" + index + "]", inherited, width);
        }
    }

    /**
     * Validates cells with resolved spans and actual padding/border deductions.
     *
     * @param cells located row-starting cells
     * @param table table containing the cells
     * @param path row diagnostic path
     * @param inherited table typography
     * @param width full table width in millimeters
     */
    private void row(
            List<TableGrid.PlacedCell> cells,
            TableBlock table,
            String path,
            ResolvedStyle inherited,
            double width) {
        for (var placed : cells) {
            TableCell cell = placed.cell();
            String cellPath = path + "[" + placed.sourceIndex() + "]";
            styles.validate(cell.style(), cellPath + ".style");
            styles.validate(cell.semanticStyle(), cellPath + ".semanticStyle");
            if (cell.backgroundColor() != null) {
                StyleValidator.color(cell.backgroundColor(), cellPath + ".backgroundColor");
            }
            appearance(cell.defaultAppearance(), cellPath + ".defaultAppearance");
            appearance(cell.appearance(), cellPath + ".appearance");
            CellAppearance appearance = TableCells.appearance(table.options(), cell);
            double innerWidth =
                    TableCells.innerWidth(
                            table.columnWeights(),
                            placed.columnIndex(),
                            cell.columnSpan() == null ? 1 : cell.columnSpan(),
                            width,
                            appearance);
            ValidationChecks.require(
                    innerWidth > 0,
                    cellPath + ".appearance",
                    "padding and borders leave no available cell width");
            ValidationChecks.require(
                    (cell.runs() == null) != (cell.blocks() == null),
                    cellPath,
                    "exactly one of runs or blocks is required");
            ResolvedStyle resolved =
                    inherited
                            .overlay(cell.semanticStyle())
                            .overlay(cell.style())
                            .overlay(NO_CELL_SPACING);
            if (cell.blocks() == null) {
                blocks.runs(cell.runs(), cellPath + ".runs", resolved);
            } else {
                restricted(cell.blocks(), cellPath + ".blocks");
                blocks.validate(
                        cell.blocks(),
                        cellPath + ".blocks",
                        resolved.toStyle(),
                        null,
                        innerWidth,
                        false);
            }
        }
    }

    /**
     * Rejects nested tables and explicit page breaks, including those hidden inside groups.
     *
     * @param content rich cell content
     * @param path block-list diagnostic path
     */
    private static void restricted(List<Block> content, String path) {
        ValidationChecks.required(content, path);
        for (int index = 0; index < content.size(); index++) {
            Block block = content.get(index);
            String blockPath = path + "[" + index + "]";
            ValidationChecks.required(block, blockPath);
            ValidationChecks.require(
                    !(block instanceof TableBlock),
                    blockPath,
                    "nested tables are not supported in cells");
            ValidationChecks.require(
                    !(block instanceof PageBreakBlock),
                    blockPath,
                    "page breaks are not supported in cells");
            if (block instanceof CanvasBlock canvas) {
                ValidationChecks.required(canvas.elements(), blockPath + ".elements");
                for (var element : canvas.elements()) {
                    ValidationChecks.required(element, blockPath + ".elements");
                    restricted(element.blocks(), blockPath + ".elements.blocks");
                }
            }
            if (block instanceof ColumnsBlock columns) {
                ValidationChecks.required(columns.columns(), blockPath + ".columns");
                for (var column : columns.columns()) {
                    ValidationChecks.required(column, blockPath + ".columns");
                    restricted(column.blocks(), blockPath + ".columns.blocks");
                }
            }
            if (block instanceof BoxBlock box) {
                restricted(box.blocks(), blockPath + ".blocks");
            }
            if (block instanceof GroupBlock group) {
                restricted(group.blocks(), blockPath + ".blocks");
            }
        }
    }

    /**
     * Checks explicitly supplied appearance values while leaving nullable inheritance untouched.
     *
     * @param value nullable appearance overrides
     * @param path appearance field path
     */
    static void appearance(CellAppearance value, String path) {
        if (value == null) {
            return;
        }
        if (value.backgroundColor() != null) {
            StyleValidator.color(value.backgroundColor(), path + ".backgroundColor");
        }
        if (value.border() != null) {
            ValidationChecks.nonnegative(value.border().widthPt(), path + ".border.widthPt");
            ValidationChecks.required(value.border().style(), path + ".border.style");
            StyleValidator.color(value.border().color(), path + ".border.color");
        }
        if (value.padding() != null) {
            ValidationChecks.nonnegative(value.padding().topMm(), path + ".padding.topMm");
            ValidationChecks.nonnegative(value.padding().rightMm(), path + ".padding.rightMm");
            ValidationChecks.nonnegative(value.padding().bottomMm(), path + ".padding.bottomMm");
            ValidationChecks.nonnegative(value.padding().leftMm(), path + ".padding.leftMm");
        }
    }

    /**
     * Validates body-row indices and disjoint contiguous row groups.
     *
     * @param options nullable table options
     * @param rows body-row count
     * @param path options diagnostic path
     */
    private static void options(TableOptions options, int rows, String path) {
        if (options == null) {
            return;
        }
        appearance(options.cellAppearance(), path + ".cellAppearance");
        var policies = new HashSet<Integer>();
        for (int index = 0; index < options.rowPolicies().size(); index++) {
            var policy = options.rowPolicies().get(index);
            String policyPath = path + ".rowPolicies[" + index + "]";
            ValidationChecks.required(policy, policyPath);
            ValidationChecks.require(
                    policy.rowIndex() >= 0 && policy.rowIndex() < rows,
                    policyPath + ".rowIndex",
                    "must identify a body row");
            ValidationChecks.require(
                    policies.add(policy.rowIndex()),
                    policyPath + ".rowIndex",
                    "duplicate row policy");
        }
        var grouped = new HashSet<Integer>();
        for (int index = 0; index < options.rowGroups().size(); index++) {
            var group = options.rowGroups().get(index);
            String groupPath = path + ".rowGroups[" + index + "]";
            ValidationChecks.required(group, groupPath);
            ValidationChecks.require(
                    group.startRow() >= 0 && group.startRow() < rows,
                    groupPath + ".startRow",
                    "must identify a body row");
            ValidationChecks.require(
                    group.rowCount() > 0 && group.rowCount() <= rows - group.startRow(),
                    groupPath + ".rowCount",
                    "must fit within body rows");
            ValidationChecks.required(group.keepTogether(), groupPath + ".keepTogether");
            for (int row = group.startRow(); row < group.startRow() + group.rowCount(); row++) {
                ValidationChecks.require(grouped.add(row), groupPath, "row groups cannot overlap");
            }
        }
    }
}
