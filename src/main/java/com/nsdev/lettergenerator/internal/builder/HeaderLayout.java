package com.nsdev.lettergenerator.internal.builder;

import com.nsdev.lettergenerator.internal.defaults.LetterLayoutDefaults;
import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.internal.util.ArgumentChecks;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.style.TextAlign;

/**
 * Immutable header options resolved by {@link com.nsdev.lettergenerator.builder.LetterBuilder}
 * after its final page and logo settings are known. Explicit bounds bypass automatic placement.
 *
 * @param heightMm automatic header height in millimeters
 * @param widthMm automatic width in millimeters, or null for the space beside the logo
 * @param alignment automatic horizontal alignment, or null for left alignment
 * @param boundsMm exact bounds relative to the top zone, or null for automatic placement
 */
public record HeaderLayout(double heightMm, Double widthMm, TextAlign alignment, Bounds boundsMm) {
    /** Horizontal separation between the logo and automatic header, in millimeters. */
    private static final double LOGO_GAP_MM = 5.0;

    /**
     * Validates dimensions immediately; page fit is checked when geometry is resolved.
     *
     * @param heightMm finite positive header height
     * @param widthMm optional finite positive width
     * @param alignment optional left, center or right alignment
     * @param boundsMm optional nonnegative origin and positive size, all in millimeters
     * @throws IllegalArgumentException if dimensions are invalid or alignment is justify
     */
    public HeaderLayout {
        ArgumentChecks.positive(heightMm, "header height");
        if (widthMm != null) {
            ArgumentChecks.positive(widthMm, "header width");
        }
        ArgumentChecks.require(
                alignment != TextAlign.JUSTIFY, "Header alignment must be LEFT, CENTER or RIGHT");
        if (boundsMm != null) {
            ArgumentChecks.nonnegative(boundsMm.x(), "header x");
            ArgumentChecks.nonnegative(boundsMm.y(), "header y");
            ArgumentChecks.positive(boundsMm.width(), "header width");
            ArgumentChecks.positive(boundsMm.height(), "header height");
        }
    }

    /**
     * @return automatic left-aligned layout with a 25 mm height and available width
     */
    public static HeaderLayout defaults() {
        return new HeaderLayout(LetterLayoutDefaults.HEADER_HEIGHT_MM, null, null, null);
    }

    /**
     * Returns a copy with the requested alignment. Null and justify are rejected.
     *
     * @param value left, center or right alignment
     * @return the updated immutable options
     * @throws IllegalArgumentException if the supplied alignment is invalid
     */
    public HeaderLayout withAlignment(TextAlign value) {
        return new HeaderLayout(
                heightMm, widthMm, ArgumentChecks.required(value, "header alignment"), boundsMm);
    }

    /**
     * Returns a copy with the requested width. An explicit box is resized too.
     *
     * @param value finite positive width in millimeters
     * @return the updated immutable options
     * @throws IllegalArgumentException if the supplied width is invalid
     */
    public HeaderLayout withWidth(double value) {
        Bounds bounds =
                boundsMm == null
                        ? null
                        : new Bounds(boundsMm.x(), boundsMm.y(), value, boundsMm.height());
        return new HeaderLayout(heightMm, value, alignment, bounds);
    }

    /**
     * Returns a copy with the requested height. An explicit box is resized too.
     *
     * @param value finite positive height in millimeters
     * @return the updated immutable options
     * @throws IllegalArgumentException if the supplied height is invalid
     */
    public HeaderLayout withHeight(double value) {
        Bounds bounds =
                boundsMm == null
                        ? null
                        : new Bounds(boundsMm.x(), boundsMm.y(), boundsMm.width(), value);
        return new HeaderLayout(value, widthMm, alignment, bounds);
    }

    /**
     * Returns a copy with the requested bounds. The width and height options are synchronized with
     * the box.
     *
     * @param value non-null explicit bounds in millimeters
     * @return the updated immutable options
     * @throws IllegalArgumentException if the supplied bounds is invalid
     */
    public HeaderLayout withBounds(Bounds value) {
        ArgumentChecks.required(value, "header bounds");
        return new HeaderLayout(value.height(), value.width(), alignment, value);
    }

    /**
     * Resolves automatic bounds beside a logo with a 5 mm gap. The chosen alignment distributes
     * spare horizontal space; automatic height includes a taller logo. Explicit bounds return
     * unchanged and are checked later by {@link LetterRegions} and document validation.
     *
     * @param innerWidth page width inside the left/right margins, in millimeters
     * @param logo fitted logo, or null
     * @return top-zone-relative bounds in millimeters
     * @throws IllegalArgumentException if an explicit automatic width exceeds the available space
     */
    public Bounds resolve(double innerWidth, ImageBlock logo) {
        if (boundsMm != null) {
            return boundsMm;
        }
        double left = logo == null ? 0 : Math.min(logo.widthMm(), innerWidth) + LOGO_GAP_MM;
        double available = innerWidth - left;
        double width = widthMm == null ? available : widthMm;
        if (widthMm != null) {
            ArgumentChecks.require(
                    width <= available + PageGeometry.GEOMETRY_TOLERANCE_MM,
                    "Header width exceeds the available space beside the logo");
        }
        double spare = Math.max(0, available - width);
        double offset =
                alignment == TextAlign.RIGHT
                        ? spare
                        : alignment == TextAlign.CENTER ? spare / 2 : 0;
        return new Bounds(
                left + offset,
                0,
                width,
                logo == null ? heightMm : Math.max(heightMm, logo.heightMm()));
    }
}
