package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.letter.LetterElement;
import org.jsoup.nodes.Element;

/** Adds a literal bold reference label to authored rich reference content. */
final class ReferenceTagHandler implements TemplateTagHandler {
    @Override
    public String tagName() {
        return "reference";
    }

    @Override
    public void handle(Element region, TemplateContext context) {
        MarkupSupport.attributes(region, "label");
        String label = region.attr("label");
        MarkupSupport.require(!label.isBlank(), region, "Reference label must not be blank");
        MarkupSupport.require(
                !region.text().isBlank() || region.childrenSize() > 0,
                region,
                "Reference needs content");
        var prefix = new Element("strong").text(label + ": ");
        Element first = region.childrenSize() == 0 ? null : region.child(0);
        if (first != null && first.normalName().equals("p")) {
            first.prependChild(prefix);
        } else {
            region.prependChild(prefix);
        }
        context.builder()
                .part(
                        LetterElement.REFERENCE,
                        TemplateBlocks.read(
                                region, context, context.page().heightMm(), false, "label"));
    }
}
