package com.nsdev.lettergenerator.internal.layout;

import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSpec;

/** Derived inner page dimensions, calculated after geometry validation. */
public record PageGeometry(
        double innerWidthMm, double innerHeightMm, double bodyHeightMm, double lastBodyHeightMm) {
    public static final double GEOMETRY_TOLERANCE_MM = 0.000001;

    public static PageGeometry from(PageSpec page) {
        Margins margins = page.marginsMm();
        double width = page.size().widthMm() - margins.left() - margins.right();
        double height = page.size().heightMm() - margins.top() - margins.bottom();
        double bodyHeight = height - page.topZoneMm() - page.bottomZoneMm();
        return new PageGeometry(width, height, bodyHeight, bodyHeight - page.signatureZoneMm());
    }
}
