package com.nsdev.lettergenerator.internal.builder;

import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Zone;
import java.util.ArrayList;
import java.util.List;

/** Compiles ordinary letter regions to the explicit geometry required by the rendering model. */
public final class LetterRegions {
    /** Separation between generated fixed regions and the body, in millimeters. */
    private static final double REGION_GAP_MM = 5.0;

    /** Stable identifier assigned to the generated repeated header. */
    private static final String HEADER_ID = "letter.header";

    /** Stable identifier assigned to the generated repeated footer. */
    private static final String FOOTER_ID = "letter.footer";

    /** Stable identifier assigned to the generated repeated logo. */
    private static final String LOGO_ID = "letter.logo";

    /** Prevents construction of this static helper. */
    private LetterRegions() {}

    /**
     * Geometry output consumed by the letter builder when assembling its document.
     *
     * @param page page specification with body reservations in millimeters
     * @param elements fixed elements in logo, header, footer, then caller-supplied order
     */
    public record Layout(PageSpec page, List<FixedElement> elements) {
        /**
         * Snapshots the element list; elements and the immutable page are retained.
         *
         * @param page page geometry
         * @param elements non-null element list without null entries
         * @throws NullPointerException if the list or an entry is null
         */
        public Layout {
            elements = List.copyOf(elements);
        }
    }

    /**
     * Compiles convenience regions into fixed elements and body-height reservations. A populated
     * top or bottom zone includes a 5 mm separation gap. Extra top/bottom elements activate the
     * corresponding reservation but do not enlarge its configured height; later validation checks
     * their fit. Generated regions repeat on every page.
     *
     * @param size non-null paper size
     * @param margins non-null physical margins in millimeters
     * @param header non-null header block list
     * @param footer non-null footer block list
     * @param logo optional fitted logo
     * @param headerBounds non-null resolved top-zone header box in millimeters
     * @param footerHeightMm configured footer height in millimeters
     * @param extra non-null additional fixed elements, appended after generated ones
     * @return page specification and a snapshot of the combined fixed elements
     * @throws IllegalArgumentException if margins, body space or the populated header box are
     *     invalid
     */
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
