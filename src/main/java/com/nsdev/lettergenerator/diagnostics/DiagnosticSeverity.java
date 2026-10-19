package com.nsdev.lettergenerator.diagnostics;

/** Severity of a structured authoring or rendering diagnostic. */
public enum DiagnosticSeverity {
    /** Invalid input or rejected layout. */
    ERROR,
    /** Accepted output with a condition the caller should review. */
    WARNING,
    /** Information about accepted processing. */
    INFO
}
