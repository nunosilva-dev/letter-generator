package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.internal.layout.Units;
import com.nsdev.lettergenerator.internal.table.TableCells;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ColumnsBlock;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.table.Border;
import com.nsdev.lettergenerator.model.table.BorderStyle;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.TableOptions;
import java.util.List;

/** Rejects indivisible canvases whose declared height cannot fit their physical parent zone. */
final class CanvasHeights {
    /** Prevents instances of the geometry walker. */
    private CanvasHeights() {}

    /**
     * Checks known height limits after structural content validation has succeeded.
     *
     * @param document validated immutable content
     * @param geometry ordinary and final body reservations
     */
    static void validate(LetterDocument document, PageGeometry geometry) {
        for (int index = 0; index < document.sections().size(); index++) {
            blocks(
                    document.sections().get(index).blocks(),
                    geometry.bodyHeightMm(),
                    "sections[" + index + "].blocks");
        }
        for (int index = 0; index < document.fixedElements().size(); index++) {
            var element = document.fixedElements().get(index);
            blocks(
                    element.blocks(),
                    element.boundsMm().height(),
                    "fixedElements[" + index + "].blocks");
        }
        if (document.signature() != null) {
            var signature = document.signature();
            blocks(
                    signature.blocks(),
                    signature.placement() == SignaturePlacement.BOTTOM_LAST
                            ? signature.boundsMm().height()
                            : geometry.bodyHeightMm(),
                    "signature.blocks");
        }
    }

    /**
     * Traverses rich containers without claiming to predict text pagination.
     *
     * @param content validated ordered content
     * @param height maximum indivisible canvas height
     * @param path source model path
     */
    private static void blocks(List<Block> content, double height, String path) {
        for (int index = 0; index < content.size(); index++) {
            var block = content.get(index);
            String item = path + "[" + index + "]";
            if (block instanceof CanvasBlock canvas) {
                ValidationChecks.require(
                        canvas.heightMm() <= height + PageGeometry.GEOMETRY_TOLERANCE_MM,
                        item + ".heightMm",
                        "canvas cannot fit its containing page or box");
                for (int child = 0; child < canvas.elements().size(); child++) {
                    var element = canvas.elements().get(child);
                    blocks(
                            element.blocks(),
                            element.boundsMm().height(),
                            item + ".elements[" + child + "].blocks");
                }
            } else if (block instanceof GroupBlock group) {
                blocks(group.blocks(), height, item + ".blocks");
            } else if (block instanceof BoxBlock box) {
                double inner = height;
                if (box.appearance() != null && box.appearance().padding() != null) {
                    inner -=
                            box.appearance().padding().topMm()
                                    + box.appearance().padding().bottomMm();
                }
                if (box.appearance() != null && box.appearance().borders() != null) {
                    inner -=
                            borderHeight(box.appearance().borders().top())
                                    + borderHeight(box.appearance().borders().bottom());
                }
                blocks(box.blocks(), inner, item + ".blocks");
            } else if (block instanceof ColumnsBlock columns) {
                for (int child = 0; child < columns.columns().size(); child++) {
                    blocks(
                            columns.columns().get(child).blocks(),
                            height,
                            item + ".columns[" + child + "].blocks");
                }
            } else if (block instanceof TableBlock table) {
                for (var cell : table.header()) {
                    cell(cell, table.options(), height, item + ".header.blocks");
                }
                for (var row : table.rows()) {
                    for (var cell : row) {
                        cell(cell, table.options(), height, item + ".rows.blocks");
                    }
                }
            }
        }
    }

    /**
     * Checks a rich cell against the maximum space left by its own padding and border.
     *
     * @param cell validated cell
     * @param options resolved table defaults
     * @param height containing height limit
     * @param path cell content path
     */
    private static void cell(TableCell cell, TableOptions options, double height, String path) {
        if (cell.blocks() == null) {
            return;
        }
        var appearance = TableCells.appearance(options, cell);
        double inner =
                height
                        - appearance.padding().topMm()
                        - appearance.padding().bottomMm()
                        - borderHeight(appearance.border()) * 2;
        blocks(cell.blocks(), inner, path);
    }

    /**
     * Converts a visible physical border into its occupied millimeter height.
     *
     * @param border optional validated border
     * @return physical thickness, zero for absent/invisible edges
     */
    private static double borderHeight(Border border) {
        return border == null || border.style() == BorderStyle.NONE
                ? 0
                : Units.pointsToMillimeters(border.widthPt());
    }
}
