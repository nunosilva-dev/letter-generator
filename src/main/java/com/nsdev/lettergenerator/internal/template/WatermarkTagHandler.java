package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.defaults.WatermarkDefaults;
import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import java.util.Locale;
import org.jsoup.nodes.Element;

/** Text/image watermark appearance and page selection, independent of body layout. */
final class WatermarkTagHandler implements TemplateTagHandler {
    @Override
    public String tagName() {
        return "watermark";
    }

    /**
     * Stores a watermark independently of body layout; the renderer applies it after pagination
     * according to its page selector.
     */
    @Override
    public void handle(Element element, TemplateContext context) {
        context.builder().watermark(read(element, context));
    }

    /**
     * Chooses an image watermark when children exist, requiring one img with its own styles and an
     * 80 by 40 mm default box. Otherwise reads plain text with watermark-specific defaults; root
     * text style is not inherited and bold/italic/underline are forbidden. Opacity and rotation are
     * numeric overrides (rotation in degrees); shared validation checks their range.
     *
     * @param element parsed element being read and used for diagnostics
     * @param context per-compilation builder, page defaults and named assets
     * @return text or image watermark with its own appearance and page selector
     */
    private static Watermark read(Element element, TemplateContext context) {
        MarkupSupport.attributes(element, "opacity", "rotation", "pages");
        TextWatermark preset = TextWatermark.of(element.text());
        double opacity = TemplateAttributes.number(element, "opacity", preset.opacity());
        double rotation =
                TemplateAttributes.number(
                        element,
                        "rotation",
                        element.children().isEmpty() ? preset.rotationDegrees() : 0);
        PageSelector pages;
        try {
            pages =
                    element.hasAttr("pages")
                            ? PageSelector.valueOf(element.attr("pages").toUpperCase(Locale.ROOT))
                            : PageSelector.ALL;
        } catch (IllegalArgumentException exception) {
            throw MarkupSupport.error(element, "Unsupported watermark page selector");
        }
        if (!element.children().isEmpty()) {
            MarkupSupport.require(
                    element.attr("style").isBlank(),
                    element,
                    "Apply image watermark styles to img");
            var block =
                    TemplateImages.read(
                            element,
                            context,
                            context.page().heightMm(),
                            Style.plain(),
                            WatermarkDefaults.IMAGE_MAX_WIDTH_MM,
                            WatermarkDefaults.IMAGE_MAX_HEIGHT_MM);
            return new ImageWatermark(
                    block.data(),
                    block.mimeType(),
                    block.widthMm(),
                    block.heightMm(),
                    pages,
                    opacity,
                    rotation);
        }
        Style style = HtmlStyles.text(null, element);
        MarkupSupport.require(
                !Boolean.TRUE.equals(style.bold())
                        && !Boolean.TRUE.equals(style.italic())
                        && !Boolean.TRUE.equals(style.underline()),
                element,
                "Text watermarks use regular undecorated fonts");
        return new TextWatermark(
                element.text(),
                style.fontFamily() == null ? preset.fontFamily() : style.fontFamily(),
                style.fontSizePt() == null ? preset.fontSizePt() : style.fontSizePt(),
                style.color() == null ? preset.color() : style.color(),
                pages,
                opacity,
                rotation);
    }
}
