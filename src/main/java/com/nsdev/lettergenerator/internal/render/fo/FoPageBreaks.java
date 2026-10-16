package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.xml.XmlBuilder;

/** Keeps explicit page transitions distinct when FOP removes empty/zero-height blocks. */
final class FoPageBreaks {
    // One FOP millipoint: a real line area without visible ink. A nonbreaking space prevents
    // whitespace removal. The initial anchor also preserves a deliberately blank first page.
    private static final double ANCHOR_SIZE_PT = 0.001;
    private static final String NONBREAKING_SPACE = "\u00A0";

    private FoPageBreaks() {}

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
