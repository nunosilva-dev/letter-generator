package com.nsdev.lettergenerator.exception;

/**
 * Indicates failure while composing, laying out, rendering or post-processing a PDF. The generator
 * returns no PDF byte array when this exception is thrown. Its cause can provide engine or
 * watermark-processing details; invalid input detected by validation instead uses {@link
 * InvalidLetterException}.
 */
public final class PdfGenerationException extends LetterPdfException {
    /** Serialization version for this caller-facing exception type. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a rendering failure while retaining its underlying cause.
     *
     * @param message diagnostic describing the failed generation stage, or null
     * @param cause underlying engine or post-processing failure, or null
     */
    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
