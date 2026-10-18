package com.nsdev.lettergenerator.internal.template;

import org.jsoup.nodes.Element;

/** Required body region; its blocks retain their input order and may contain page breaks. */
final class MainTagHandler implements TemplateTagHandler {
    @Override
    public String tagName() {
        return "main";
    }

    @Override
    public boolean required() {
        return true;
    }

    @Override
    public void handle(Element element, TemplateContext context) {
        TemplateBlocks.read(element, context, context.page().heightMm(), true)
                .forEach(context.builder()::block);
    }
}
