package com.nsdev.lettergenerator.internal.template;

import org.jsoup.nodes.Element;

/**
 * Internal stateless compiler for one top-level region. The registry owns reusable instances;
 * implementations must keep all mutable compilation state in the supplied context. Package
 * visibility intentionally keeps this dispatch mechanism out of the public extension API.
 */
interface TemplateTagHandler {
    /**
     * @return unique normalized region name recognized by the registry
     */
    String tagName();

    /**
     * @return whether registry validation must find this region before executing any handler
     */
    default boolean required() {
        return false;
    }

    /**
     * Identifies one region instance for duplicate detection; ordinary tags remain unique.
     *
     * @param element parsed region being indexed
     * @return stable identity within this handler's region kind
     */
    default String regionKey(Element element) {
        return tagName();
    }

    /**
     * Compiles a validated unique region into the current builder.
     *
     * @param element source region; handler owns validation of its attributes and content
     * @param context per-call builder, page defaults and named assets
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if region input fails
     */
    void handle(Element element, TemplateContext context);
}
