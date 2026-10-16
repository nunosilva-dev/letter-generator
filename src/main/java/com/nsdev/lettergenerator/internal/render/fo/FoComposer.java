package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import java.net.URI;
import java.util.Map;

/** Converts validated immutable input into internal XSL-FO and supplied image resources. */
public final class FoComposer {
    private static final String FO_PREFIX = "fo";
    private static final String FO_NAMESPACE = "http://www.w3.org/1999/XSL/Format";
    private static final int SOFT_KEEP_PRIORITY = 1;
    private final LetterDocument document;
    private final boolean dedicatedSignaturePage;

    public record Composition(String xml, Map<URI, byte[]> images) {}

    public FoComposer(LetterDocument document, boolean dedicatedSignaturePage) {
        this.document = document;
        this.dedicatedSignaturePage = dedicatedSignaturePage;
    }

    public Composition compose() {
        XmlBuilder xml = new XmlBuilder(FO_PREFIX, FO_NAMESPACE);
        FoBlockWriter blocks = new FoBlockWriter(xml, document.defaultStyle());
        FoPageLayoutWriter layout = new FoPageLayoutWriter(xml, document, blocks);
        xml.start("root");
        layout.masters(dedicatedSignaturePage);
        xml.start("page-sequence")
                .attribute("master-reference", FoPageLayoutWriter.PAGE_SEQUENCE_NAME)
                .attribute("force-page-count", "no-force");
        layout.fixedContent();
        xml.start("flow").attribute("flow-name", FoPageLayoutWriter.BODY_FLOW_NAME);
        boolean hasPageBreak =
                document.sections().stream()
                        .flatMap(section -> section.blocks().stream())
                        .anyMatch(PageBreakBlock.class::isInstance);
        if (hasPageBreak) {
            FoPageBreaks.anchor(xml, false);
        }
        for (Section section : document.sections()) {
            blocks.blocks(section.blocks(), section.style());
        }
        flowSignature(xml, blocks);
        if (dedicatedSignaturePage) {
            terminalPage(xml);
        }
        xml.end().end().end();
        return new Composition(xml.finish(), blocks.images());
    }

    private void flowSignature(XmlBuilder xml, FoBlockWriter blocks) {
        Signature signature = document.signature();
        if (signature != null && signature.placement() == SignaturePlacement.FLOW) {
            xml.start("block").attribute("keep-together.within-page", SOFT_KEEP_PRIORITY);
            blocks.blocks(signature.blocks(), null);
            xml.end();
        }
    }

    private static void terminalPage(XmlBuilder xml) {
        // A zero-height line forces FOP to allocate the reserved last master without adding visible
        // text.
        xml.start("block")
                .attribute("break-before", "page")
                .attribute("font-size", FoLengths.pt(0))
                .attribute("line-height", FoLengths.pt(0))
                .attribute("white-space-collapse", false)
                .text(" ")
                .end();
    }
}
