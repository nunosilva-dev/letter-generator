package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.xml.XmlBuilder;

/** Keeps explicit page transitions distinct when FOP removes empty/zero-height blocks. */
final class FoPageBreaks {
    // One FOP millipoint: a real line area without visible ink. A nonbreaking space prevents
    // whitespace removal. The initial anchor also preserves a deliberately blank first page.
    /** One FOP millipoint expressed in PDF points, producing a real but nonvisible line area. */
    private static final double ANCHOR_SIZE_PT = 0.001;

    /** Whitespace-preserving anchor character that prevents explicit blank pages collapsing. */
    private static final String NONBREAKING_SPACE = "\u00A0";

    /** Prevents instances of the page-transition serializer. */
    private FoPageBreaks() {}

    /**
     * Writes a one-millipoint nonbreaking-space line. An initial anchor preserves a blank first
     * page; transition anchors prevent leading, trailing or consecutive page breaks from
     * collapsing.
     *
     * @param xml current composition stream, inside the body flow
     * @param newPage whether this anchor starts a new page or preserves the initial page
     */
    static void anchor(XmlBuilder xml, boolean newPage) {
        xml.start("block");
        if (newPage) {
            xml.attribute("break-before", "page");
        }
        xml.attribute("font-size", FoLengths.pt(ANCHOR_SIZE_PT))
                .attribute("line-height", FoLengths.pt(ANCHOR_SIZE_PT))
                .attribute("white-space-collapse", false)
                .text(NONBREAKING_SPACE)
                .end();
    }
}
