package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.layout.*;

/** Writes page masters and fixed regions independently of body block serialization. */
final class FoPageLayoutWriter {
    static final String PAGE_SEQUENCE_NAME = "letter";
    static final String BODY_FLOW_NAME = "xsl-region-body";
    private final XmlBuilder xml;
    private final LetterDocument document;
    private final FoBlockWriter blocks;

    FoPageLayoutWriter(XmlBuilder xml, LetterDocument document, FoBlockWriter blocks) {
        this.xml = xml;
        this.document = document;
        this.blocks = blocks;
    }

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

    void fixedContent() {
        for (PageMaster master : PageMaster.values()) {
            fixedContent(master, Zone.TOP);
            fixedContent(master, Zone.BOTTOM);
        }
    }

    private void master(PageMaster master) {
        PageSpec page = document.page();
        Margins margins = page.marginsMm();
        double bottom = page.bottomZoneMm() + (master.isLastPage() ? page.signatureZoneMm() : 0);
        xml.start("simple-page-master")
                .attribute("master-name", master.identifier())
                .attribute("page-width", FoLengths.mm(page.size().widthMm()))
                .attribute("page-height", FoLengths.mm(page.size().heightMm()))
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
