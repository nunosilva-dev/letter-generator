package com.nsdev.lettergenerator.internal.builder;

/** Immediate argument checks for the application-facing convenience API. */
public final class BuilderChecks {
    private BuilderChecks() {}

    public static void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }

    public static <T> T required(T value, String name) {
        require(value != null, name + " must not be null");
        return value;
    }

    public static double positive(double value, String name) {
        require(Double.isFinite(value) && value > 0, name + " must be finite and positive");
        return value;
    }

    public static double nonnegative(double value, String name) {
        require(Double.isFinite(value) && value >= 0, name + " must be finite and non-negative");
        return value;
    }
}
