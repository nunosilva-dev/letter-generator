package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import org.jsoup.nodes.Element;

/** Separate top-left logo; dimensions and appearance belong to the nested image. */
final class LogoTagHandler implements TemplateTagHandler {
    /** Fallback image fitting width in millimeters. */
    private static final double DEFAULT_WIDTH_MM = 45.0;

    /** Fallback image fitting height in millimeters. */
    private static final double DEFAULT_HEIGHT_MM = 20.0;

    @Override
    public String tagName() {
        return "logo";
    }

    /**
     * Stores one named image, defaulting its fitting box to 45 by 20 mm. The logo wrapper cannot
     * supply appearance; style and dimensions belong to its img child.
     */
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
