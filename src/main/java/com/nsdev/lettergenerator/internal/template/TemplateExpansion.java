package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.localization.ValueFormats;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/** Expands trusted template structure while inserting bound values as literal text. */
final class TemplateExpansion {
    /** Current root or repeat scope. */
    private final Map<String, ?> values;

    /** Explicit locale for param tags. */
    private final LocaleSettings locale;

    /** Literal substitution for this scope. */
    private final TemplateBindings bindings;

    /** Optional aggregate scalar inspection sink. */
    private final BiConsumer<String, String> missing;

    /**
     * Creates one scope with optional scalar diagnostic aggregation.
     *
     * @param values owned scope
     * @param locale formatting choices
     * @param missing optional missing-name/path sink
     */
    TemplateExpansion(
            Map<String, ?> values, LocaleSettings locale, BiConsumer<String, String> missing) {
        this.values = values;
        this.locale = locale;
        this.missing = missing;
        bindings = new TemplateBindings(values, locale, missing);
    }

    /**
     * Applies structural directives before visiting ordinary parsed scalar values.
     *
     * @param node trusted parsed node
     */
    void apply(Node node) {
        apply(node, true);
    }

    /**
     * Expands one node, optionally retaining root attributes that were already bound for locale
     * resolution. Descendants always receive their own single scalar substitution pass.
     *
     * @param node trusted parsed node
     * @param bindScalar whether this node's scalar values still require binding
     */
    void apply(Node node, boolean bindScalar) {
        if (node instanceof Element element) {
            if (element.hasAttr("data-each")) {
                repeat(element);
                return;
            }
            MarkupSupport.require(
                    !element.hasAttr("data-as"), element, "data-as requires data-each");
            if (element.hasAttr("data-if")) {
                boolean show = flag(element, element.attr("data-if"));
                element.removeAttr("data-if");
                if (!show) {
                    element.remove();
                    return;
                }
            }
            if (element.normalName().equals("if")) {
                condition(element);
                return;
            }
            MarkupSupport.require(
                    !element.normalName().equals("else"),
                    element,
                    "else requires a direct if parent");
            if (element.normalName().equals("param")) {
                parameter(element);
                return;
            }
        }
        if (bindScalar) {
            bindings.scalar(node);
        }
        for (Node child : new ArrayList<>(node.childNodes())) {
            apply(child);
        }
    }

    /**
     * Repeats cloned trusted nodes with a lexical local alias.
     *
     * @param element repeated block or row
     */
    private void repeat(Element element) {
        String alias = element.attr("data-as");
        MarkupSupport.require(
                alias.matches("[A-Za-z_][A-Za-z0-9_]*"),
                element,
                "data-as requires a simple alias");
        MarkupSupport.require(
                List.of(
                                "tr",
                                "p",
                                "div",
                                "h1",
                                "h2",
                                "h3",
                                "h4",
                                "h5",
                                "h6",
                                "ul",
                                "ol",
                                "li",
                                "table",
                                "img",
                                "box",
                                "columns",
                                "canvas",
                                "component")
                        .contains(element.normalName()),
                element,
                "data-each requires a row or block");
        Object source = required(element, element.attr("data-each"));
        MarkupSupport.require(source instanceof List<?>, element, "data-each requires a list");
        for (Object item : (List<?>) source) {
            Element clone = element.clone();
            clone.removeAttr("data-each");
            clone.removeAttr("data-as");
            var scope = new LinkedHashMap<String, Object>(values);
            scope.keySet().removeIf(key -> key.startsWith(alias + "."));
            scope.put(alias, item);
            element.before(clone);
            new TemplateExpansion(scope, locale, missing).apply(clone);
        }
        element.remove();
    }

    /**
     * Selects exactly one branch; unselected values are never evaluated.
     *
     * @param element if wrapper outside table structure
     */
    private void condition(Element element) {
        MarkupSupport.attributes(element, "flag");
        MarkupSupport.require(
                element.attributes().size() == 1 && element.hasAttr("flag"),
                element,
                "if requires only a flag attribute");
        MarkupSupport.require(
                element.parents().stream().noneMatch(parent -> parent.normalName().equals("table")),
                element,
                "Use data-if inside tables");
        var alternatives =
                element.children().stream()
                        .filter(child -> child.normalName().equals("else"))
                        .toList();
        MarkupSupport.require(alternatives.size() <= 1, element, "if permits one else branch");
        Element alternative = alternatives.isEmpty() ? null : alternatives.get(0);
        if (alternative != null) {
            MarkupSupport.require(
                    alternative.attributes().size() == 0, alternative, "else has no attributes");
            MarkupSupport.require(
                    alternative.nextSibling() == null
                            || alternative.siblingNodes().stream()
                                    .filter(
                                            sibling ->
                                                    sibling.siblingIndex()
                                                            > alternative.siblingIndex())
                                    .allMatch(
                                            sibling ->
                                                    sibling instanceof org.jsoup.nodes.Comment
                                                            || sibling instanceof TextNode text
                                                                    && text.isBlank()),
                    alternative,
                    "else must be the last branch");
        }
        boolean show = flag(element, element.attr("flag"));
        List<Node> selected =
                show
                        ? new ArrayList<>(element.childNodes())
                        : alternative == null
                                ? List.of()
                                : new ArrayList<>(alternative.childNodes());
        for (Node child : selected) {
            if (child != alternative) {
                element.before(child);
                apply(child);
            }
        }
        element.remove();
    }

    /**
     * Replaces an empty param element with a literal formatted text node.
     *
     * @param element empty param
     */
    private void parameter(Element element) {
        MarkupSupport.attributes(element, "key", "format", "pattern", "currency");
        MarkupSupport.require(
                element.childNodeSize() == 0
                        && !element.hasAttr("style")
                        && !element.hasAttr("style-name"),
                element,
                "param must be empty and unstyled");
        Object value = required(element, element.attr("key"));
        try {
            element.replaceWith(
                    new TextNode(
                            ValueFormats.format(
                                    value,
                                    optional(element, "format"),
                                    optional(element, "pattern"),
                                    locale,
                                    optional(element, "currency"))));
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            throw MarkupSupport.error(
                    element,
                    "Invalid parameter " + element.attr("key") + ": " + exception.getMessage());
        }
    }

    /**
     * Reads a named boolean without truthiness or expression evaluation.
     *
     * @param element diagnostic source
     * @param key map path
     * @return explicit boolean
     */
    private boolean flag(Element element, String key) {
        Object value = required(element, key);
        MarkupSupport.require(
                value instanceof Boolean, element, "Condition requires a boolean: " + key);
        return Boolean.TRUE.equals(value);
    }

    /**
     * Resolves a required finite map path.
     *
     * @param element diagnostic source
     * @param key path
     * @return supported value
     */
    private Object required(Element element, String key) {
        MarkupSupport.require(!key.isBlank(), element, "Data path must not be blank");
        Object value = TemplateData.find(values, key);
        MarkupSupport.require(value != null, element, "Missing variable: " + key);
        return value;
    }

    /**
     * Distinguishes an absent formatting option from its explicit string.
     *
     * @param element source
     * @param name attribute
     * @return nullable value
     */
    private static String optional(Element element, String name) {
        return element.hasAttr(name) ? element.attr(name) : null;
    }
}
