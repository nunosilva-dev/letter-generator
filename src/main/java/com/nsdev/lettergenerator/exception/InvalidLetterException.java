package com.nsdev.lettergenerator.exception;

/**
 * Indicates invalid JSON, markup, model data or registered font configuration. {@link #fieldPath()}
 * identifies the failing input location; the exception message starts with that path followed by
 * the diagnostic. Paths can refer to document fields, template nodes or font registrations rather
 * than only JSON properties.
 */
public final class InvalidLetterException extends LetterPdfException {
    /** Serialization version for this caller-facing exception type. */
    private static final long serialVersionUID = 1L;

    /** Input location retained separately from the prefixed exception message. */
    private final String fieldPath;

    /**
     * Creates a validation failure without an underlying cause. The message is prefixed with the
     * supplied path; neither argument is validated here.
     *
     * @param fieldPath input location to retain and prefix in the message
     * @param message diagnostic explaining why that location is invalid
     */
    public InvalidLetterException(String fieldPath, String message) {
        this(fieldPath, message, null);
    }

    /**
     * Creates a validation failure retaining its input location and underlying cause. The message
     * is prefixed with the supplied path; neither path nor message is validated here.
     *
     * @param fieldPath input location to retain and prefix in the message
     * @param message diagnostic explaining why that location is invalid
     * @param cause underlying parsing, decoding or validation failure, or null
     */
    public InvalidLetterException(String fieldPath, String message, Throwable cause) {
        super(fieldPath + ": " + message, cause);
        this.fieldPath = fieldPath;
    }

    /**
     * Returns the input location supplied when this exception was constructed. Library-generated
     * failures use paths such as {@code document}, indexed model fields, {@code template...}, or
     * {@code fonts[family]}.
     *
     * @return the retained field path; it can be null if supplied by a caller
     */
    public String fieldPath() {
        return fieldPath;
    }
}
