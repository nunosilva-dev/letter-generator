package com.nsdev.lettergenerator.model.table;

import com.nsdev.lettergenerator.model.layout.Keep;

/**
 * Consecutive body rows requested to stay on one page. Groups cannot overlap and cannot include
 * header rows. A soft preference may break when necessary; an impossible strong keep can fail.
 *
 * @param startRow first zero-based body row index
 * @param rowCount positive number of rows in the group
 * @param keepTogether required keep strength, including AUTO to allow normal breaks
 */
public record TableRowGroup(int startRow, int rowCount, Keep keepTogether) {}
