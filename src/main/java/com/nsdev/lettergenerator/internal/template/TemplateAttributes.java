package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import java.util.Locale;
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

    /**
     * Reads the first/following/all stationery selector, defaulting to all pages.
     *
     * @param element region carrying an optional pages attribute
     * @return supported physical page selection
     */
    static PageSelector pages(Element element) {
        try {
            return element.hasAttr("pages")
                    ? PageSelector.valueOf(element.attr("pages").toUpperCase(Locale.ROOT))
                    : PageSelector.ALL;
        } catch (IllegalArgumentException exception) {
            throw MarkupSupport.error(element, "Pages must be all, first or following");
        }
    }
}
