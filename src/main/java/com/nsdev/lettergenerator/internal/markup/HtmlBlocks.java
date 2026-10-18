package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ListBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/** Converts supported markup fragments to the same immutable blocks as the fluent API. */
public final class HtmlBlocks {
    /** Text block tags dispatched as paragraphs or headings. */
    private static final Set<String> TEXT_BLOCKS = Set.of("p", "h1", "h2", "h3", "h4", "h5", "h6");

    /** Caller-supplied named image bytes used only during this block traversal. */
    private final Map<String, byte[]> assets;

    /** Available image width in millimeters. */
    private final double width;

    /** Image fitting/percentage height base in millimeters. */
    private final double height;

    /** Whether this traversal permits page breaks and body image defaults. */
    private final boolean body;

    /**
     * Captures the inputs for one block traversal without retaining them beyond the call.
     *
     * @param assets named encoded image bytes consulted during this traversal
     * @param width available image width in millimeters
     * @param height available image height in millimeters
     * @param body whether this traversal reads body content
     */
    private HtmlBlocks(Map<String, byte[]> assets, double width, double height, boolean body) {
        this.assets = assets;
        this.width = width;
        this.height = height;
        this.body = body;
    }

    /**
     * Entry point for fluent-builder HTML chunks, starting with an empty inherited style.
     *
     * @param markup non-null HTML fragment
     * @param assets non-null map of asset names to encoded image bytes; consulted during this call
     * @param width available image width in millimeters
     * @param height available image height in millimeters
     * @param body whether this is body content, allowing explicit page breaks
     * @return immutable ordered model blocks
     * @throws IllegalArgumentException if markup is null
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if markup is unsupported
     */
    public static List<Block> fragment(
            String markup, Map<String, byte[]> assets, double width, double height, boolean body) {
        return read(HtmlParser.body(markup), Style.plain(), assets, width, height, body);
    }

    /**
     * Compiles a parsed region's children while preserving block order. Consecutive loose text and
     * inline nodes become paragraphs; containers recurse, tables remain rectangular text tables,
     * and images remain standalone blocks. This method reads but does not mutate nodes.
     *
     * @param parent non-null region/container whose children are compiled
     * @param inherited parent text style, or null for no inherited properties
     * @param assets non-null caller-supplied image map; no paths or URLs are opened
     * @param width available image width in millimeters
     * @param height available image height in millimeters
     * @param body true for body images/defaults and page breaks; false for repeated regions
     * @return immutable ordered block list, possibly empty
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if content, styling, image
     *     assets or page-break placement are unsupported
     */
    public static List<Block> read(
            Element parent,
            Style inherited,
            Map<String, byte[]> assets,
            double width,
            double height,
            boolean body) {
        return new HtmlBlocks(assets, width, height, body).children(parent, inherited);
    }

    /**
     * Dispatches direct children, flushing loose inline content before each block. A div with block
     * children is an inheritance container and cannot carry paragraph margins; an inline div is one
     * paragraph. Non-body page breaks and decorated/nonempty breaks are rejected.
     *
     * @param parent container whose children are compiled
     * @param inherited inherited text style, or null for no overrides
     * @return immutable ordered child blocks
     */
    private List<Block> children(Element parent, Style inherited) {
        var result = new ArrayList<Block>();
        var inline = new ArrayList<Node>();
        for (Node node : parent.childNodes()) {
            if (node instanceof Comment) {
                continue;
            }
            if (node instanceof TextNode
                    || node instanceof Element element && InlineReader.isInline(element)) {
                inline.add(node);
                continue;
            }
            flush(inline, inherited, result);
            MarkupSupport.require(node instanceof Element, node, "Unsupported markup content");
            Element element = (Element) node;
            String tag = element.normalName();
            if (TEXT_BLOCKS.contains(tag)) {
                textBlock(element, inherited, result);
            } else {
                switch (tag) {
                    case "div" -> {
                        MarkupSupport.attributes(element);
                        Style style = HtmlStyles.text(inherited, element);
                        boolean hasBlocks =
                                element.children().stream()
                                        .anyMatch(child -> !InlineReader.isInline(child));
                        if (hasBlocks) {
                            MarkupSupport.require(
                                    style.spaceBeforeMm() == null && style.spaceAfterMm() == null,
                                    element,
                                    "Apply paragraph margins to the child text blocks");
                            result.addAll(children(element, style));
                        } else {
                            result.add(
                                    new Paragraph(
                                            InlineReader.read(element.childNodes(), style), style));
                        }
                    }
                    case "img" ->
                            result.add(
                                    HtmlImages.read(
                                            element, assets, width, height, !body, inherited));
                    case "ul", "ol" -> result.add(list(element, inherited));
                    case "table" -> result.add(HtmlTables.read(element, inherited));
                    case "page-break" -> {
                        MarkupSupport.attributes(element);
                        MarkupSupport.require(
                                element.attributes().isEmpty() && element.childNodes().isEmpty(),
                                element,
                                "page-break must be empty and have no attributes");
                        MarkupSupport.require(
                                body, element, "Page breaks are supported only in the body");
                        result.add(new PageBreakBlock());
                    }
                    default -> throw MarkupSupport.error(element, "Unsupported element: " + tag);
                }
            }
        }
        flush(inline, inherited, result);
        return List.copyOf(result);
    }

