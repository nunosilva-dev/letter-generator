package com.nsdev.lettergenerator.exception;

public final class InvalidLetterException extends LetterPdfException {
    private static final long serialVersionUID = 1L;

    private final String fieldPath;

    public InvalidLetterException(String fieldPath, String message) {
        this(fieldPath, message, null);
    }

    public InvalidLetterException(String fieldPath, String message, Throwable cause) {
        super(fieldPath + ": " + message, cause);
        this.fieldPath = fieldPath;
    }

    public String fieldPath() {
        return fieldPath;
    }
}
