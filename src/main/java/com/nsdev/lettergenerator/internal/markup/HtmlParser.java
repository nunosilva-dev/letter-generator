package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

/** Rejects HTML recovery that would otherwise discard or relocate caller-supplied content. */
public final class HtmlParser {
    private HtmlParser() {}

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
