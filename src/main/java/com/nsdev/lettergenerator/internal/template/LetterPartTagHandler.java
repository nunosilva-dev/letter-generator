package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.letter.LetterElement;
import java.util.Locale;
import org.jsoup.nodes.Element;

/** Compiles one rich semantic letter role into the shared ordinary block model. */
final class LetterPartTagHandler implements TemplateTagHandler {
    /** Stable semantic role handled by this immutable instance. */
    private final LetterElement element;

    /**
     * Captures one role without mutable per-document state.
     *
     * @param element sender, recipient, subject, salutation or closing
     */
    LetterPartTagHandler(LetterElement element) {
        this.element = element;
    }

    @Override
    public String tagName() {
        return element.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public void handle(Element region, TemplateContext context) {
        context.builder()
                .part(
                        element,
                        TemplateBlocks.read(region, context, context.page().heightMm(), false));
    }
}
