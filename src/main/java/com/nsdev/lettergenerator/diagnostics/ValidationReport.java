package com.nsdev.lettergenerator.diagnostics;

import java.util.List;

/**
 * Immutable input inspection result. Success does not guarantee that content fits during layout.
 *
 * @param diagnostics ordered immutable diagnostics
 */
public record ValidationReport(List<Diagnostic> diagnostics) {
    /**
     * Snapshots the diagnostic list.
     *
     * @param diagnostics non-null diagnostics
     */
    public ValidationReport {
        diagnostics = List.copyOf(diagnostics);
    }

    /**
     * @return true when there are no error diagnostics
     */
    public boolean isValid() {
        return diagnostics.stream().noneMatch(item -> item.severity() == DiagnosticSeverity.ERROR);
    }
}
