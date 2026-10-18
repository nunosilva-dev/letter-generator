package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.builder.LetterBuilder;
import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.Locale;
import org.jsoup.nodes.Element;

/**
 * Root options shared by region handlers. Inner dimensions exclude physical margins, but do not
 * subtract header/footer body reservations; final builder geometry resolves those reservations.
 *
 * @param size selected paper dimensions
 * @param marginsMm uniform physical margins in millimeters
 * @param headerHeightMm configured header height in millimeters
 * @param footerHeightMm configured footer height in millimeters
 * @param defaults root text overrides inherited by region markup
 */
record TemplatePage(
        PageSize size,
        double marginsMm,
        double headerHeightMm,
        double footerHeightMm,
        Style defaults) {
    /** Default uniform physical margin in millimeters. */
    private static final double DEFAULT_MARGIN_MM = 20.0;

    /** Default header reservation height in millimeters. */
    private static final double DEFAULT_HEADER_HEIGHT_MM = 25.0;

    /** Default footer reservation height in millimeters. */
    private static final double DEFAULT_FOOTER_HEIGHT_MM = 15.0;

    /**
     * Reads the root allowlist, A4/LETTER paper name, numeric millimeter options and text style.
     * Missing options default to A4, 20 mm margins, 25 mm header and 15 mm footer. Numeric parsing
     * rejects non-finite values here; builder argument checks enforce valid geometry afterward.
     *
     * @param root parsed letter root
     * @return typed root options and text defaults
     */
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

    /**
     * Creates the per-call builder and applies root defaults before any region handler runs.
     *
     * @return fresh mutable builder initialized with the root options
     */
    LetterBuilder builder() {
        return new LetterBuilder()
                .style(defaults)
                .page(size)
                .marginsMm(marginsMm)
                .headerHeightMm(headerHeightMm)
                .footerHeightMm(footerHeightMm);
    }

    /**
     * Returns paper width inside uniform left/right margins, in millimeters.
     *
     * @return inner-page width in millimeters
     */
    double widthMm() {
        return size.widthMm() - marginsMm * 2;
    }

    /**
     * Returns paper height inside uniform top/bottom margins, in millimeters.
     *
     * @return inner-page height in millimeters
     */
    double heightMm() {
        return size.heightMm() - marginsMm * 2;
    }
}
