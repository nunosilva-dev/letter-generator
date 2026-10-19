package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.HtmlBlocks;
import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;
import org.jsoup.nodes.Element;

/** Shared region inheritance and block validation; individual handlers own their attributes. */
final class TemplateBlocks {
    /** Prevents construction of this static helper. */
    private TemplateBlocks() {}

    /**
     * Reads region text style over root defaults, rejecting region-level paragraph margins.
     * Delegates child compilation to HtmlBlocks using inner-page width and the caller's region
     * height in millimeters; body alone permits page-break blocks. The extra attribute names are
     * the owning handler's allowlist in addition to style.
     *
     * @param region parsed region element
     * @param context per-compilation builder, page defaults and named assets
     * @param heightMm region image fitting/percentage height in millimeters
     * @param body whether body-only page breaks and body image defaults are allowed
     * @param attributes additional allowed region attribute names
     * @return immutable ordered region blocks
     */
    static List<Block> read(
            Element region,
            TemplateContext context,
            double heightMm,
            boolean body,
            String... attributes) {
        MarkupSupport.attributes(region, attributes);
        Style style = HtmlStyles.text(context.page().defaults(), region);
        MarkupSupport.require(
                style.spaceBeforeMm() == null && style.spaceAfterMm() == null,
                region,
                "Apply paragraph margins to p/div text blocks");
        return HtmlBlocks.read(
                region,
                style,
                context.assets(),
                context.page().widthMm(),
                heightMm,
                body,
                context.theme());
    }
}
