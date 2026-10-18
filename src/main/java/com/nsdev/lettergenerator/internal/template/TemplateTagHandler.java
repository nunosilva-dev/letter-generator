package com.nsdev.lettergenerator.internal.template;

import org.jsoup.nodes.Element;

/** Stateless compiler for one top-level region inside a letter template. */
interface TemplateTagHandler {
    String tagName();

    default boolean required() {
        return false;
    }

    void handle(Element element, TemplateContext context);
}
