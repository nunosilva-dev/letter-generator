package com.nsdev.lettergenerator.model.layout;

/** Box relative to its zone's upper-left corner, in millimeters. */
public record Bounds(double x, double y, double width, double height) {
    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }

    public boolean intersects(Bounds other) {
        return x < other.right()
                && other.x() < right()
                && y < other.bottom()
                && other.y() < bottom();
    }
}
