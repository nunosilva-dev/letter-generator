package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSpec;

final class GeometryValidator {
    private GeometryValidator() {}

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
