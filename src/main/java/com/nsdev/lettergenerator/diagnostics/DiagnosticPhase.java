package com.nsdev.lettergenerator.diagnostics;

/** Processing stage that produced a diagnostic. */
public enum DiagnosticPhase {
    /** Parsing, binding, asset decoding or declared model validation; no PDF layout. */
    INPUT,
    /** Pagination, painting or accepted layout recovery. */
    LAYOUT
}
