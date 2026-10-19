package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Converts a validated letter model into XSL Formatting Objects (FO), not PDF bytes.
 *
 * <p>Page masters/fixed regions are written by {@code FoPageLayoutWriter}; flow blocks and image
 * URIs are written by {@code FoBlockWriter}. The result is passed to the FOP boundary. All mutable
 * writer and resource state belongs to one {@link #compose()} invocation.
 */
public final class FoComposer {
    /** Namespace prefix used consistently for every generated FO element. */
    private static final String FO_PREFIX = "fo";

    /** XML namespace identifying the XSL Formatting Objects vocabulary. */
    private static final String FO_NAMESPACE = "http://www.w3.org/1999/XSL/Format";

    /**
     * A breakable keep strength that prefers a grouped signature without forcing an impossible
     * layout.
     */
    private static final int SOFT_KEEP_PRIORITY = 1;

    /** Validated model read by this composition attempt. */
    private final LetterDocument document;

    /** Whether recovery must append a terminal page to select the last-page master. */
    private final boolean dedicatedSignaturePage;

    /**
     * One attempt's generated FO and the image lookup needed to resolve its private URIs.
     *
     * @param xml complete, escaped XSL-FO document
     * @param images URI-to-bytes lookup supplied by the block writer; this record does not
     *     deep-copy arrays
     */
    public record Composition(String xml, Map<URI, byte[]> images) {}

    /**
     * Configures one composition attempt after input validation.
     *
     * @param document model whose declared sizes, fonts and resources have been checked
     * @param dedicatedSignaturePage whether to append a terminal page for reserved-signature
     *     recovery
     */
    public FoComposer(LetterDocument document, boolean dedicatedSignaturePage) {
        this.document = document;
        this.dedicatedSignaturePage = dedicatedSignaturePage;
    }

    /**
     * Writes page masters, fixed content and ordered body sections into one FO document.
     *
     * @return FO and its registered image resources; nothing has been rendered yet
     */
    public Composition compose() {
        XmlBuilder xml = new XmlBuilder(FO_PREFIX, FO_NAMESPACE);
        FoBlockWriter blocks = new FoBlockWriter(xml, document.defaultStyle());
        FoPageLayoutWriter layout = new FoPageLayoutWriter(xml, document, blocks);
        xml.start("root").attribute("xmlns:fox", "http://xmlgraphics.apache.org/fop/extensions");
        FoDocumentSettings.write(xml, document.settings());
        layout.masters(dedicatedSignaturePage);
        FoDocumentSettings.catalog(xml, document.settings());
        xml.start("page-sequence")
                .attribute("id", FoPageLayoutWriter.PAGE_SEQUENCE_ID)
                .attribute("master-reference", FoPageLayoutWriter.PAGE_SEQUENCE_NAME)
                .attribute("force-page-count", "no-force");
        layout.fixedContent();
        xml.start("flow").attribute("flow-name", FoPageLayoutWriter.BODY_FLOW_NAME);
        boolean hasPageBreak =
                document.sections().stream()
                        .anyMatch(section -> containsPageBreak(section.blocks()));
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

    /**
     * Finds breaks inside transparent style scopes as well as direct section content.
     *
     * @param content validated ordered blocks
     * @return whether initial flow anchoring is needed
     */
    private static boolean containsPageBreak(List<Block> content) {
        return content.stream()
                .anyMatch(
                        block ->
                                block instanceof PageBreakBlock
                                        || block instanceof GroupBlock group
                                                && containsPageBreak(group.blocks()));
    }

    /**
     * Appends FLOW signature blocks as one softly kept group after the body; BOTTOM_LAST signatures
     * are emitted by page-layout serialization instead.
     *
     * @param xml current composition stream, inside the body flow
     * @param blocks shared text/image serializer
     */
    private void flowSignature(XmlBuilder xml, FoBlockWriter blocks) {
        Signature signature = document.signature();
        if (signature != null && signature.placement() == SignaturePlacement.FLOW) {
            xml.start("block").attribute("keep-together.within-page", SOFT_KEEP_PRIORITY);
            blocks.blocks(signature.blocks(), null);
            xml.end();
        }
    }

    /**
     * Adds a nonvisible page transition used only by the bounded last-page recovery attempt.
     *
     * @param xml current composition stream, inside the body flow
     */
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
