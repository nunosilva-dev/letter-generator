package com.nsdev.lettergenerator.exception;

import com.nsdev.lettergenerator.diagnostics.Diagnostic;
import java.util.List;

/**
 * Indicates failure while composing, laying out, rendering or post-processing a PDF. The generator
 * returns no PDF byte array when this exception is thrown. Its cause can provide engine or
 * watermark-processing details; invalid input detected by validation instead uses {@link
 * InvalidLetterException}.
 */
public final class PdfGenerationException extends LetterPdfException {
    /** Serialization version for this caller-facing exception type. */
    private static final long serialVersionUID = 1L;

    /** Structured layout errors, empty for failures outside classified layout checks. */
    private final Diagnostic[] diagnostics;

    /**
     * Creates a rendering failure while retaining its underlying cause.
     *
     * @param message diagnostic describing the failed generation stage, or null
     * @param cause underlying engine or post-processing failure, or null
     */
    public PdfGenerationException(String message, Throwable cause) {
        this(message, cause, List.of());
    }

    /**
     * Creates a rejected layout with classified errors and its underlying cause.
     *
     * @param message readable failure explanation
     * @param cause underlying engine failure
     * @param diagnostics structured errors, snapshotted
     */
    public PdfGenerationException(String message, Throwable cause, List<Diagnostic> diagnostics) {
        super(message, cause);
        this.diagnostics = List.copyOf(diagnostics).toArray(Diagnostic[]::new);
    }

    /**
     * @return immutable layout errors, empty for unclassified processing failures
     */
    public List<Diagnostic> diagnostics() {
        return List.of(diagnostics);
    }
}
