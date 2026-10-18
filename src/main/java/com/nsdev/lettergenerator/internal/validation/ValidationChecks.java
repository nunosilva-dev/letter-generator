package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.xml.XmlCharacters;

/** Shared validation primitives; every error retains the caller's document field path. */
final class ValidationChecks {
    /** Prevents instantiation of shared field-path validation primitives. */
    private ValidationChecks() {}

    /**
     * Throws a field-path input exception when a semantic precondition is false.
     *
     * @param condition required semantic condition
     * @param path model-field path used to locate any validation error
     * @param message detail describing the violated requirement
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if condition is false
     */
    static void require(boolean condition, String path, String message) {
        if (!condition) {
            throw new InvalidLetterException(path, message);
        }
    }

    /**
     * Requires a non-null value without changing it or replacing it with a default.
     *
     * @param value candidate required value
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if value is null
     */
    static void required(Object value, String path) {
        require(value != null, path, "is required");
    }

    /**
     * Rejects NaN and positive/negative infinity for a numeric model field.
     *
     * @param value numeric field in the caller's units
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if value is NaN or
     *     infinite
     */
    static void finite(double value, String path) {
        require(Double.isFinite(value), path, "must be finite");
    }

    /**
     * Requires a non-null, finite value strictly greater than zero.
     *
     * @param value required numeric field in the caller's units
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if value is null,
     *     non-finite, zero, or negative
     */
    static void positive(Double value, String path) {
        required(value, path);
        finite(value, path);
        require(value > 0, path, "must be positive");
    }

    /**
     * Requires a finite value greater than or equal to zero.
     *
     * @param value numeric field in the caller's units
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if value is non-finite or
     *     negative
     */
    static void nonnegative(double value, String path) {
        finite(value, path);
        require(value >= 0, path, "must be non-negative");
    }

    /**
     * Requires non-null XML-safe text; empty and whitespace-only strings remain valid.
     *
     * @param value required text, which may be empty or whitespace-only
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if value is null or
     *     contains a code point outside XML 1.0 character ranges
     */
    static void text(String value, String path) {
        required(value, path);
        require(value.codePoints().allMatch(XmlCharacters::isValid), path, "invalid XML character");
    }

    /**
     * Adds a nonblank constraint to the shared XML-safe text validation.
     *
     * @param value required descriptive text
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if value is null, blank,
     *     or contains a code point outside XML 1.0 character ranges
     */
    static void nonblankText(String value, String path) {
        text(value, path);
        require(!value.isBlank(), path, "must not be blank");
    }
}
