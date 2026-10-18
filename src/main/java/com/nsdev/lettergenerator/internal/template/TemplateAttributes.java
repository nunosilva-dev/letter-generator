package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import org.jsoup.nodes.Element;

/** Optional numeric attributes retain the common strict markup diagnostics. */
final class TemplateAttributes {
    private TemplateAttributes() {}

    static double number(Element element, String attribute, double fallback) {
        return element.hasAttr(attribute)
                ? HtmlStyles.number(element.attr(attribute), element)
                : fallback;
    }
}
