package com.nsdev.lettergenerator.model.layout;

/** Page-breaking strength for a block, successor relationship or table row. */
public enum Keep {
    /** Allow normal page breaks, including disabling an ordinary heading keep. */
    AUTO,
    /** Prefer keeping content together, but allow a break when necessary. */
    PREFER,
    /** Strongly request a keep; impossible layouts remain subject to engine validation. */
    ALWAYS
}