    /**
     * Appends a paragraph or heading, except image-only text wrappers are unwrapped into image
     * blocks carrying the wrapper's inherited text style. Mixed text/images go through the inline
     * reader and fail because images must be standalone.
     *
     * @param element parsed element being read and used for diagnostics
     * @param parent parent text style, with null meaning no overrides
     * @param result caller-owned block list to append to
     */
    private void textBlock(Element element, Style parent, List<Block> result) {
        MarkupSupport.attributes(element);
        Style style = HtmlStyles.text(parent, element);
        boolean onlyImages =
                !element.children().isEmpty()
                        && element.children().stream()
                                .allMatch(child -> child.normalName().equals("img"))
                        && element.childNodes().stream()
                                .allMatch(
                                        node ->
                                                node instanceof Element
                                                        || node instanceof Comment
                                                        || node instanceof TextNode text
                                                                && text.isBlank());
        if (onlyImages) {
            for (Element image : element.children()) {
                result.add(HtmlImages.read(image, assets, width, height, !body, style));
            }
            return;
        }
        List<TextRun> runs = InlineReader.read(element.childNodes(), style);
        result.add(
                element.normalName().equals("p")
                        ? new Paragraph(runs, style)
                        : new Heading(runs, style));
    }

    /**
     * Reads a nonempty flat list of li children. Each item is inline-only; list-level margins
     * belong to the block, while item margins and nested block/list content are rejected.
     *
     * @param element parsed element being read and used for diagnostics
     * @param parent parent text style, or null
     * @return ordered or unordered list with immutable inline items
     */
    private ListBlock list(Element element, Style parent) {
        MarkupSupport.attributes(element);
        Style style = HtmlStyles.text(parent, element);
        var items = new ArrayList<List<TextRun>>();
        for (Node node : element.childNodes()) {
            if (node instanceof Comment || node instanceof TextNode text && text.isBlank()) {
                continue;
            }
            MarkupSupport.require(
                    node instanceof Element item && item.normalName().equals("li"),
                    node,
                    "Lists require li children");
            Element item = (Element) node;
            MarkupSupport.attributes(item);
            Style itemStyle = HtmlStyles.text(style, item);
            MarkupSupport.require(
                    itemStyle.spaceBeforeMm() == null && itemStyle.spaceAfterMm() == null,
                    item,
                    "List item margins are unsupported; style the list instead");
            items.add(InlineReader.read(item.childNodes(), itemStyle));
        }
        MarkupSupport.require(!items.isEmpty(), element, "List requires at least one item");
        return new ListBlock(items, element.normalName().equals("ol"), style);
    }

    /**
     * Emits a paragraph for nonblank pending content, strips inherited paragraph spacing, and
     * clears the caller-owned buffer. Indentation whitespace between blocks produces no block.
     *
     * @param pending caller-owned inline buffer, cleared after flushing
     * @param inherited inherited text style, or null for no overrides
     * @param output caller-owned output list to append to
     */
    private static void flush(List<Node> pending, Style inherited, List<Block> output) {
        boolean hasContent =
                pending.stream()
                        .anyMatch(node -> !(node instanceof TextNode text) || !text.isBlank());
        if (hasContent) {
            output.add(
                    new Paragraph(
                            InlineReader.read(pending, inherited),
                            HtmlStyles.inherited(inherited)));
        }
        pending.clear();
    }
}
