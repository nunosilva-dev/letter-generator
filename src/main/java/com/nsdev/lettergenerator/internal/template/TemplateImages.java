package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.HtmlImages;
import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.style.Style;
import org.jsoup.nodes.Element;

/** Resolves a region containing one named image, preserving explicit dimensions and styles. */
final class TemplateImages {
    private TemplateImages() {}

    static ImageBlock read(
            Element region,
            TemplateContext context,
            double heightMm,
            Style defaults,
            double defaultWidthMm,
            double defaultHeightMm) {
        MarkupSupport.require(
                region.childrenSize() == 1
                        && region.child(0).normalName().equals("img")
                        && region.ownText().isBlank(),
                region,
                "Expected one img element");
        Element image = region.child(0).clone();
        var appearance = HtmlStyles.read(image);
        if (!image.hasAttr("width") && appearance.width() == null) {
            image.attr("width", defaultWidthMm + "mm");
        }
        if (!image.hasAttr("height") && appearance.height() == null) {
            image.attr("height", defaultHeightMm + "mm");
        }
        return HtmlImages.read(
                image, context.assets(), context.page().widthMm(), heightMm, true, defaults);
    }
}
