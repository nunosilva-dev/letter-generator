package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import java.util.Set;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/** Shared diagnostics and the deliberately small markup attribute contract. */
public final class MarkupSupport {
    private MarkupSupport() {}

    public static String path(Node node) {
        return "template."
                + (node instanceof Element element
                        ? element.cssSelector()
                        : node.parent() instanceof Element parent ? parent.cssSelector() : "text");
    }

    public static InvalidLetterException error(Node node, String message) {
        return new InvalidLetterException(path(node), message);
    }

    public static void require(boolean condition, Node node, String message) {
        if (!condition) {
            throw error(node, message);
        }
    }

    public static void attributes(Element element, String... additional) {
        Set<String> allowed = Set.of(additional);
        element.attributes()
                .forEach(
                        attribute ->
                                require(
                                        attribute.getKey().equals("style")
                                                || allowed.contains(attribute.getKey()),
                                        element,
                                        "Unsupported attribute: " + attribute.getKey()));
    }

    static void elementOnlyContent(Element parent) {
        for (Node node : parent.childNodes()) {
            require(
                    node instanceof Element
                            || node instanceof Comment
                            || node instanceof TextNode text && text.isBlank(),
                    node,
                    "Table structure requires elements; place text inside td or th");
        }
    }
}
