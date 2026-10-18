package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import org.jsoup.nodes.Element;

/** Separate top-left logo; dimensions and appearance belong to the nested image. */
final class LogoTagHandler implements TemplateTagHandler {
    private static final double DEFAULT_WIDTH_MM = 45.0;
    private static final double DEFAULT_HEIGHT_MM = 20.0;

    @Override
    public String tagName() {
        return "logo";
    }

    @Override
    public void handle(Element element, TemplateContext context) {
        MarkupSupport.attributes(element);
        MarkupSupport.require(
                element.attr("style").isBlank(), element, "Apply logo styles to its img element");
        context.builder()
                .logo(
                        TemplateImages.read(
                                element,
                                context,
                                context.page().headerHeightMm(),
                                context.page().defaults(),
                                DEFAULT_WIDTH_MM,
                                DEFAULT_HEIGHT_MM));
    }
}
