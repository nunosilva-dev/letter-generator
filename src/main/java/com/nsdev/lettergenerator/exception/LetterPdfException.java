package com.nsdev.lettergenerator.exception;

/**
 * Unchecked base for caller-facing letter validation and PDF rendering failures. Catch {@link
 * InvalidLetterException} to inspect an invalid input's field path, or {@link
 * PdfGenerationException} for composition, layout and post-processing failures. Convenience builder
 * argument errors may instead use {@link IllegalArgumentException} or {@link IllegalStateException}
 * before generation starts.
 */
public class LetterPdfException extends RuntimeException {
    /** Serialization version for this caller-facing exception type. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a library failure with a descriptive message and no cause.
     *
     * @param message diagnostic message, or null if unavailable
     */
    public LetterPdfException(String message) {
        super(message);
    }

    /**
     * Creates a library failure retaining the underlying failure for diagnosis.
     *
     * @param message diagnostic message, or null if unavailable
     * @param cause underlying failure, or null if none
     */
    public LetterPdfException(String message, Throwable cause) {
        super(message, cause);
    }
}
