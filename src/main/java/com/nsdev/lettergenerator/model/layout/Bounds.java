package com.nsdev.lettergenerator.model.layout;

/**
 * Rectangle measured from a containing zone's upper-left corner, in millimeters. The x axis runs
 * right and the y axis runs down. For a fixed element the containing zone is TOP or BOTTOM; a
 * BOTTOM_LAST signature uses the last-page signature zone.
 *
 * <p>Construction performs no validation. Generation requires finite, nonnegative x/y, positive
 * width/height, and a rectangle fitting inside its containing zone.
 *
 * @param x horizontal offset from the zone's left edge, in millimeters
 * @param y vertical offset from the zone's top edge, in millimeters
 * @param width rectangle width in millimeters
 * @param height rectangle height in millimeters
 */
public record Bounds(double x, double y, double width, double height) {
    /**
     * Returns the horizontal coordinate of the right edge in the same zone.
     *
     * @return x plus width, in millimeters
     */
    public double right() {
        return x + width;
    }

    /**
     * Returns the vertical coordinate of the bottom edge in the same zone.
     *
     * @return y plus height, in millimeters
     */
    public double bottom() {
        return y + height;
    }

    /**
     * Tests whether rectangles overlap with positive area. Merely touching an edge or corner is not
     * an intersection. Both rectangles must use the same coordinate system.
     *
     * @param other rectangle to compare
     * @return true if their interiors overlap
     * @throws NullPointerException if other is null
     */
    public boolean intersects(Bounds other) {
        return x < other.right()
                && other.x() < right()
                && y < other.bottom()
                && other.y() < bottom();
    }
}
