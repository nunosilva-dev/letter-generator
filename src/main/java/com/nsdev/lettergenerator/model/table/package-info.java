/**
 * Immutable cell content, appearance and body-row page policies for native tables. A cell contains
 * either compact inline runs or blocks; explicit empty block lists create visual empty cells.
 * Positive column/row spans must completely cover independent header/body grids without overlap.
 * Rich cell blocks support paragraphs, headings, lists and images, but no nested tables or page
 * breaks. Table defaults inherit independently into local cell appearance; legacy explicit cell
 * backgroundColor overrides both. Border widths use points and padding uses millimeters. Omitted
 * options preserve white quarter-point separators, one-millimeter padding, vertically centered
 * content and soft row keeps. Row groups refer to disjoint zero-based body-row ranges.
 */
package com.nsdev.lettergenerator.model.table;
