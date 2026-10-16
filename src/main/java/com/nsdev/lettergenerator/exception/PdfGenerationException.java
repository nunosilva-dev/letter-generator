package com.nsdev.lettergenerator.exception;

public final class PdfGenerationException extends LetterPdfException {
    private static final long serialVersionUID = 1L;

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
