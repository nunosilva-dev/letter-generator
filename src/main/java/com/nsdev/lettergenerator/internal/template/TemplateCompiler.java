package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.builder.LetterBuilder;
import com.nsdev.lettergenerator.internal.markup.HtmlBlocks;
import com.nsdev.lettergenerator.internal.markup.HtmlImages;
import com.nsdev.lettergenerator.internal.markup.HtmlParser;
import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

/** Compiles one full letter template through the ordinary fluent document builder. */
public final class TemplateCompiler {
    private static final double DEFAULT_MARGIN_MM = 20.0;
    private static final double DEFAULT_HEADER_HEIGHT_MM = 25.0;
    private static final double DEFAULT_FOOTER_HEIGHT_MM = 15.0;
    private static final double DEFAULT_SIGNATURE_HEIGHT_MM = 20.0;
    private static final double DEFAULT_LOGO_WIDTH_MM = 45.0;
    private static final double DEFAULT_WATERMARK_WIDTH_MM = 80.0;
    private static final double DEFAULT_WATERMARK_HEIGHT_MM = 40.0;
    private static final Set<String> REGIONS =
            Set.of("main", "header", "footer", "signature", "logo", "watermark");

    private TemplateCompiler() {}

    public static LetterDocument compile(
            String source,
            Map<String, String> values,
            Map<String, byte[]> assets,
            boolean substitute) {
        Element body = HtmlParser.body(source);
        MarkupSupport.require(
                body.childrenSize() == 1 && body.child(0).normalName().equals("letter"),
                body,
                "Expected one letter root element");
        for (var node : body.childNodes()) {
            MarkupSupport.require(
                    node instanceof Element
                            || node instanceof Comment
                            || node instanceof TextNode text && text.isBlank(),
                    node,
                    "Unexpected content outside letter");
        }
        Element root = body.child(0);
        if (substitute) {
            new TemplateBindings(values).apply(root);
        }
        MarkupSupport.attributes(
                root, "page", "margins-mm", "header-height-mm", "footer-height-mm");
        Style defaults = HtmlStyles.text(null, root);
        var builder = new LetterBuilder().style(defaults);
        assets.forEach(builder::asset);
        PageSize size;
        try {
            size =
                    root.hasAttr("page")
                            ? PageSize.valueOf(root.attr("page").toUpperCase(Locale.ROOT))
                            : PageSize.A4;
        } catch (IllegalArgumentException exception) {
            throw MarkupSupport.error(root, "Page must be A4 or LETTER");
        }
        double margins = number(root, "margins-mm", DEFAULT_MARGIN_MM);
        double headerHeight = number(root, "header-height-mm", DEFAULT_HEADER_HEIGHT_MM);
        double footerHeight = number(root, "footer-height-mm", DEFAULT_FOOTER_HEIGHT_MM);
        builder.page(size)
                .marginsMm(margins)
                .headerHeightMm(headerHeight)
                .footerHeightMm(footerHeight);
        double width = size.widthMm() - margins * 2;
        double height = size.heightMm() - margins * 2;
        Map<String, Element> regions = regions(root);
        MarkupSupport.require(
                regions.containsKey("main"), root, "Letter requires one main element");
        if (regions.containsKey("logo")) {
            Element logo = regions.get("logo");
            MarkupSupport.attributes(logo);
            MarkupSupport.require(
                    logo.attr("style").isBlank(), logo, "Apply logo styles to its img element");
            Element image = onlyImage(logo).clone();
            var appearance = HtmlStyles.read(image);
            if (!image.hasAttr("width") && appearance.width() == null) {
                image.attr("width", DEFAULT_LOGO_WIDTH_MM + "mm");
            }
            if (!image.hasAttr("height") && appearance.height() == null) {
                image.attr("height", DEFAULT_SIGNATURE_HEIGHT_MM + "mm");
            }
            var block = HtmlImages.read(image, assets, width, headerHeight, true, defaults);
            builder.logo(block);
        }
        blocks(regions.get("main"), defaults, assets, width, height, true).forEach(builder::block);
        if (regions.containsKey("header")) {
            TemplateHeader.apply(builder, regions.get("header"), defaults);
        }
        if (regions.containsKey("footer")) {
            builder.footerBlocks(
                    blocks(regions.get("footer"), defaults, assets, width, footerHeight, false)
                            .toArray(Block[]::new));
        }
        if (regions.containsKey("signature")) {
            builder.signatureBlocks(
                    blocks(
                                    regions.get("signature"),
                                    defaults,
                                    assets,
                                    width,
                                    DEFAULT_SIGNATURE_HEIGHT_MM,
                                    false)
                            .toArray(Block[]::new));
        }
        if (regions.containsKey("watermark")) {
            builder.watermark(watermark(regions.get("watermark"), assets, width, height));
        }
        return builder.build();
    }

    private static List<Block> blocks(
            Element region,
            Style defaults,
            Map<String, byte[]> assets,
            double width,
            double height,
            boolean body) {
        MarkupSupport.attributes(region);
        Style style = HtmlStyles.text(defaults, region);
        MarkupSupport.require(
                style.spaceBeforeMm() == null && style.spaceAfterMm() == null,
                region,
                "Apply paragraph margins to p/div text blocks");
        return HtmlBlocks.read(region, style, assets, width, height, body);
    }

    private static Map<String, Element> regions(Element root) {
        Map<String, Element> result = new HashMap<>();
        for (var node : root.childNodes()) {
            if (node instanceof Comment || node instanceof TextNode text && text.isBlank()) {
                continue;
            }
            MarkupSupport.require(node instanceof Element, node, "Expected a letter region");
            Element element = (Element) node;
            String tag = element.normalName();
            MarkupSupport.require(
                    REGIONS.contains(tag), element, "Unsupported letter region: " + tag);
            MarkupSupport.require(
                    result.putIfAbsent(tag, element) == null,
                    element,
                    "Duplicate letter region: " + tag);
        }
        return result;
    }

    private static Watermark watermark(
            Element element, Map<String, byte[]> assets, double width, double height) {
        MarkupSupport.attributes(element, "opacity", "rotation", "pages");
        TextWatermark preset = TextWatermark.of(element.text());
        double opacity = number(element, "opacity", preset.opacity());
        double rotation =
                number(
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
            Element image = onlyImage(element).clone();
            var appearance = HtmlStyles.read(image);
            if (!image.hasAttr("width") && appearance.width() == null) {
                image.attr("width", DEFAULT_WATERMARK_WIDTH_MM + "mm");
            }
            if (!image.hasAttr("height") && appearance.height() == null) {
                image.attr("height", DEFAULT_WATERMARK_HEIGHT_MM + "mm");
            }
            var block = HtmlImages.read(image, assets, width, height, true, Style.plain());
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

    private static Element onlyImage(Element region) {
        MarkupSupport.require(
                region.childrenSize() == 1
                        && region.child(0).normalName().equals("img")
                        && region.ownText().isBlank(),
                region,
                "Expected one img element");
        return region.child(0);
    }

    private static double number(Element element, String attribute, double fallback) {
        return element.hasAttr(attribute)
                ? HtmlStyles.number(element.attr(attribute), element)
                : fallback;
    }
}
