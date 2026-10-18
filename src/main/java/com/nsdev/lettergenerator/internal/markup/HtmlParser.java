package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

/** Parses HTML fragments and rejects reported parse errors before model compilation. */
public final class HtmlParser {
    /** Prevents construction of this static helper. */
    private HtmlParser() {}

    /**
     * Parses into a synthetic HTML body and rejects the first error reported by jsoup's HTML
     * parser. Accepted HTML normalization still occurs; this is not XML well-formedness checking.
     * The returned mutable tree is owned by this call and may be bound by the template compiler.
     *
     * @param source non-null fragment text, possibly empty
     * @return synthetic body containing the parsed nodes
     * @throws IllegalArgumentException if source is null
     * @throws InvalidLetterException if the parser reports a markup error
     */
    public static Element body(String source) {
        if (source == null) {
            throw new IllegalArgumentException("Markup must not be null");
        }
        var parser = Parser.htmlParser().setTrackErrors(1);
        Element body = Document.createShell("").body();
        var nodes = parser.parseFragmentInput(source, body, "");
        if (!parser.getErrors().isEmpty()) {
            var error = parser.getErrors().get(0);
            throw new InvalidLetterException(
                    "template.source[" + error.getPosition() + "]",
                    "Invalid markup at " + error.getCursorPos() + ": " + error.getErrorMessage());
        }
        body.insertChildren(0, nodes);
        return body;
    }
}
