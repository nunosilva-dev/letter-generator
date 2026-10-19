package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.defaults.LetterLayoutDefaults;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import org.jsoup.nodes.Element;

/** Separate top-left logo; dimensions and appearance belong to the nested image. */
final class LogoTagHandler implements TemplateTagHandler {
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
                element.attr("style").isBlank() && !element.hasAttr("style-name"),
                element,
                "Apply logo styles to its img element");
        context.builder()
                .logo(
                        TemplateImages.read(
                                element,
                                context,
                                context.page().headerHeightMm(),
                                context.page().defaults(),
                                LetterLayoutDefaults.LOGO_MAX_WIDTH_MM,
                                LetterLayoutDefaults.LOGO_MAX_HEIGHT_MM));
    }
}
