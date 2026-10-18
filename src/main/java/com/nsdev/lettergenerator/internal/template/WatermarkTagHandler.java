package com.nsdev.lettergenerator.internal.template;

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
    private static final double DEFAULT_WIDTH_MM = 80.0;
    private static final double DEFAULT_HEIGHT_MM = 40.0;

    @Override
    public String tagName() {
        return "watermark";
    }

    @Override
    public void handle(Element element, TemplateContext context) {
        context.builder().watermark(read(element, context));
    }

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
                            DEFAULT_WIDTH_MM,
                            DEFAULT_HEIGHT_MM);
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
