package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.model.block.Block;
import org.jsoup.nodes.Element;

/** Repeated footer content, sized against the root's configured footer height. */
final class FooterTagHandler implements TemplateTagHandler {
    @Override
    public String tagName() {
        return "footer";
    }

    @Override
    public void handle(Element element, TemplateContext context) {
        context.builder()
                .footerBlocks(
                        TemplateBlocks.read(
                                        element, context, context.page().footerHeightMm(), false)
                                .toArray(Block[]::new));
    }
}
