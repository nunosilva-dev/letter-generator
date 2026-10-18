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
    private TemplateBlocks() {}

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
                region, style, context.assets(), context.page().widthMm(), heightMm, body);
    }
}
