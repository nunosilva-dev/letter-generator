package com.nsdev.lettergenerator.model.table;

/**
 * Cell inner spacing, in millimeters. Each value must be finite and nonnegative at generation.
 *
 * @param topMm space inside the top edge
 * @param rightMm space inside the right edge
 * @param bottomMm space inside the bottom edge
 * @param leftMm space inside the left edge
 */
public record Padding(double topMm, double rightMm, double bottomMm, double leftMm) {
    /** Legacy one-millimeter padding on each edge. */
    public static final Padding DEFAULT = new Padding(1, 1, 1, 1);

    /** Explicit zero padding on each edge. */
    public static final Padding ZERO = new Padding(0, 0, 0, 0);

    /**
     * Creates equal padding on all edges, deferring range checks to generation.
     *
     * @param millimeters uniform inner spacing
     * @return immutable four-edge padding
     */
    public static Padding uniform(double millimeters) {
        return new Padding(millimeters, millimeters, millimeters, millimeters);
    }
}
