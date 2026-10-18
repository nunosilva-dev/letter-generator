package com.nsdev.lettergenerator.model.layout;

/** Portrait paper sizes used to derive page, body, and reserved-zone dimensions. */
public enum PageSize {
    /** ISO A4 portrait: 210 by 297 millimeters. */
    A4(210.0, 297.0),
    /** US Letter portrait: 215.9 by 279.4 millimeters. */
    LETTER(215.9, 279.4);

    /** Physical portrait width before subtracting margins, in millimeters. */
    private final double widthMm;

    /** Physical portrait height before subtracting margins, in millimeters. */
    private final double heightMm;

    /**
     * Stores the physical portrait dimensions for a supported paper size.
     *
     * @param widthMm physical width in millimeters
     * @param heightMm physical height in millimeters
     */
    PageSize(double widthMm, double heightMm) {
        this.widthMm = widthMm;
        this.heightMm = heightMm;
    }

    /**
     * Returns the physical page width, before margins.
     *
     * @return width in millimeters
     */
    public double widthMm() {
        return widthMm;
    }

    /**
     * Returns the physical page height, before margins.
     *
     * @return height in millimeters
     */
    public double heightMm() {
        return heightMm;
    }
}
