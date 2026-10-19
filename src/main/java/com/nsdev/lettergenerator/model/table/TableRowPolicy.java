package com.nsdev.lettergenerator.model.table;

import com.nsdev.lettergenerator.model.layout.Keep;

/**
 * Overrides page-breaking behavior for one zero-based body row. Indices must be unique and in
 * range.
 *
 * @param rowIndex body row index, starting at zero
 * @param keepTogether optional row split control; AUTO allows splitting
 * @param keepWithNext optional relationship to the following body row
 */
public record TableRowPolicy(int rowIndex, Keep keepTogether, Keep keepWithNext) {}
