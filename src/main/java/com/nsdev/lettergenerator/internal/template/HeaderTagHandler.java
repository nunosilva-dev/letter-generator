package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.builder.LetterBuilder;
import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import java.util.Locale;
import org.jsoup.nodes.Element;

/** Applies template header options before its markup is resolved against the final header box. */
final class HeaderTagHandler implements TemplateTagHandler {
    @Override
    public String tagName() {
        return "header";
    }

    @Override
    public void handle(Element element, TemplateContext context) {
        LetterBuilder builder = context.builder();
        Style defaults = context.page().defaults();
        MarkupSupport.attributes(element, "align", "width-mm", "height-mm", "x-mm", "y-mm");
        Style inherited = defaults;
        if (element.hasAttr("align")) {
            TextAlign alignment;
            try {
                alignment = TextAlign.valueOf(element.attr("align").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw MarkupSupport.error(element, "Header align must be left, center or right");
            }
            MarkupSupport.require(
                    alignment != TextAlign.JUSTIFY,
                    element,
                    "Header align must be left, center or right");
            builder.headerAlignment(alignment);
            inherited =
                    HtmlStyles.inherited(defaults)
                            .overlay(Style.builder().textAlign(alignment).build());
        }
        Style style = HtmlStyles.text(inherited, element);
        MarkupSupport.require(
                style.spaceBeforeMm() == null && style.spaceAfterMm() == null,
                element,
                "Apply paragraph margins to p/div text blocks");
        boolean exact = element.hasAttr("x-mm") || element.hasAttr("y-mm");
        if (exact) {
            for (String attribute : new String[] {"x-mm", "y-mm", "width-mm", "height-mm"}) {
                MarkupSupport.require(
                        element.hasAttr(attribute),
                        element,
                        "Exact header placement requires x-mm, y-mm, width-mm and height-mm");
            }
            builder.headerBoundsMm(
                    number(element, "x-mm"),
                    number(element, "y-mm"),
                    number(element, "width-mm"),
                    number(element, "height-mm"));
        } else {
            if (element.hasAttr("width-mm")) {
                builder.headerWidthMm(number(element, "width-mm"));
            }
            if (element.hasAttr("height-mm")) {
                builder.headerHeightMm(number(element, "height-mm"));
            }
        }
        builder.headerStyle(style).headerHtml(element.html());
    }

    private static double number(Element element, String attribute) {
        return HtmlStyles.number(element.attr(attribute), element);
    }
}
