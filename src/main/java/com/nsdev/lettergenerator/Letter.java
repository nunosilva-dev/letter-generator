package com.nsdev.lettergenerator;

import com.nsdev.lettergenerator.builder.LetterBuilder;
import com.nsdev.lettergenerator.template.TemplateBuilder;

/** Content-first entry point. Defaults to A4, standard margins and readable letter typography. */
public final class Letter {
    /** Use between body strings, or embed a form feed in text, to start a new page. */
    public static final String PAGE_BREAK = "\f";

    private Letter() {}

    public static LetterBuilder builder() {
        return new LetterBuilder();
    }

    public static TemplateBuilder template(String markup) {
        return TemplateBuilder.template(markup);
    }

    public static TemplateBuilder fromHtml(String resolvedMarkup) {
        return TemplateBuilder.resolvedHtml(resolvedMarkup);
    }
}
