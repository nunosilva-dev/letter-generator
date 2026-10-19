package com.nsdev.lettergenerator.internal.composition;

import com.nsdev.lettergenerator.internal.table.TableCells;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.TableOptions;
import com.nsdev.lettergenerator.theme.LetterTheme;

/** Resolves theme appearance beneath explicit table/cell controls without altering row metadata. */
public final class ThemeTables {
    /** Prevents instances. */
    private ThemeTables() {}

    /**
     * Layers shared cell appearance and row defaults below table-local values.
     *
     * @param theme optional theme
     * @param local optional table options
     * @return resolved options or the legacy local value
     */
    static TableOptions options(LetterTheme theme, TableOptions local) {
        if (theme == null || theme.tableAppearance() == null) {
            return local;
        }
        var defaults = theme.tableAppearance();
        return new TableOptions(
                local == null ? null : local.cellAppearance(),
                local != null && local.rowKeepTogether() != null
                        ? local.rowKeepTogether()
                        : defaults.rowKeepTogether(),
                local == null ? null : local.rowPolicies(),
                local == null ? null : local.rowGroups());
    }

    /**
     * Resolves the same complete cell appearance used after document preparation, allowing markup
     * image percentages to use final themed padding and borders before creating ordinary blocks.
     * Explicit table and cell declarations remain above inherited theme defaults.
     *
     * @param theme optional document theme
     * @param header whether this cell belongs to the repeating header
     * @param local optional explicit table options
     * @param source immutable source cell before theme preparation
     * @return complete appearance for intrinsic cell geometry
     */
    public static CellAppearance appearance(
            LetterTheme theme, boolean header, TableOptions local, TableCell source) {
        var cell =
                new TableCell(
                        source.runs(),
                        source.style(),
                        source.backgroundColor(),
                        source.blocks(),
                        source.columnSpan(),
                        source.rowSpan(),
                        source.appearance(),
                        source.semanticStyle(),
                        cell(theme, header, source.defaultAppearance()));
        return TableCells.appearance(options(theme, local), cell);
    }

    /**
     * Retains builtin header fills beneath shared theme defaults.
     *
     * @param theme optional theme
     * @param header whether the cell belongs to the repeating header
     * @param fallback optional builtin appearance
     * @return appearance below explicit table and cell values
     */
    static CellAppearance cell(LetterTheme theme, boolean header, CellAppearance fallback) {
        if (theme == null || theme.tableAppearance() == null) {
            return fallback;
        }
        var defaults = theme.tableAppearance();
        CellAppearance result =
                fallback == null ? new CellAppearance(null, null, null, null) : fallback;
        result = result.overlay(defaults.cells());
        return header ? result.overlay(defaults.header()) : result;
    }
}
