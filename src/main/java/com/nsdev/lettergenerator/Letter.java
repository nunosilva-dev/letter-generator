package com.nsdev.lettergenerator;

import com.nsdev.lettergenerator.builder.HtmlLetterBuilder;
import com.nsdev.lettergenerator.builder.LetterBuilder;

/** Content-first entry point. Defaults to A4, standard margins and readable letter typography. */
public final class Letter {
    /** Use between body strings, or embed a form feed in text, to start a new page. */
    public static final String PAGE_BREAK = "\f";

    private Letter() {}

    public static LetterBuilder builder() {
        return new LetterBuilder();
    }

    public static HtmlLetterBuilder template(String markup) {
        return HtmlLetterBuilder.template(markup);
    }

    public static HtmlLetterBuilder fromHtml(String resolvedMarkup) {
        return HtmlLetterBuilder.resolvedHtml(resolvedMarkup);
    }
}
