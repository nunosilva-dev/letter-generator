package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.model.block.Block;
import org.jsoup.nodes.Element;

/** Repeated footer content, sized against the root's configured footer height. */
final class FooterTagHandler implements TemplateTagHandler {
    @Override
    public String tagName() {
        return "footer";
    }

    /** {@inheritDoc} */
    @Override
    public String regionKey(Element element) {
        return tagName() + ":" + TemplateAttributes.pages(element);
    }

    /**
     * Replaces repeated footer blocks, inheriting root/region style and using the configured footer
     * height as the image fitting base. Page breaks are rejected.
     */
    @Override
    public void handle(Element element, TemplateContext context) {
        context.builder()
                .footerBlocks(
                        TemplateAttributes.pages(element),
                        TemplateBlocks.read(
                                        element,
                                        context,
                                        context.page().footerHeightMm(),
                                        false,
                                        "pages")
                                .toArray(Block[]::new));
    }
}
