package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.HtmlImages;
import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.style.Style;
import org.jsoup.nodes.Element;

/** Resolves a region containing one named image, preserving explicit dimensions and styles. */
final class TemplateImages {
    /** Prevents construction of this static helper. */
    private TemplateImages() {}

    /**
     * Requires exactly one img child and no nonblank own text. Clones the image before inserting
     * missing millimeter defaults, preserving the parsed template tree. Explicit CSS/attributes
     * take precedence; HtmlImages resolves only supplied asset bytes and preserves aspect ratio.
     * The supplied height is the image percentage/default fitting base in millimeters.
     *
     * @param region parsed region element
     * @param context per-compilation builder, page defaults and named assets
     * @param heightMm region image fitting/percentage height in millimeters
     * @param defaults inherited text defaults for the image
     * @param defaultWidthMm fallback fitting width in millimeters
     * @param defaultHeightMm fallback fitting height in millimeters
     * @return aspect-ratio-fitted named image with appearance overrides
     */
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
