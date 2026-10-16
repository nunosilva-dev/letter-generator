package com.nsdev.lettergenerator.internal.builder;

import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.style.TextAlign;

/** Deferred header geometry uses the final page margins, logo and content settings. */
public record HeaderLayout(double heightMm, Double widthMm, TextAlign alignment, Bounds boundsMm) {
    private static final double DEFAULT_HEIGHT_MM = 25.0;
    private static final double LOGO_GAP_MM = 5.0;

    public HeaderLayout {
        BuilderChecks.positive(heightMm, "header height");
        if (widthMm != null) {
            BuilderChecks.positive(widthMm, "header width");
        }
        BuilderChecks.require(
                alignment != TextAlign.JUSTIFY, "Header alignment must be LEFT, CENTER or RIGHT");
        if (boundsMm != null) {
            BuilderChecks.nonnegative(boundsMm.x(), "header x");
            BuilderChecks.nonnegative(boundsMm.y(), "header y");
            BuilderChecks.positive(boundsMm.width(), "header width");
            BuilderChecks.positive(boundsMm.height(), "header height");
        }
    }

    public static HeaderLayout defaults() {
        return new HeaderLayout(DEFAULT_HEIGHT_MM, null, null, null);
    }

    public HeaderLayout withAlignment(TextAlign value) {
        return new HeaderLayout(
                heightMm, widthMm, BuilderChecks.required(value, "header alignment"), boundsMm);
    }

    public HeaderLayout withWidth(double value) {
        Bounds bounds =
                boundsMm == null
                        ? null
                        : new Bounds(boundsMm.x(), boundsMm.y(), value, boundsMm.height());
        return new HeaderLayout(heightMm, value, alignment, bounds);
    }

    public HeaderLayout withHeight(double value) {
        Bounds bounds =
                boundsMm == null
                        ? null
                        : new Bounds(boundsMm.x(), boundsMm.y(), boundsMm.width(), value);
        return new HeaderLayout(value, widthMm, alignment, bounds);
    }

    public HeaderLayout withBounds(Bounds value) {
        BuilderChecks.required(value, "header bounds");
        return new HeaderLayout(value.height(), value.width(), alignment, value);
    }

    public Bounds resolve(double innerWidth, ImageBlock logo) {
        if (boundsMm != null) {
            return boundsMm;
        }
        double left = logo == null ? 0 : Math.min(logo.widthMm(), innerWidth) + LOGO_GAP_MM;
        double available = innerWidth - left;
        double width = widthMm == null ? available : widthMm;
        if (widthMm != null) {
            BuilderChecks.require(
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
