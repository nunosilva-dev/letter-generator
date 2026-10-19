package com.nsdev.lettergenerator;

import com.nsdev.lettergenerator.builder.CanvasBuilder;

/** Convenience entry point for stable, bounded positioned content within ordinary letter flow. */
public final class Canvas {
    /** Prevents construction of the authoring entry-point utility. */
    private Canvas() {}

    /**
     * Starts a mutable local builder; resulting models snapshot all positioned elements.
     *
     * @param widthMm positive canvas width in millimeters
     * @param heightMm positive canvas height in millimeters
     * @return a new builder for this declared canvas rectangle
     */
    public static CanvasBuilder builder(double widthMm, double heightMm) {
        return new CanvasBuilder(widthMm, heightMm);
    }
}
