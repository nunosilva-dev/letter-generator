package com.nsdev.lettergenerator.internal.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Validates a deliberately finite SVG tree without deleting or rewriting supplied content. */
final class SvgSanitizer {
    /** The only accepted element namespace. */
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

    /** Namespace accepted only for legacy local xlink:href references. */
    private static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";

    /** Maximum XML nesting depth, bounding recursive native engine traversal. */
    private static final int MAX_DEPTH = 64;

    /** Maximum number of XML nodes, including whitespace and comments. */
    private static final int MAX_NODES = 10_000;

    /** Static shapes, text and reusable paint/clip resources accepted by the library. */
    private static final Set<String> ELEMENTS =
            Set.of(
                    "svg",
                    "g",
                    "defs",
                    "title",
                    "desc",
                    "rect",
                    "circle",
                    "ellipse",
                    "line",
                    "polyline",
                    "polygon",
                    "path",
                    "text",
                    "tspan",
                    "textPath",
                    "use",
                    "symbol",
                    "linearGradient",
                    "radialGradient",
                    "stop",
                    "clipPath",
                    "mask",
                    "pattern",
                    "marker");

    /** Inline presentation properties; every URL-like value is checked separately. */
    private static final Set<String> PRESENTATION =
            Set.of(
                    "fill",
                    "fill-opacity",
                    "fill-rule",
                    "stroke",
                    "stroke-width",
                    "stroke-opacity",
                    "stroke-linecap",
                    "stroke-linejoin",
                    "stroke-miterlimit",
                    "stroke-dasharray",
                    "stroke-dashoffset",
                    "opacity",
                    "color",
                    "display",
                    "visibility",
                    "clip-path",
                    "clip-rule",
                    "mask",
                    "marker-start",
                    "marker-mid",
                    "marker-end",
                    "font-family",
                    "font-size",
                    "font-style",
                    "font-weight",
                    "font-stretch",
                    "font-variant",
                    "text-anchor",
                    "text-decoration",
                    "dominant-baseline",
                    "alignment-baseline",
                    "baseline-shift",
                    "letter-spacing",
                    "word-spacing",
                    "stop-color",
                    "stop-opacity",
                    "color-interpolation",
                    "shape-rendering",
                    "text-rendering",
                    "vector-effect",
                    "overflow");

    /** Structural and geometry attributes accepted in addition to presentation properties. */
    private static final Set<String> ATTRIBUTES =
            Set.of(
                    "id",
                    "version",
                    "width",
                    "height",
                    "viewBox",
                    "preserveAspectRatio",
                    "x",
                    "y",
                    "x1",
                    "x2",
                    "y1",
                    "y2",
                    "cx",
                    "cy",
                    "r",
                    "rx",
                    "ry",
                    "d",
                    "points",
                    "transform",
                    "dx",
                    "dy",
                    "rotate",
                    "textLength",
                    "lengthAdjust",
                    "href",
                    "style",
                    "offset",
                    "gradientUnits",
                    "gradientTransform",
                    "spreadMethod",
                    "fx",
                    "fy",
                    "fr",
                    "clipPathUnits",
                    "maskUnits",
                    "maskContentUnits",
                    "patternUnits",
                    "patternContentUnits",
                    "patternTransform",
                    "markerUnits",
                    "markerWidth",
                    "markerHeight",
                    "refX",
                    "refY",
                    "orient",
                    "startOffset",
                    "method",
                    "spacing");

    /** Local fragment grammar intentionally excludes percent encoding and URI separators. */
    private static final Pattern FRAGMENT = Pattern.compile("#[A-Za-z_][A-Za-z0-9_.-]*");

    /** CSS URL tokens allow a quoted or unquoted local fragment only. */
    private static final Pattern LOCAL_URL =
            Pattern.compile(
                    "url\\s*\\(\\s*(['\"]?)(#[A-Za-z_][A-Za-z0-9_.-]*)\\1\\s*\\)",
                    Pattern.CASE_INSENSITIVE);

    /** IDs resolved without allowing duplicate definitions. */
    private final Map<String, Element> ids = new HashMap<>();

    /** Local-reference edges retained by element identity for cycle checks. */
    private final Map<Element, List<String>> references = new IdentityHashMap<>();

    /** Counts every visited node against the finite input budget. */
    private int nodes;

    /** Creates a validator scoped to one document and one validation call. */
    private SvgSanitizer() {}

    /**
     * Checks complete XML content, IDs, all references and graph cycles.
     *
     * @param document securely parsed XML document
     */
    static void validate(Document document) {
        Element root = document.getDocumentElement();
        require(root != null && "svg".equals(root.getLocalName()), "Image root must be svg");
        var validator = new SvgSanitizer();
        validator.visit(document, 0);
        validator.checkReferences(root, new HashSet<>(), new IdentityHashMap<>());
    }

