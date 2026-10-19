package com.nsdev.lettergenerator.internal.table;

import com.nsdev.lettergenerator.internal.layout.Units;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.TableOptions;
import java.util.List;

/** Resolves shared cell appearance and its actual inner width for parsing and validation. */
public final class TableCells {
    /** Prevents instances of the cell geometry utility. */
    private TableCells() {}

    /**
     * Applies legacy defaults, table defaults, cell appearance and legacy explicit fill in order.
     *
     * @param options nullable table options
     * @param cell cell whose appearance is resolved
     * @return complete appearance, with nullable transparent background
     */
    public static CellAppearance appearance(TableOptions options, TableCell cell) {
        CellAppearance result =
                CellAppearance.DEFAULT
                        .overlay(cell.defaultAppearance())
                        .overlay(options == null ? null : options.cellAppearance())
                        .overlay(cell.appearance());
        return cell.backgroundColor() == null
                ? result
                : result.overlay(new CellAppearance(null, null, null, cell.backgroundColor()));
    }

    /**
     * Computes the occupied proportional column widths, subtracting horizontal padding and both
     * complete border edges conservatively even when adjacent collapsed borders are shared.
     *
     * @param weights positive finite column weights
     * @param column first physical column
     * @param columnSpan number of occupied columns
     * @param tableWidth full table width in millimeters
     * @param appearance complete validated cell appearance
     * @return available inner content width in millimeters, possibly nonpositive for invalid input
     */
    public static double innerWidth(
            List<Double> weights,
            int column,
            int columnSpan,
            double tableWidth,
            CellAppearance appearance) {
        double total = weights.stream().mapToDouble(Double::doubleValue).sum();
        double spanned = 0;
        for (int index = column; index < column + columnSpan; index++) {
            spanned += weights.get(index);
        }
        double border =
                appearance.border().style()
                                == com.nsdev.lettergenerator.model.table.BorderStyle.NONE
                        ? 0
                        : Units.pointsToMillimeters(appearance.border().widthPt());
        return tableWidth * (spanned / total)
                - appearance.padding().leftMm()
                - appearance.padding().rightMm()
                - border * 2;
    }
}
