package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.builder.LetterBuilder;
import com.nsdev.lettergenerator.internal.defaults.LetterLayoutDefaults;
import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.Orientation;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.style.Style;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.jsoup.nodes.Element;

/**
 * Root geometry shared by region handlers; inner dimensions exclude physical margins.
 *
 * @param size named paper dimensions
 * @param marginsMm uniform margins
 * @param headerHeightMm header region height
 * @param footerHeightMm footer region height
 * @param defaults root text style
 * @param paperWidthMm optional custom paper width
 * @param paperHeightMm optional custom paper height
 * @param orientation selected paper orientation
 */
record TemplatePage(
        PageSize size,
        double marginsMm,
        double headerHeightMm,
        double footerHeightMm,
        Style defaults,
        Double paperWidthMm,
        Double paperHeightMm,
        Orientation orientation) {
    /**
     * Reads allowlisted root geometry, locale and typography options.
     *
     * @param root parsed letter root
     * @return typed geometry
     */
    static TemplatePage read(Element root) {
        MarkupSupport.attributes(
                root,
                "page",
                "margins-mm",
                "header-height-mm",
                "footer-height-mm",
                "page-width-mm",
                "page-height-mm",
                "orientation",
                "language-tag",
                "date-pattern",
                "date-style",
                "currency",
                "zone-id");
        Style defaults = HtmlStyles.text(null, root);
        PageSize size;
        Orientation orientation;
        try {
            size =
                    root.hasAttr("page")
                            ? PageSize.valueOf(root.attr("page").toUpperCase(Locale.ROOT))
                            : PageSize.A4;
            orientation =
                    root.hasAttr("orientation")
                            ? Orientation.valueOf(root.attr("orientation").toUpperCase(Locale.ROOT))
                            : Orientation.PORTRAIT;
        } catch (IllegalArgumentException exception) {
            throw MarkupSupport.error(root, "Invalid page name or orientation");
        }
        MarkupSupport.require(
                root.hasAttr("page-width-mm") == root.hasAttr("page-height-mm"),
                root,
                "Custom page requires both width and height");
        Double width =
                root.hasAttr("page-width-mm")
                        ? TemplateAttributes.number(root, "page-width-mm", 0)
                        : null;
        Double height =
                root.hasAttr("page-height-mm")
                        ? TemplateAttributes.number(root, "page-height-mm", 0)
                        : null;
        MarkupSupport.require(
                width == null || width > 0 && height > 0,
                root,
                "Custom page dimensions must be positive");
        return new TemplatePage(
                size,
                TemplateAttributes.number(root, "margins-mm", Margins.defaults().top()),
                TemplateAttributes.number(
                        root, "header-height-mm", LetterLayoutDefaults.HEADER_HEIGHT_MM),
                TemplateAttributes.number(
                        root, "footer-height-mm", LetterLayoutDefaults.FOOTER_HEIGHT_MM),
                defaults,
                width,
                height,
                orientation);
    }

    /**
     * Resolves root formatting options above optional builder settings.
     *
     * @param root letter root
     * @param defaults optional explicit default locale
     * @return deterministic formatting settings
     */
    static LocaleSettings locale(Element root, LocaleSettings defaults) {
        LocaleSettings base = defaults == null ? LocaleSettings.defaults() : defaults;
        try {
            return new LocaleSettings(
                    root.hasAttr("language-tag") ? root.attr("language-tag") : base.languageTag(),
                    root.hasAttr("date-pattern") ? root.attr("date-pattern") : base.datePattern(),
                    root.hasAttr("date-style")
                            ? FormatStyle.valueOf(root.attr("date-style").toUpperCase(Locale.ROOT))
                            : base.dateStyle(),
                    root.hasAttr("currency") ? root.attr("currency") : base.currencyCode(),
                    root.hasAttr("zone-id") ? root.attr("zone-id") : base.zoneId());
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            throw MarkupSupport.error(root, "Invalid locale: " + exception.getMessage());
        }
    }

    /**
     * Detects explicit root locale configuration.
     *
     * @param root parsed letter root
     * @return whether any formatting option is supplied
     */
    static boolean hasLocale(Element root) {
        return root.hasAttr("language-tag")
                || root.hasAttr("date-pattern")
                || root.hasAttr("date-style")
                || root.hasAttr("currency")
                || root.hasAttr("zone-id");
    }

    /**
     * @return a fresh builder initialized with final paper geometry
     */
    LetterBuilder builder() {
        var builder =
                new LetterBuilder()
                        .style(defaults)
                        .page(size)
                        .marginsMm(marginsMm)
                        .headerHeightMm(headerHeightMm)
                        .footerHeightMm(footerHeightMm);
        if (paperWidthMm != null) {
            builder.pageSizeMm(paperWidthMm, paperHeightMm);
        }
        return builder.orientation(orientation);
    }

    /**
     * @return inner oriented page width in millimeters
     */
    double widthMm() {
        double width = paperWidthMm == null ? size.widthMm() : paperWidthMm;
        double height = paperHeightMm == null ? size.heightMm() : paperHeightMm;
        return (orientation == Orientation.LANDSCAPE
                        ? Math.max(width, height)
                        : Math.min(width, height))
                - marginsMm * 2;
    }

    /**
     * @return inner oriented page height in millimeters
     */
    double heightMm() {
        double width = paperWidthMm == null ? size.widthMm() : paperWidthMm;
        double height = paperHeightMm == null ? size.heightMm() : paperHeightMm;
        return (orientation == Orientation.LANDSCAPE
                        ? Math.min(width, height)
                        : Math.max(width, height))
                - marginsMm * 2;
    }
}
