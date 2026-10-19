package com.nsdev.lettergenerator.internal.table;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.table.TableCell;
import java.util.ArrayList;
import java.util.List;

/** Validates and locates cells using a linear-space occupancy grid shared by all input paths. */
public final class TableGrid {
    /** Prevents instances of the grid utility. */
    private TableGrid() {}

    /**
     * Cell occupying a rectangular grid area, beginning at a physical column.
     *
     * @param cell immutable model cell
     * @param columnIndex zero-based physical starting column
     * @param sourceIndex zero-based cell position in its source row
     */
    public record PlacedCell(TableCell cell, int columnIndex, int sourceIndex) {}

    /**
     * Locates all cells and checks complete row coverage, positive spans, overlap and bounds. A row
     * with no starting cells is valid only when earlier row spans cover every column.
     *
     * @param rows section rows; header and body must be passed separately
     * @param columns positive physical column count
     * @param path section path, such as table.rows or table.header
     * @param header whether this section is the single header row, omitting a row path index
     * @return immutable rows of located starting cells
     * @throws InvalidLetterException if the section cannot cover the grid exactly
     */
    public static List<List<PlacedCell>> layout(
            List<List<TableCell>> rows, int columns, String path, boolean header) {
        require(rows != null, path, "is required");
        require(columns > 0, path, "must have columns");
        int[] occupiedUntil = new int[columns];
        var result = new ArrayList<List<PlacedCell>>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            String rowPath = header ? path : path + "[" + rowIndex + "]";
            List<TableCell> row = rows.get(rowIndex);
            require(row != null, rowPath, "is required");
            var placed = new ArrayList<PlacedCell>();
            int column = 0;
            for (int index = 0; index < row.size(); index++) {
                String cellPath = rowPath + "[" + index + "]";
                TableCell cell = row.get(index);
                require(cell != null, cellPath, "is required");
                int columnSpan = cell.columnSpan() == null ? 1 : cell.columnSpan();
                int rowSpan = cell.rowSpan() == null ? 1 : cell.rowSpan();
                require(columnSpan > 0, cellPath + ".columnSpan", "must be positive");
                require(rowSpan > 0, cellPath + ".rowSpan", "must be positive");
                while (column < columns && occupiedUntil[column] > rowIndex) {
                    column++;
                }
                require(column < columns, rowPath, "must match the column count");
                require(
                        columnSpan <= columns - column,
                        cellPath + ".columnSpan",
                        "exceeds column count");
                require(
                        rowSpan <= rows.size() - rowIndex,
                        cellPath + ".rowSpan",
                        "extends beyond its section");
                for (int offset = 0; offset < columnSpan; offset++) {
                    require(
                            occupiedUntil[column + offset] <= rowIndex,
                            cellPath + ".columnSpan",
                            "overlaps another row span");
                }
                placed.add(new PlacedCell(cell, column, index));
                for (int offset = 0; offset < columnSpan; offset++) {
                    occupiedUntil[column + offset] = rowIndex + rowSpan;
                }
                column += columnSpan;
            }
            for (int index = 0; index < columns; index++) {
                require(occupiedUntil[index] > rowIndex, rowPath, "must match the column count");
            }
            result.add(List.copyOf(placed));
        }
        return List.copyOf(result);
    }

    /**
     * Throws an input diagnostic without coupling the grid to the validation service package.
     *
     * @param condition required grid invariant
     * @param path model path identifying its source
     * @param message short explanation on failure
     */
    private static void require(boolean condition, String path, String message) {
        if (!condition) {
            throw new InvalidLetterException(path, message);
        }
    }
}
