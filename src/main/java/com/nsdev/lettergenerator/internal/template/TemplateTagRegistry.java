package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.letter.LetterElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

/** Immutable registration order controls execution; source order never controls region layout. */
final class TemplateTagRegistry {
    /** Immutable insertion-ordered handlers defining compilation execution order. */
    private final Map<String, TemplateTagHandler> handlers;

    /**
     * Snapshots handlers in execution order and rejects duplicate tag names at construction.
     *
     * @param handlers stateless handlers in desired execution order
     */
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

    /**
     * Defines logo, main, header, footer, signature, watermark execution order. Logo precedes
     * header so deferred header placement sees its width; source region order has no effect.
     *
     * @return immutable standard handler registry
     */
    static TemplateTagRegistry standard() {
        return new TemplateTagRegistry(
                List.of(
                        new LogoTagHandler(),
                        new LetterPartTagHandler(LetterElement.SENDER),
                        new DateTagHandler(),
                        new LetterPartTagHandler(LetterElement.RECIPIENT),
                        new ReferenceTagHandler(),
                        new LetterPartTagHandler(LetterElement.SUBJECT),
                        new LetterPartTagHandler(LetterElement.SALUTATION),
                        new LetterPartTagHandler(LetterElement.CLOSING),
                        new MainTagHandler(),
                        new HeaderTagHandler(),
                        new FooterTagHandler(),
                        new SignatureTagHandler(),
                        new WatermarkTagHandler()));
    }

    /**
     * Collects and validates region names/uniqueness and checks all required regions before
     * executing any handler. Each present region is then compiled in registration order.
     *
     * @param root parsed letter root
     * @param context per-compilation builder, page defaults and named assets
     */
    void apply(Element root, TemplateContext context) {
        Map<String, List<Element>> regions = regions(root);
        for (var handler : handlers.values()) {
            MarkupSupport.require(
                    !handler.required() || regions.containsKey(handler.tagName()),
                    root,
                    "Letter requires one " + handler.tagName() + " element");
        }
        for (var handler : handlers.values()) {
            for (Element region : regions.getOrDefault(handler.tagName(), List.of())) {
                handler.handle(region, context);
            }
        }
    }

    /**
     * Indexes immediate children by normalized tag; only comments and blank text are skipped.
     * Unknown tags, non-element content and duplicate regions fail with their source-node path.
     *
     * @param root parsed letter root
     * @return unique supported regions indexed by tag name
     */
    private Map<String, List<Element>> regions(Element root) {
        var result = new LinkedHashMap<String, List<Element>>();
        var identities = new HashSet<String>();
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
                    identities.add(tag + ":" + handlers.get(tag).regionKey(element)),
                    element,
                    "Duplicate letter region: " + tag);
            result.computeIfAbsent(tag, ignored -> new ArrayList<>()).add(element);
        }
        return result;
    }
}
