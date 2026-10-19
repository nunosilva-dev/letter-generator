package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import java.util.Set;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/** Shared diagnostics and the deliberately small markup attribute contract. */
public final class MarkupSupport {
    /** Prevents construction of this static helper. */
    private MarkupSupport() {}

    /**
     * Produces a diagnostic path from the element or its nearest direct parent element.
     *
     * @param node non-null parsed node
     * @return template-prefixed CSS-selector path, or template.text without an element parent
     */
    public static String path(Node node) {
        return "template."
                + (node instanceof Element element
                        ? element.cssSelector()
                        : node.parent() instanceof Element parent ? parent.cssSelector() : "text");
    }

    /**
     * Constructs but does not throw a markup validation failure.
     *
     * @param node non-null node identifying the error location
     * @param message diagnostic text
     * @return exception containing the template path
     */
    public static InvalidLetterException error(Node node, String message) {
        return new InvalidLetterException(path(node), message);
    }

    /**
     * Enforces a markup rule with a template-local diagnostic.
     *
     * @param condition whether the rule holds
     * @param node non-null location when the rule fails
     * @param message diagnostic on failure
     * @throws InvalidLetterException if condition is false
     */
    public static void require(boolean condition, Node node, String message) {
        if (!condition) {
            throw error(node, message);
        }
    }

    /**
     * Rejects every attribute except style and the caller's explicit element-specific allowlist.
     *
     * @param element non-null parsed element
     * @param additional allowed attribute names, excluding the implicitly allowed style
     * @throws InvalidLetterException if an attribute is unsupported
     * @throws IllegalArgumentException if additional contains duplicate names
     */
    public static void attributes(Element element, String... additional) {
        Set<String> allowed = Set.of(additional);
        element.attributes()
                .forEach(
                        attribute ->
                                require(
                                        attribute.getKey().equals("style")
                                                || attribute.getKey().equals("style-name")
                                                || allowed.contains(attribute.getKey()),
                                        element,
                                        "Unsupported attribute: " + attribute.getKey()));
    }

    /**
     * Rejects structural table text except comments and blank indentation text nodes.
     *
     * @param parent table container whose direct child content is checked
     */
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
