package com.nsdev.lettergenerator.internal.builder;

/** Immediate argument checks for the application-facing convenience API. */
public final class BuilderChecks {
    /** Prevents construction of this static helper. */
    private BuilderChecks() {}

    /**
     * Enforces an immediate convenience-API precondition.
     *
     * @param valid whether the precondition holds
     * @param message diagnostic used verbatim on failure
     * @throws IllegalArgumentException if {@code valid} is false
     */
    public static void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks a required argument and preserves its identity.
     *
     * @param <T> argument type
     * @param value argument to check
     * @param name argument name for the diagnostic
     * @return the same non-null value
     * @throws IllegalArgumentException if the value is null
     */
    public static <T> T required(T value, String name) {
        require(value != null, name + " must not be null");
        return value;
    }

    /**
     * Checks a finite, strictly positive scalar in the caller's units.
     *
     * @param value scalar to check
     * @param name argument name for the diagnostic
     * @return the checked value
     * @throws IllegalArgumentException if non-finite or not positive
     */
    public static double positive(double value, String name) {
        require(Double.isFinite(value) && value > 0, name + " must be finite and positive");
        return value;
    }

    /**
     * Checks a finite scalar that may be zero, in the caller's units.
     *
     * @param value scalar to check
     * @param name argument name for the diagnostic
     * @return the checked value
     * @throws IllegalArgumentException if non-finite or negative
     */
    public static double nonnegative(double value, String name) {
        require(Double.isFinite(value) && value >= 0, name + " must be finite and non-negative");
        return value;
    }
}
