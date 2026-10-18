package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

/** Immutable registration order controls execution; source order never controls region layout. */
final class TemplateTagRegistry {
    private final Map<String, TemplateTagHandler> handlers;

    TemplateTagRegistry(List<TemplateTagHandler> handlers) {
        var registered = new LinkedHashMap<String, TemplateTagHandler>();
        for (var handler : handlers) {
            if (registered.putIfAbsent(handler.tagName(), handler) != null) {
                throw new IllegalArgumentException(
                        "Duplicate template tag handler: " + handler.tagName());
            }
        }
        this.handlers = Collections.unmodifiableMap(registered);
    }

    static TemplateTagRegistry standard() {
        return new TemplateTagRegistry(
                List.of(
                        new LogoTagHandler(),
                        new MainTagHandler(),
                        new HeaderTagHandler(),
                        new FooterTagHandler(),
                        new SignatureTagHandler(),
                        new WatermarkTagHandler()));
    }

    void apply(Element root, TemplateContext context) {
        Map<String, Element> regions = regions(root);
        for (var handler : handlers.values()) {
            MarkupSupport.require(
                    !handler.required() || regions.containsKey(handler.tagName()),
                    root,
                    "Letter requires one " + handler.tagName() + " element");
        }
        for (var handler : handlers.values()) {
            Element region = regions.get(handler.tagName());
            if (region != null) {
                handler.handle(region, context);
            }
        }
    }

    private Map<String, Element> regions(Element root) {
        var result = new LinkedHashMap<String, Element>();
        for (var node : root.childNodes()) {
            if (node instanceof Comment || node instanceof TextNode text && text.isBlank()) {
                continue;
            }
            MarkupSupport.require(node instanceof Element, node, "Expected a letter region");
            Element element = (Element) node;
            String tag = element.normalName();
            MarkupSupport.require(
                    handlers.containsKey(tag), element, "Unsupported letter region: " + tag);
            MarkupSupport.require(
                    result.putIfAbsent(tag, element) == null,
                    element,
                    "Duplicate letter region: " + tag);
        }
        return result;
    }
}
