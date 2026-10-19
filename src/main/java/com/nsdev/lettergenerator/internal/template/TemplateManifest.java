package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.template.TemplateRequirement;
import com.nsdev.lettergenerator.template.TemplateRequirements;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/** Collects syntactic source requirements without evaluating conditional branches. */
final class TemplateManifest {
    /** Literal placeholder grammar; no expression or default expansion. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^{}]+)}");

    /** Text/parameter occurrences. */
    private final List<TemplateRequirement> variables = new ArrayList<>();

    /** Boolean occurrences. */
    private final List<TemplateRequirement> flags = new ArrayList<>();

    /** Collection occurrences. */
    private final List<TemplateRequirement> collections = new ArrayList<>();

    /** Image occurrences. */
    private final List<TemplateRequirement> assets = new ArrayList<>();

    /** Style occurrences. */
    private final List<TemplateRequirement> styles = new ArrayList<>();

    /** Component occurrences. */
    private final List<TemplateRequirement> components = new ArrayList<>();

    /** Creates an empty collector. */
    private TemplateManifest() {}

    /**
     * Reads a parsed source tree, preserving occurrence order and selector paths.
     *
     * @param root source tree
     * @return immutable source manifest
     */
    static TemplateRequirements read(Element root) {
        var manifest = new TemplateManifest();
        manifest.visit(root);
        return new TemplateRequirements(
                manifest.variables,
                manifest.flags,
                manifest.collections,
                manifest.assets,
                manifest.styles,
                manifest.components);
    }

    /**
     * Visits one parsed node and its trusted descendants.
     *
     * @param node source node
     */
    private void visit(Node node) {
        String path = MarkupSupport.path(node);
        if (node instanceof TextNode text) {
            placeholders(text.getWholeText(), path);
        } else if (node instanceof Element element) {
            element.attributes()
                    .forEach(
                            attribute ->
                                    placeholders(
                                            attribute.getValue(), path + "@" + attribute.getKey()));
            occurrence(element, "data-if", flags);
            occurrence(element, "data-each", collections);
            occurrence(element, "style-name", styles);
            if (element.normalName().equals("if")) {
                occurrence(element, "flag", flags);
            }
            if (element.normalName().equals("param")) {
                occurrence(element, "key", variables);
            }
            if (element.normalName().equals("component")) {
                occurrence(element, "ref", components);
            }
            if (element.normalName().equals("img") && element.attr("src").startsWith("asset:")) {
                assets.add(
                        new TemplateRequirement(
                                element.attr("src").substring("asset:".length()), path + "@src"));
            }
        }
        node.childNodes().forEach(this::visit);
    }

    /**
     * Records every placeholder occurrence.
     *
     * @param input existing scalar
     * @param path source location
     */
    private void placeholders(String input, String path) {
        var matcher = PLACEHOLDER.matcher(input);
        while (matcher.find()) {
            variables.add(new TemplateRequirement(matcher.group(1), path));
        }
    }

    /**
     * Records an existing directive/reference attribute.
     *
     * @param element source element
     * @param attribute attribute name
     * @param output occurrence list
     */
    private static void occurrence(
            Element element, String attribute, List<TemplateRequirement> output) {
        if (element.hasAttr(attribute)) {
            output.add(
                    new TemplateRequirement(
                            element.attr(attribute),
                            MarkupSupport.path(element) + "@" + attribute));
        }
    }
}
