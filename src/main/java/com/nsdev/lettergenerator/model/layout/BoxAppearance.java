package com.nsdev.lettergenerator.model.layout;

import com.nsdev.lettergenerator.model.table.Padding;

/**
 * Optional flow-box decoration, independent of typography and pagination.
 *
 * @param padding nullable nonnegative millimeter padding; null means zero
 * @param borders nullable independent physical edges; null means no borders
 * @param backgroundColor nullable six-digit RGB color; null means transparent
 */
public record BoxAppearance(Padding padding, Borders borders, String backgroundColor) {}
