package com.nsdev.lettergenerator.render;

/**
 * Declared layout rectangle, measured from the physical page's upper-left corner. These rectangles
 * describe allocated areas, not the bounds of individual glyphs or image pixels.
 *
 * @param pageNumber one-based page number
 * @param kind declared rectangle classification
 * @param xMm left edge in millimeters
 * @param yMm top edge in millimeters
 * @param widthMm allocated width in millimeters
 * @param heightMm allocated height in millimeters
 */
public record DebugBox(
        int pageNumber, String kind, double xMm, double yMm, double widthMm, double heightMm) {}
