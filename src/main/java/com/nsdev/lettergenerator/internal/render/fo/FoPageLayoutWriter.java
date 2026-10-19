package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.layout.Zone;

/**
 * Serializes the physical page regions separately from content blocks.
 *
 * <p>ONLY/FIRST/MIDDLE/LAST masters share paper and margins. Last-page masters enlarge the bottom
 * region by the signature zone; repeated footers move below that zone and a bottom-last signature
 * occupies its top. Fixed-element selectors are evaluated against each master before content is
 * emitted.
 */
final class FoPageLayoutWriter {
    /** Master-sequence identifier referenced by the document page sequence. */
    static final String PAGE_SEQUENCE_NAME = "letter";

    /** Stable target for total-page citations, covering the complete rendered page sequence. */
    static final String PAGE_SEQUENCE_ID = "letter.document";

    /** Standard FO flow name used for all ordered body sections. */
    static final String BODY_FLOW_NAME = "xsl-region-body";

    /** Current composition XML stream. */
    private final XmlBuilder xml;

    /** Validated page reservations, fixed content and signature configuration. */
    private final LetterDocument document;

    /** Shared block serializer that also accumulates fixed-region image resources. */
    private final FoBlockWriter blocks;

    /**
     * Shares the current composition writer and block serializer; no engine state is created here.
     *
     * @param xml current composition stream
     * @param document validated page/fixed-region configuration
     * @param blocks shared block serializer
     */
    FoPageLayoutWriter(XmlBuilder xml, LetterDocument document, FoBlockWriter blocks) {
        this.xml = xml;
        this.document = document;
        this.blocks = blocks;
    }

    /**
     * Writes master definitions and ordered selection alternatives. Recovery prevents a one-page
     * master from consuming the dedicated signature page.
     *
     * @param dedicatedSignaturePage whether recovery must distinguish the first page from a later
     *     last page
     */
    void masters(boolean dedicatedSignaturePage) {
        xml.start("layout-master-set");
        for (PageMaster master : PageMaster.values()) {
            master(master);
        }
        xml.start("page-sequence-master").attribute("master-name", PAGE_SEQUENCE_NAME);
        xml.start("repeatable-page-master-alternatives");
        for (PageMaster master : PageMaster.SELECTION_ORDER) {
            PageMaster reference =
                    dedicatedSignaturePage && master == PageMaster.ONLY ? PageMaster.FIRST : master;
            xml.start("conditional-page-master-reference")
                    .attribute("master-reference", reference.identifier())
                    .attribute("page-position", master.pagePosition())
                    .end();
        }
        xml.end().end().end();
    }

    /**
     * Creates separate top/bottom static flows for every page master so selected fixed elements can
     * repeat without entering body flow.
     */
    void fixedContent() {
        for (PageMaster master : PageMaster.values()) {
            fixedContent(master, Zone.TOP);
            fixedContent(master, Zone.BOTTOM);
        }
    }

    /**
     * Derives body and before/after region extents from model millimeters, reserving the signature
     * height only for last-page masters.
     *
     * @param master page state whose region extents are being defined
     */
    private void master(PageMaster master) {
        PageSpec page = document.page();
        Margins margins = page.marginsMm();
        double bottom = page.bottomZoneMm() + (master.isLastPage() ? page.signatureZoneMm() : 0);
        xml.start("simple-page-master")
                .attribute("master-name", master.identifier())
                .attribute("page-width", FoLengths.mm(page.widthMm()))
                .attribute("page-height", FoLengths.mm(page.heightMm()))
                .attribute("margin-top", FoLengths.mm(margins.top()))
                .attribute("margin-right", FoLengths.mm(margins.right()))
                .attribute("margin-bottom", FoLengths.mm(margins.bottom()))
                .attribute("margin-left", FoLengths.mm(margins.left()));
        xml.start("region-body")
                .attribute("margin-top", FoLengths.mm(page.topZoneMm()))
                .attribute("margin-bottom", FoLengths.mm(bottom))
                .end();
        xml.start("region-before")
                .attribute("region-name", master.topRegion())
                .attribute("extent", FoLengths.mm(page.topZoneMm()))
                .end();
        xml.start("region-after")
                .attribute("region-name", master.bottomRegion())
                .attribute("extent", FoLengths.mm(bottom))
                .end();
        xml.end();
    }

    /**
     * Positions selected fixed boxes and the optional bottom-last signature relative to the chosen
     * region origin.
     *
     * @param master page state used to evaluate content selectors
     * @param zone top or bottom static flow being serialized
     */
    private void fixedContent(PageMaster master, Zone zone) {
        xml.start("static-content")
                .attribute(
                        "flow-name", zone == Zone.TOP ? master.topRegion() : master.bottomRegion());
        xml.start("block").end();
        for (FixedElement element : document.fixedElements()) {
            if (element.zone() == zone && master.matches(element.pages())) {
                double offset =
                        zone == Zone.BOTTOM && master.isLastPage()
                                ? document.page().signatureZoneMm()
                                : 0;
                blocks.positionedBox(element.boundsMm(), offset, element.blocks());
            }
        }
        Signature signature = document.signature();
        if (zone == Zone.BOTTOM
                && master.isLastPage()
                && signature != null
                && signature.placement() == SignaturePlacement.BOTTOM_LAST) {
            blocks.positionedBox(signature.boundsMm(), 0, signature.blocks());
        }
        xml.end();
    }
}
