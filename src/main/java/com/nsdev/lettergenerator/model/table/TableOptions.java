package com.nsdev.lettergenerator.model.table;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.layout.Keep;
import java.util.List;

/**
 * Optional table appearance and body-row page policies. Absent options retain one-millimeter
 * padding, white quarter-point borders, centered content and a soft keep on every row.
 *
 * @param cellAppearance optional defaults inherited independently by every cell
 * @param rowKeepTogether optional default row split control, null means PREFER
 * @param rowPolicies optional body-row overrides, null becomes empty
 * @param rowGroups optional disjoint body-row groups, null becomes empty
 */
public record TableOptions(
        CellAppearance cellAppearance,
        Keep rowKeepTogether,
        List<TableRowPolicy> rowPolicies,
        List<TableRowGroup> rowGroups) {
    /**
     * Snapshots row metadata without validating indices or appearance values.
     *
     * @param cellAppearance nullable cell appearance defaults
     * @param rowKeepTogether nullable default row keep
     * @param rowPolicies nullable body-row overrides
     * @param rowGroups nullable disjoint body-row groups
     */
    public TableOptions {
        rowPolicies = rowPolicies == null ? List.of() : ImmutableLists.copyNullable(rowPolicies);
        rowGroups = rowGroups == null ? List.of() : ImmutableLists.copyNullable(rowGroups);
    }
}
