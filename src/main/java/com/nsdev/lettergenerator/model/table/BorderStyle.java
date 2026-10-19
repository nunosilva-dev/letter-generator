package com.nsdev.lettergenerator.model.table;

/** Finite vocabulary of native table-cell border line styles. */
public enum BorderStyle {
    /** Paint no border, irrespective of the configured width and color. */
    NONE,
    /** Paint a continuous line. */
    SOLID,
    /** Paint a dashed line. */
    DASHED,
    /** Paint a dotted line. */
    DOTTED
}
