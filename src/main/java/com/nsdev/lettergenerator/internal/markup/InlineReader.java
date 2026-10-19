package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.PageNumber;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/** Converts nested inline markup to styled runs, retaining text boundaries and explicit br tags. */
final class InlineReader {
    /** Finite inline vocabulary accepted inside paragraphs and table cells. */
    private static final Set<String> TAGS =
            Set.of("span", "strong", "b", "em", "i", "u", "a", "br", "page-number", "page-count");

    /**
     * HTML whitespace collapsed to a single space without treating nonbreaking spaces as blanks.
     */
    private static final Pattern HTML_WHITESPACE = Pattern.compile("[ \\t\\r\\n\\f]+");

    /** Prevents construction of this static helper. */
    private InlineReader() {}

    /**
     * Tests the fixed inline vocabulary used by block grouping and recursive run reading.
     *
     * @param element parsed element being read and used for diagnostics
     * @return whether the normalized tag belongs to the inline vocabulary
     */
    static boolean isInline(Element element) {
        return TAGS.contains(element.normalName());
    }

    /**
     * Produces immutable runs, collapsing HTML whitespace across node boundaries and trimming
     * leading/trailing spaces. Explicit br newlines survive; empty content becomes one empty run.
     * Parent style may be null; nodes and styles are read without mutation.
     *
     * @param nodes inline nodes read in source order
     * @param inherited inherited text style, or null for no overrides
     * @return immutable runs, including one empty run for empty content
     */
    static List<TextRun> read(List<Node> nodes, Style inherited) {
        var runs = new ArrayList<TextRun>();
        for (Node node : nodes) {
            append(node, inherited, null, runs);
        }
        trimSpaces(runs);
        return runs.isEmpty() ? List.of(new TextRun("", null, null)) : List.copyOf(runs);
    }

    /**
     * Walks inline descendants with inherited style and current link destination. Text whitespace
     * is collapsed without inserting spaces between adjacent elements. Anchors replace the
     * inherited href; this pass requires href presence, while link safety is validated later.
     * Images, block elements, unknown attributes and inline block margins are rejected.
     *
     * @param node parsed node to traverse
     * @param parent parent text style, with null meaning no overrides
     * @param href current inherited link destination, or null
     * @param output caller-owned output list to append to
     */
    private static void append(Node node, Style parent, String href, List<TextRun> output) {
        if (node instanceof Comment) {
            return;
        }
        if (node instanceof TextNode text) {
            String value = HTML_WHITESPACE.matcher(text.getWholeText()).replaceAll(" ");
            if (!output.isEmpty()) {
                String previous = output.get(output.size() - 1).text();
                if (previous.endsWith(" ") || previous.endsWith("\n")) {
                    value = value.replaceFirst("^ +", "");
                }
            }
            if (!value.isEmpty()) {
                output.add(new TextRun(value, HtmlStyles.inherited(parent), href));
            }
            return;
        }
        MarkupSupport.require(node instanceof Element, node, "Unsupported inline content");
        Element element = (Element) node;
        MarkupSupport.require(
                isInline(element),
                element,
                "Unsupported inline element; images and other blocks must be standalone: "
                        + element.normalName());
        if (element.normalName().equals("a")) {
            MarkupSupport.attributes(element, "href", "title");
            MarkupSupport.require(element.hasAttr("href"), element, "Link requires href");
            href = element.attr("href");
        } else {
            MarkupSupport.attributes(element);
        }
        Style style = HtmlStyles.text(parent, element);
        MarkupSupport.require(
                style.spaceBeforeMm() == null && style.spaceAfterMm() == null,
                element,
                "Inline elements cannot have block margins; style the containing text block");
        if (element.normalName().equals("page-number")
                || element.normalName().equals("page-count")) {
            MarkupSupport.require(
                    element.childNodes().isEmpty(), element, "Page fields must be empty");
            output.add(
                    new TextRun(
                            "",
                            style,
                            href,
                            element.normalName().equals("page-number")
                                    ? PageNumber.CURRENT
                                    : PageNumber.TOTAL));
        } else if (element.normalName().equals("br")) {
            output.add(new TextRun("\n", style, href));
        } else {
            for (Node child : element.childNodes()) {
                append(child, style, href, output);
            }
        }
    }

    /**
     * Trims only outer space characters across runs, retaining each surviving style and href.
     *
     * @param runs mutable run list to trim in place
     */
    private static void trimSpaces(List<TextRun> runs) {
        while (!runs.isEmpty() && runs.get(0).text().startsWith(" ")) {
            TextRun first = runs.remove(0);
            String value = first.text().replaceFirst("^ +", "");
            if (!value.isEmpty()) {
                runs.add(0, new TextRun(value, first.style(), first.href()));
            }
        }
        while (!runs.isEmpty() && runs.get(runs.size() - 1).text().endsWith(" ")) {
            TextRun last = runs.remove(runs.size() - 1);
            String value = last.text().replaceFirst(" +$", "");
            if (!value.isEmpty()) {
                runs.add(new TextRun(value, last.style(), last.href()));
            }
        }
    }
}
