package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import org.jsoup.nodes.Element;

/** Optional numeric attributes retain the common strict markup diagnostics. */
final class TemplateAttributes {
    /** Prevents construction of this static helper. */
    private TemplateAttributes() {}

    /**
     * Reads a finite numeric attribute or preserves its fallback, in the caller's chosen units.
     *
     * @param element parsed element being read and used for diagnostics
     * @param attribute attribute name to read
     * @param fallback finite fallback returned when the attribute is absent
     * @return parsed finite number or the fallback
     */
    static double number(Element element, String attribute, double fallback) {
        return element.hasAttr(attribute)
                ? HtmlStyles.number(element.attr(attribute), element)
                : fallback;
    }
}
