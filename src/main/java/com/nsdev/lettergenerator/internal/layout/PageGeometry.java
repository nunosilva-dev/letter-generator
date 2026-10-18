package com.nsdev.lettergenerator.internal.layout;

import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSpec;

/**
 * Derived dimensions used by validation and page-master layout.
 *
 * <p>The inner rectangle excludes page margins. The ordinary body additionally excludes the top and
 * bottom zones; the final body also excludes the signature zone. Calculation itself accepts
 * negative results so {@code GeometryValidator} can report the offending input field.
 *
 * @param innerWidthMm width between left and right margins, in millimeters
 * @param innerHeightMm height between top and bottom margins, in millimeters
 * @param bodyHeightMm ordinary body height after fixed-zone reservation, in millimeters
 * @param lastBodyHeightMm final-page body height after signature reservation, in millimeters
 */
public record PageGeometry(
        double innerWidthMm, double innerHeightMm, double bodyHeightMm, double lastBodyHeightMm) {
    /**
     * Tolerance for floating-point box-edge comparisons, in millimeters; it does not permit visible
     * overflow.
     */
    public static final double GEOMETRY_TOLERANCE_MM = 0.000001;

    /**
     * Calculates geometry from a model whose optional page fields have already been defaulted.
     *
     * @param page non-null page size, margins and reserved-zone dimensions
     * @return derived dimensions, before positivity checks
     * @throws NullPointerException if page is null
     */
    public static PageGeometry from(PageSpec page) {
        Margins margins = page.marginsMm();
        double width = page.size().widthMm() - margins.left() - margins.right();
        double height = page.size().heightMm() - margins.top() - margins.bottom();
        double bodyHeight = height - page.topZoneMm() - page.bottomZoneMm();
        return new PageGeometry(width, height, bodyHeight, bodyHeight - page.signatureZoneMm());
    }
}
