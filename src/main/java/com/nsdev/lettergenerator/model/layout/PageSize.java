package com.nsdev.lettergenerator.model.layout;

/** Supported physical paper dimensions, in millimeters. */
public enum PageSize {
    A4(210.0, 297.0),
    LETTER(215.9, 279.4);

    private final double widthMm;
    private final double heightMm;

    PageSize(double widthMm, double heightMm) {
        this.widthMm = widthMm;
        this.heightMm = heightMm;
    }

    public double widthMm() {
        return widthMm;
    }

    public double heightMm() {
        return heightMm;
    }
}
