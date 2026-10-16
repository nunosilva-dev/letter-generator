package com.nsdev.lettergenerator.internal.builder;

import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.layout.*;
import java.util.ArrayList;
import java.util.List;

/** Compiles ordinary letter regions to the explicit geometry required by the rendering model. */
public final class LetterRegions {
    private static final double REGION_GAP_MM = 5.0;
    private static final String HEADER_ID = "letter.header";
    private static final String FOOTER_ID = "letter.footer";
    private static final String LOGO_ID = "letter.logo";

    private LetterRegions() {}

    public record Layout(PageSpec page, List<FixedElement> elements) {
        public Layout {
            elements = List.copyOf(elements);
        }
    }

    public static Layout arrange(
            PageSize size,
            Margins margins,
            List<Block> header,
            List<Block> footer,
            ImageBlock logo,
            Bounds headerBounds,
            double footerHeightMm,
            List<FixedElement> extra) {
        BuilderChecks.nonnegative(margins.top(), "top margin");
        BuilderChecks.nonnegative(margins.right(), "right margin");
        BuilderChecks.nonnegative(margins.bottom(), "bottom margin");
        BuilderChecks.nonnegative(margins.left(), "left margin");
        double width = size.widthMm() - margins.left() - margins.right();
        BuilderChecks.positive(width, "available page width");
        boolean hasHeader =
                logo != null
                        || !header.isEmpty()
                        || extra.stream().anyMatch(element -> element.zone() == Zone.TOP);
        boolean hasFooter =
                !footer.isEmpty()
                        || extra.stream().anyMatch(element -> element.zone() == Zone.BOTTOM);
        double headerHeight =
                logo == null
                        ? headerBounds.bottom()
                        : Math.max(headerBounds.bottom(), logo.heightMm());
        double topZone = hasHeader ? headerHeight + REGION_GAP_MM : 0;
        double bottomZone = hasFooter ? footerHeightMm + REGION_GAP_MM : 0;
        BuilderChecks.positive(
                size.heightMm() - margins.top() - margins.bottom() - topZone - bottomZone,
                "available body height");
        var elements = new ArrayList<FixedElement>();
        if (logo != null) {
            elements.add(
                    new FixedElement(
                            LOGO_ID,
                            Zone.TOP,
                            PageSelector.ALL,
                            new Bounds(0, 0, logo.widthMm(), logo.heightMm()),
                            List.of(logo)));
        }
        if (!header.isEmpty()) {
            BuilderChecks.nonnegative(headerBounds.x(), "header x");
            BuilderChecks.positive(headerBounds.width(), "available header width beside the logo");
            BuilderChecks.require(
                    headerBounds.right() <= width + PageGeometry.GEOMETRY_TOLERANCE_MM,
                    "Header box exceeds the available page width");
            elements.add(
                    new FixedElement(HEADER_ID, Zone.TOP, PageSelector.ALL, headerBounds, header));
        }
        if (!footer.isEmpty()) {
            elements.add(
                    new FixedElement(
                            FOOTER_ID,
                            Zone.BOTTOM,
                            PageSelector.ALL,
                            new Bounds(0, REGION_GAP_MM, width, footerHeightMm),
                            footer));
        }
        elements.addAll(extra);
        return new Layout(new PageSpec(size, margins, topZone, bottomZone, 0d), elements);
    }
}
