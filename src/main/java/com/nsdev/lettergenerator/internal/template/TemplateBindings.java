package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/** Map-only substitution on parsed values: replacements can never create markup nodes. */
final class TemplateBindings {
    private final StringSubstitutor substitutor;

    TemplateBindings(Map<String, String> values) {
        substitutor =
                new StringSubstitutor(values)
                        .setEnableUndefinedVariableException(true)
                        .setDisableSubstitutionInValues(true)
                        .setEnableSubstitutionInVariables(false)
                        .setValueDelimiterMatcher(null);
    }

    void apply(Node node) {
        if (node instanceof TextNode text) {
            text.text(replace(text.getWholeText(), MarkupSupport.path(node)));
        } else if (node instanceof Element element) {
            element.attributes()
                    .forEach(
                            attribute ->
                                    attribute.setValue(
                                            attribute.getKey().equals("style")
                                                    ? style(attribute.getValue(), element)
                                                    : replace(
                                                            attribute.getValue(),
                                                            MarkupSupport.path(element)
                                                                    + "@"
                                                                    + attribute.getKey())));
        }
        for (Node child : node.childNodes()) {
            apply(child);
        }
    }

    private String style(String input, Element element) {
        var result = new StringBuilder();
        for (String declaration : input.split(";")) {
            if (declaration.isBlank()) {
                continue;
            }
            int colon = declaration.indexOf(':');
            MarkupSupport.require(colon > 0, element, "Invalid CSS declaration: " + declaration);
            String property = declaration.substring(0, colon);
            MarkupSupport.require(
                    !property.contains("${"), element, "CSS property names must be literal");
            String value =
                    replace(
                            declaration.substring(colon + 1),
                            MarkupSupport.path(element) + "@style." + property.trim());
            MarkupSupport.require(
                    !value.contains(";"),
                    element,
                    "A substituted CSS value cannot introduce another declaration");
            result.append(property).append(':').append(value).append(';');
        }
        return result.toString();
    }

    private String replace(String input, String path) {
        try {
            return substitutor.replace(input);
        } catch (IllegalArgumentException exception) {
            throw new InvalidLetterException(path, exception.getMessage(), exception);
        }
    }
}
