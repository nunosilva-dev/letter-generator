package com.nsdev.lettergenerator.exception;

public class LetterPdfException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LetterPdfException(String message) {
        super(message);
    }

    public LetterPdfException(String message, Throwable cause) {
        super(message, cause);
    }
}
