package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import java.util.Locale;

final class FoStyleWriter {
    private FoStyleWriter() {}

    static void inline(XmlBuilder xml, ResolvedStyle style) {
        font(xml, style);
        xml.attribute("line-height", style.lineHeight());
    }

    static void block(XmlBuilder xml, ResolvedStyle style) {
        font(xml, style);
        xml.attribute("text-align", style.textAlign().name().toLowerCase(Locale.ROOT));
        xml.attribute("line-height", style.lineHeight());
        spacing(xml, style);
    }

    static void spacing(XmlBuilder xml, ResolvedStyle style) {
        xml.attribute("space-before", FoLengths.mm(style.spaceBeforeMm()));
        xml.attribute("space-before.conditionality", "retain");
        xml.attribute("space-after", FoLengths.mm(style.spaceAfterMm()));
    }

    private static void font(XmlBuilder xml, ResolvedStyle style) {
        xml.attribute("font-family", style.fontFamily());
        xml.attribute("font-size", FoLengths.pt(style.fontSizePt()));
        xml.attribute("color", style.color());
        xml.attribute("font-weight", style.bold() ? "bold" : "normal");
        xml.attribute("font-style", style.italic() ? "italic" : "normal");
        xml.attribute("text-decoration", style.underline() ? "underline" : "none");
    }
}
