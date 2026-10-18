package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSpec;

/**
 * Checks physical page reservations and fixed rectangles before the layout engine is invoked. All
 * dimensions are in millimeters and errors preserve model field paths.
 */
final class GeometryValidator {
    /** Prevents instantiation of the stateless geometry checks. */
    private GeometryValidator() {}

    /**
     * Requires finite nonnegative margins/zones and positive inner, ordinary-body and final-body
     * dimensions; returns the derived rectangle sizes for content validation.
     *
     * @param page page specification with record defaults already applied
     * @return derived dimensions whose width and body heights are positive
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if margins, zones, or
     *     resulting usable dimensions are invalid
     */
    static PageGeometry page(PageSpec page) {
        Margins margins = page.marginsMm();
        ValidationChecks.nonnegative(margins.top(), "page.marginsMm.top");
        ValidationChecks.nonnegative(margins.right(), "page.marginsMm.right");
        ValidationChecks.nonnegative(margins.bottom(), "page.marginsMm.bottom");
        ValidationChecks.nonnegative(margins.left(), "page.marginsMm.left");
        ValidationChecks.nonnegative(page.topZoneMm(), "page.topZoneMm");
        ValidationChecks.nonnegative(page.bottomZoneMm(), "page.bottomZoneMm");
        ValidationChecks.nonnegative(page.signatureZoneMm(), "page.signatureZoneMm");
        PageGeometry geometry = PageGeometry.from(page);
        ValidationChecks.require(geometry.innerWidthMm() > 0, "page.marginsMm", "no usable width");
        ValidationChecks.require(geometry.bodyHeightMm() > 0, "page", "no usable body height");
        ValidationChecks.require(
                geometry.lastBodyHeightMm() > 0,
                "page.signatureZoneMm",
                "no usable last-page body height");
        return geometry;
    }

    /**
     * Requires a positive rectangle wholly inside the supplied zone width/height, allowing only the
     * shared floating-point geometry tolerance at its far edges.
     *
     * @param bounds candidate rectangle, relative to the zone upper-left corner
     * @param width containing zone width in millimeters
     * @param height containing zone height in millimeters
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if the rectangle is
     *     missing, non-finite, non-positive in size, or outside its zone
     */
    static void bounds(Bounds bounds, double width, double height, String path) {
        ValidationChecks.required(bounds, path);
        ValidationChecks.nonnegative(bounds.x(), path + ".x");
        ValidationChecks.nonnegative(bounds.y(), path + ".y");
        ValidationChecks.positive(bounds.width(), path + ".width");
        ValidationChecks.positive(bounds.height(), path + ".height");
        ValidationChecks.require(
                bounds.right() <= width + PageGeometry.GEOMETRY_TOLERANCE_MM
                        && bounds.bottom() <= height + PageGeometry.GEOMETRY_TOLERANCE_MM,
                path,
                "box exceeds its zone");
    }
}
