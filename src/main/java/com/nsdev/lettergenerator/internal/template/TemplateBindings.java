package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.localization.ValueFormats;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.commons.text.StringSubstitutor;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/** Map-only substitution on parsed values: replacements can never create markup nodes. */
final class TemplateBindings {
    /** Per-traversal map lookup with recursive expansion and defaults disabled. */
    private final StringSubstitutor substitutor;

    /** Owned typed scope for finite directives. */
    private final Map<String, ?> values;

    /** Explicit locale for formatted parameter values. */
    private final LocaleSettings locale;

    /** Optional inspection sink for missing scalar occurrences. */
    private final BiConsumer<String, String> missing;

    /** Current scalar source path, owned by this traversal. */
    private String path;

    /**
     * Configures literal scalar substitution within one immutable typed scope.
     *
     * @param values owned typed bindings
     * @param locale explicit formatting choices
     */
    TemplateBindings(Map<String, ?> values, LocaleSettings locale) {
        this(values, locale, null);
    }

    /**
     * Configures optional aggregation of missing scalar occurrences.
     *
     * @param values owned typed bindings
     * @param locale formatting choices
     * @param missing optional missing-name/path sink
     */
    TemplateBindings(
            Map<String, ?> values, LocaleSettings locale, BiConsumer<String, String> missing) {
        this.values = values;
        this.locale = locale;
        this.missing = missing;
        substitutor =
                new StringSubstitutor(
                                key -> {
                                    Object value = TemplateData.find(values, key);
                                    if (value == null && missing != null) {
                                        missing.accept(key, path);
                                    }
                                    return value == null
                                            ? null
                                            : ValueFormats.format(
                                                    value, "text", null, locale, null);
                                })
                        .setEnableUndefinedVariableException(missing == null)
                        .setDisableSubstitutionInValues(true)
                        .setEnableSubstitutionInVariables(false)
                        .setValueDelimiterMatcher(null);
    }

    /**
     * Binds root attributes once before resolving their locale, then expands content with that
     * locale. Root replacement text is never evaluated again. Inspection can continue with its
     * default locale when missing root bindings prevent locale resolution.
     *
     * @param root parsed letter root to traverse
     * @return bound root formatting choices
     */
    LocaleSettings apply(Element root) {
        var missingRootValues = new HashSet<String>();
        TemplateBindings rootBindings =
                missing == null
                        ? this
                        : new TemplateBindings(
                                values,
                                locale,
                                (key, location) -> {
                                    missingRootValues.add(location);
                                    missing.accept(key, location);
                                });
        rootBindings.scalar(root);
        LocaleSettings resolved;
        try {
            resolved = TemplatePage.locale(root, locale);
        } catch (InvalidLetterException exception) {
            if (missingRootValues.isEmpty()) {
                throw exception;
            }
            resolved = locale;
        }
        new TemplateExpansion(values, resolved, missing).apply(root, false);
        return resolved;
    }

    /**
     * Binds only this node; traversal and directives are handled separately.
     *
     * @param node existing parsed node
     */
    void scalar(Node node) {
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
    }

    /**
     * Keeps CSS property names literal and substitutes each value independently. A resulting
     * semicolon is forbidden so a replacement cannot inject another declaration; full CSS
     * vocabulary/value validation is performed later by HtmlStyles.
     *
     * @param input existing text or attribute value
     * @param element parsed element being read and used for diagnostics
     * @return substituted CSS with literal property names and preserved declaration boundaries
     */
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

    /**
     * Wraps substitution errors with the exact text/attribute/CSS-value location and cause.
     *
     * @param input existing text or attribute value
     * @param path complete diagnostic location for substitution errors
     * @return literal map substitution result
     */
    private String replace(String input, String path) {
        try {
            this.path = path;
            return substitutor.replace(input);
        } catch (IllegalArgumentException exception) {
            throw new InvalidLetterException(path, exception.getMessage(), exception);
        }
    }
}
