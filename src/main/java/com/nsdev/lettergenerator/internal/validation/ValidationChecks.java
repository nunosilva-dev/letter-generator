package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.xml.XmlCharacters;

/** Shared validation primitives; every error retains the caller's document field path. */
final class ValidationChecks {
    private ValidationChecks() {}

    static void require(boolean condition, String path, String message) {
        if (!condition) {
            throw new InvalidLetterException(path, message);
        }
    }

    static void required(Object value, String path) {
        require(value != null, path, "is required");
    }

    static void finite(double value, String path) {
        require(Double.isFinite(value), path, "must be finite");
    }

    static void positive(Double value, String path) {
        required(value, path);
        finite(value, path);
        require(value > 0, path, "must be positive");
    }

    static void nonnegative(double value, String path) {
        finite(value, path);
        require(value >= 0, path, "must be non-negative");
    }

    static void text(String value, String path) {
        required(value, path);
        require(value.codePoints().allMatch(XmlCharacters::isValid), path, "invalid XML character");
    }

    static void nonblankText(String value, String path) {
        text(value, path);
        require(!value.isBlank(), path, "must not be blank");
    }
}
