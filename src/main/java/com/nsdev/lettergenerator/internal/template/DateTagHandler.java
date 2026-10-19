package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.HtmlStyles;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.letter.LetterElement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.jsoup.nodes.Element;

/**
 * Defers ISO date values to the builder's final locale or accepts explicitly authored rich text.
 */
final class DateTagHandler implements TemplateTagHandler {
    @Override
    public String tagName() {
        return "date";
    }

    @Override
    public void handle(Element region, TemplateContext context) {
        if (!region.hasAttr("value")) {
            context.builder()
                    .part(
                            LetterElement.DATE,
                            TemplateBlocks.read(region, context, context.page().heightMm(), false));
            return;
        }
        MarkupSupport.attributes(region, "value");
        MarkupSupport.require(
                region.childrenSize() == 0 && region.text().isBlank(),
                region,
                "Date value cannot also contain rich content");
        try {
            context.builder()
                    .date(
                            LocalDate.parse(region.attr("value")),
                            HtmlStyles.text(context.page().defaults(), region));
        } catch (DateTimeParseException exception) {
            throw MarkupSupport.error(region, "Date value must be an ISO local date");
        }
    }
}