    /**
     * Walks all nodes with explicit size/depth budgets and validates every attribute value.
     *
     * @param node next document node
     * @param depth nesting level including the document root
     */
    private void visit(Node node, int depth) {
        require(++nodes <= MAX_NODES && depth <= MAX_DEPTH, "SVG exceeds node or nesting limits");
        if (node instanceof Element element) {
            require(
                    SVG_NAMESPACE.equals(element.getNamespaceURI())
                            && ELEMENTS.contains(element.getLocalName()),
                    "Unsupported SVG element: " + element.getNodeName());
            references.put(element, new ArrayList<>());
            var attributes = element.getAttributes();
            for (int index = 0; index < attributes.getLength(); index++) {
                attribute(element, attributes.item(index));
            }
        } else {
            require(
                    node.getNodeType() == Node.DOCUMENT_NODE
                            || node.getNodeType() == Node.TEXT_NODE
                            || node.getNodeType() == Node.CDATA_SECTION_NODE
                            || node.getNodeType() == Node.COMMENT_NODE,
                    "Unsupported SVG XML construct");
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            visit(child, depth + 1);
        }
    }

    /**
     * Accepts only known namespaces/attributes and validates local resource values and inline CSS.
     *
     * @param element element owning this attribute
     * @param attribute one supplied XML attribute
     */
    private void attribute(Element element, Node attribute) {
        String name = attribute.getLocalName();
        String namespace = attribute.getNamespaceURI();
        String value = attribute.getNodeValue().trim();
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespace)) {
            require(
                    SVG_NAMESPACE.equals(value) || XLINK_NAMESPACE.equals(value),
                    "Foreign SVG namespace");
            return;
        }
        if (XMLConstants.XML_NS_URI.equals(namespace)) {
            require(Set.of("space", "lang").contains(name), "Unsupported SVG XML attribute");
            return;
        }
        require(
                namespace == null || (XLINK_NAMESPACE.equals(namespace) && "href".equals(name)),
                "Foreign SVG attribute namespace");
        require(
                ATTRIBUTES.contains(name) || PRESENTATION.contains(name),
                "Unsupported SVG attribute: " + name);
        if ("id".equals(name)) {
            require(
                    FRAGMENT.matcher("#" + value).matches()
                            && ids.putIfAbsent(value, element) == null,
                    "Invalid or duplicate SVG id");
        } else if ("href".equals(name)) {
            require(FRAGMENT.matcher(value).matches(), "SVG href must be a local fragment");
            references.get(element).add(value.substring(1));
        } else if ("style".equals(name)) {
            style(element, value);
        } else {
            resourceValue(element, value);
        }
    }

    /**
     * Validates finite inline presentation declarations without CSS escapes, imports or rules.
     *
     * @param element element receiving any local reference edges
     * @param style supplied inline CSS declarations
     */
    private void style(Element element, String style) {
        require(
                !style.contains("\\")
                        && !style.contains("@")
                        && !style.contains("/*")
                        && !style.contains("{")
                        && !style.contains("}"),
                "Unsafe SVG inline style");
        for (String declaration : style.split(";")) {
            if (!declaration.isBlank()) {
                int colon = declaration.indexOf(':');
                require(
                        colon > 0 && PRESENTATION.contains(declaration.substring(0, colon).trim()),
                        "Unsupported SVG style property");
                resourceValue(element, declaration.substring(colon + 1));
            }
        }
    }

    /**
     * Rejects encoded or nonlocal URLs and collects all accepted local paint/clip references.
     *
     * @param element referencing element
     * @param value attribute or presentation value
     */
    private void resourceValue(Element element, String value) {
        require(!value.contains("\\") && !value.contains("@"), "Unsafe SVG resource value");
        Matcher matcher = LOCAL_URL.matcher(value);
        while (matcher.find()) {
            references.get(element).add(matcher.group(2).substring(1));
        }
        String remainder = matcher.reset().replaceAll("").toLowerCase(Locale.ROOT);
        require(
                !remainder.contains("url")
                        && !remainder.contains("://")
                        && !remainder.contains("data:")
                        && !remainder.contains("file:"),
                "SVG resources must use local fragments");
    }

    /**
     * Traverses child and local-reference edges with a shared visited set to avoid repeated work.
     *
     * @param element next graph vertex
     * @param active current traversal path for cycle detection
     * @param complete expanded node counts for vertices already checked completely
     * @return expanded subtree size, bounded by the static node budget
     */
    private int checkReferences(
            Element element, Set<Element> active, Map<Element, Integer> complete) {
        if (complete.containsKey(element)) {
            return complete.get(element);
        }
        require(
                active.size() < MAX_DEPTH && active.add(element),
                "Cyclic or excessively deep SVG reference");
        int expanded = 1;
        for (String reference : references.get(element)) {
            Element target = ids.get(reference);
            require(target != null, "Missing SVG fragment: " + reference);
            expanded += checkReferences(target, active, complete);
            require(expanded <= MAX_NODES, "SVG reference expansion exceeds node budget");
        }
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element nested) {
                expanded += checkReferences(nested, active, complete);
                require(expanded <= MAX_NODES, "SVG reference expansion exceeds node budget");
            }
        }
        active.remove(element);
        complete.put(element, expanded);
        return expanded;
    }

    /**
     * Throws an authoring exception for rejected input without attempting recovery or rendering.
     *
     * @param condition required static SVG precondition
     * @param message reason for rejection
     */
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
