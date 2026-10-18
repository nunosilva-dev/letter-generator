package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.builder.LetterBuilder;
import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.Locale;
import org.jsoup.nodes.Element;

/** Typed root options and the inner-page dimensions shared by region handlers. */
record TemplatePage(
        PageSize size,
        double marginsMm,
        double headerHeightMm,
        double footerHeightMm,
        Style defaults) {
    private static final double DEFAULT_MARGIN_MM = 20.0;
    private static final double DEFAULT_HEADER_HEIGHT_MM = 25.0;
    private static final double DEFAULT_FOOTER_HEIGHT_MM = 15.0;

    static TemplatePage read(Element root) {
        MarkupSupport.attributes(
                root, "page", "margins-mm", "header-height-mm", "footer-height-mm");
        Style defaults = HtmlStyles.text(null, root);
        PageSize size;
        try {
            size =
                    root.hasAttr("page")
                            ? PageSize.valueOf(root.attr("page").toUpperCase(Locale.ROOT))
                            : PageSize.A4;
        } catch (IllegalArgumentException exception) {
            throw MarkupSupport.error(root, "Page must be A4 or LETTER");
        }
        return new TemplatePage(
                size,
                TemplateAttributes.number(root, "margins-mm", DEFAULT_MARGIN_MM),
                TemplateAttributes.number(root, "header-height-mm", DEFAULT_HEADER_HEIGHT_MM),
                TemplateAttributes.number(root, "footer-height-mm", DEFAULT_FOOTER_HEIGHT_MM),
                defaults);
    }

    LetterBuilder builder() {
        return new LetterBuilder()
                .style(defaults)
                .page(size)
                .marginsMm(marginsMm)
                .headerHeightMm(headerHeightMm)
                .footerHeightMm(footerHeightMm);
    }

    double widthMm() {
        return size.widthMm() - marginsMm * 2;
    }

    double heightMm() {
        return size.heightMm() - marginsMm * 2;
    }
}
