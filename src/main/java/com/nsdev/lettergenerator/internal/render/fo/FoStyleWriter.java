package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import java.util.Locale;

/**
 * Writes fully resolved model style values as FO attributes. Font sizes use points, spacing uses
 * millimeters, and line height remains a multiplier.
 */
final class FoStyleWriter {
    /** Prevents instances of the resolved-style serializer. */
    private FoStyleWriter() {}

    /**
     * Writes font/decoration attributes for an inline run. Paragraph-level line-height handling
     * remains with the block writer.
     *
     * @param xml current composition stream with an open element start tag
     * @param style resolved appearance containing concrete values for every property
     */
    static void inline(XmlBuilder xml, ResolvedStyle style) {
        font(xml, style);
        xml.attribute("line-height", style.lineHeight());
    }

    /**
     * Adds block alignment, line height and vertical spacing to the resolved font attributes.
     *
     * @param xml current composition stream with an open element start tag
     * @param style resolved appearance containing concrete values for every property
     */
    static void block(XmlBuilder xml, ResolvedStyle style) {
        font(xml, style);
        xml.attribute("text-align", style.textAlign().name().toLowerCase(Locale.ROOT));
        xml.attribute("line-height", style.lineHeight());
        spacing(xml, style);
    }

    /**
     * Emits model paragraph gaps in millimeters and retains explicit leading space at region/page
     * boundaries.
     *
     * @param xml current composition stream with an open element start tag
     * @param style resolved appearance containing concrete values for every property
     */
    static void spacing(XmlBuilder xml, ResolvedStyle style) {
        xml.attribute("space-before", FoLengths.mm(style.spaceBeforeMm()));
        xml.attribute("space-before.conditionality", "retain");
        xml.attribute("space-after", FoLengths.mm(style.spaceAfterMm()));
    }

    /**
     * Maps the resolved family, size, RGB color and emphasis flags to explicit FO font/decoration
     * attributes.
     *
     * @param xml current composition stream with an open element start tag
     * @param style resolved appearance containing concrete values for every property
     */
    private static void font(XmlBuilder xml, ResolvedStyle style) {
        xml.attribute("font-family", style.fontFamily());
        xml.attribute("font-size", FoLengths.pt(style.fontSizePt()));
        xml.attribute("color", style.color());
        xml.attribute("font-weight", style.bold() ? "bold" : "normal");
        xml.attribute("font-style", style.italic() ? "italic" : "normal");
        xml.attribute("text-decoration", style.underline() ? "underline" : "none");
    }
}
