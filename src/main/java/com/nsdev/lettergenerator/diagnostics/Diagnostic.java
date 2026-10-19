package com.nsdev.lettergenerator.diagnostics;

import java.io.Serializable;

/**
 * Stable diagnostic code with a human-readable message and input location.
 *
 * <p>Instances are serializable for exception transport. Serialized values correspond to the record
 * components documented below.
 *
 * @param code machine-readable classification
 * @param severity diagnostic severity
 * @param phase processing stage
 * @param path input field or template selector
 * @param message readable explanation
 * @serial exclude
 */
public record Diagnostic(
        String code,
        DiagnosticSeverity severity,
        DiagnosticPhase phase,
        String path,
        String message)
        implements Serializable {
    /** Serialization version of this diagnostic value. */
    private static final long serialVersionUID = 1L;
}
