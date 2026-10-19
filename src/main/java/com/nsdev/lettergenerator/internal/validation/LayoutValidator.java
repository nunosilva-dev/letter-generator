package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.internal.layout.Units;
import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ColumnsBlock;
import com.nsdev.lettergenerator.model.layout.BoxAppearance;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.Border;
import com.nsdev.lettergenerator.model.table.BorderStyle;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import java.util.HashSet;

/** Checks declared layout containers and recursively validates rich child content. */
final class LayoutValidator {
    /** Shared content walker with registered fonts. */
    private final BlockValidator blocks;

    /** Shared unresolved style checker. */
    private final StyleValidator styles;

    /**
     * Binds recursive checks without adding mutable per-document state.
     *
     * @param blocks content checker
     * @param styles style checker
     */
    LayoutValidator(BlockValidator blocks, StyleValidator styles) {
        this.blocks = blocks;
        this.styles = styles;
    }

    /**
     * Validates stable IDs, finite positive geometry and declared child bounds.
     *
     * @param canvas canvas model
     * @param path input location
     * @param inherited effective parent typography
     * @param width available width
     */
    void canvas(CanvasBlock canvas, String path, Style inherited, double width) {
        ValidationChecks.positive(canvas.widthMm(), path + ".widthMm");
        ValidationChecks.positive(canvas.heightMm(), path + ".heightMm");
        ValidationChecks.require(
                canvas.widthMm() <= width + PageGeometry.GEOMETRY_TOLERANCE_MM,
                path + ".widthMm",
                "canvas exceeds available width");
        ValidationChecks.required(canvas.elements(), path + ".elements");
        ValidationChecks.require(
                !canvas.elements().isEmpty(), path + ".elements", "must not be empty");
        var ids = new HashSet<String>();
        for (int index = 0; index < canvas.elements().size(); index++) {
            var element = canvas.elements().get(index);
            String item = path + ".elements[" + index + "]";
            ValidationChecks.required(element, item);
            ValidationChecks.nonblankText(element.id(), item + ".id");
            ValidationChecks.require(
                    ids.add(element.id()), item + ".id", "must be unique within the canvas");
            GeometryValidator.bounds(
                    element.boundsMm(), canvas.widthMm(), canvas.heightMm(), item + ".boundsMm");
            styles.validate(element.style(), item + ".style");
            blocks.validate(
                    element.blocks(),
                    item + ".blocks",
                    inherited,
                    element.style(),
                    element.boundsMm().width(),
                    false);
            ValidationChecks.require(
                    !element.blocks().isEmpty(), item + ".blocks", "must not be empty");
        }
    }

    /**
     * Validates positive weights and usable widths after fixed gaps.
     *
     * @param columns independent columns
     * @param path input location
     * @param inherited effective parent typography
     * @param width parent width
     */
    void columns(ColumnsBlock columns, String path, Style inherited, double width) {
        ValidationChecks.required(columns.columns(), path + ".columns");
        ValidationChecks.require(
                !columns.columns().isEmpty(), path + ".columns", "must not be empty");
        ValidationChecks.nonnegative(columns.gapMm(), path + ".gapMm");
        double total = 0;
        for (int index = 0; index < columns.columns().size(); index++) {
            var column = columns.columns().get(index);
            String item = path + ".columns[" + index + "]";
            ValidationChecks.required(column, item);
            ValidationChecks.positive(column.weight(), item + ".weight");
            total += column.weight();
        }
        ValidationChecks.positive(total, path + ".columns");
        double available = width - columns.gapMm() * (columns.columns().size() - 1);
        ValidationChecks.positive(available, path + ".gapMm");
        for (int index = 0; index < columns.columns().size(); index++) {
            var column = columns.columns().get(index);
            String item = path + ".columns[" + index + "]";
            styles.validate(column.style(), item + ".style");
            blocks.validate(
                    column.blocks(),
                    item + ".blocks",
                    inherited,
                    column.style(),
                    available * column.weight() / total,
                    false);
            ValidationChecks.require(
                    !column.blocks().isEmpty(), item + ".blocks", "must not be empty");
        }
    }

    /**
     * Validates per-edge decoration and the actual inner content width.
     *
     * @param box flowing box
     * @param path source location
     * @param inherited effective parent typography
     * @param width parent width
     * @param pageBreaks whether the surrounding body permits page breaks
     */
    void box(BoxBlock box, String path, Style inherited, double width, boolean pageBreaks) {
        double inner = width;
        BoxAppearance appearance = box.appearance();
        if (appearance != null) {
            TableValidator.appearance(
                    new CellAppearance(
                            null, appearance.padding(), null, appearance.backgroundColor()),
                    path + ".appearance");
            if (appearance.padding() != null) {
                inner -= appearance.padding().leftMm() + appearance.padding().rightMm();
            }
            if (appearance.borders() != null) {
                var borders = appearance.borders();
                border(borders.top(), path + ".appearance.borders.top");
                border(borders.right(), path + ".appearance.borders.right");
                border(borders.bottom(), path + ".appearance.borders.bottom");
                border(borders.left(), path + ".appearance.borders.left");
                inner -= width(borders.left()) + width(borders.right());
            }
        }
        ValidationChecks.positive(inner, path + ".appearance");
        blocks.validate(box.blocks(), path + ".blocks", inherited, null, inner, pageBreaks);
        ValidationChecks.require(!box.blocks().isEmpty(), path + ".blocks", "must not be empty");
    }

    /**
     * Checks one optional physical border.
     *
     * @param border edge settings
     * @param path edge location
     */
    private static void border(Border border, String path) {
        TableValidator.appearance(new CellAppearance(border, null, null, null), path);
    }

    /**
     * Converts a visible edge to its occupied physical width.
     *
     * @param border optional edge
     * @return millimeter thickness
     */
    private static double width(Border border) {
        return border == null || border.style() == BorderStyle.NONE
                ? 0
                : Units.pointsToMillimeters(border.widthPt());
    }
}
