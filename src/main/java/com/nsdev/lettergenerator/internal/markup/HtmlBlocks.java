package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.ComponentBlock;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ListBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.ListItem;
import com.nsdev.lettergenerator.model.text.TextRun;
import com.nsdev.lettergenerator.theme.LetterTheme;
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

    /** Final document theme needed to resolve rich table-cell image fitting geometry. */
    private final LetterTheme theme;

    /**
     * Captures the inputs for one block traversal without retaining them beyond the call.
     *
     * @param assets named encoded image bytes consulted during this traversal
     * @param width available image width in millimeters
     * @param height available image height in millimeters
     * @param body whether this traversal reads body content
     * @param theme optional final document theme
     */
    private HtmlBlocks(
            Map<String, byte[]> assets,
            double width,
            double height,
            boolean body,
            LetterTheme theme) {
        this.assets = assets;
        this.width = width;
        this.height = height;
        this.body = body;
        this.theme = theme;
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
        return fragment(markup, assets, width, height, body, null);
    }

    /**
     * Compiles a fragment using final themed table-cell geometry for image percentages.
     *
     * @param markup supported HTML fragment
     * @param assets named encoded image bytes
     * @param width available region width in millimeters
     * @param height image fitting height basis in millimeters
     * @param body whether explicit body page breaks are allowed
     * @param theme optional final document theme
     * @return immutable ordered blocks
     */
    public static List<Block> fragment(
            String markup,
            Map<String, byte[]> assets,
            double width,
            double height,
            boolean body,
            LetterTheme theme) {
        return read(HtmlParser.body(markup), Style.plain(), assets, width, height, body, theme);
    }

    /**
     * Compiles a parsed region's children while preserving block order. Consecutive loose text and
     * inline nodes become paragraphs; containers preserve style scopes, tables compile rich cells,
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
        return read(parent, inherited, assets, width, height, body, null);
    }

    /**
     * Compiles parsed children using the final document theme when fitting rich cell images.
     *
     * @param parent parsed region or container
     * @param inherited nullable inherited typography
     * @param assets named encoded image bytes
     * @param width available width in millimeters
     * @param height image fitting height basis in millimeters
     * @param body whether explicit page breaks are allowed
     * @param theme optional final document theme
     * @return immutable ordered blocks
     */
    public static List<Block> read(
            Element parent,
            Style inherited,
            Map<String, byte[]> assets,
            double width,
            double height,
            boolean body,
            LetterTheme theme) {
        var reader = new HtmlBlocks(assets, width, height, body, theme);
        List<Block> blocks = reader.children(parent, Style.plain());
        return inherited == null || inherited.equals(Style.plain())
                ? blocks
                : List.of(
                        new GroupBlock(
                                blocks,
                                inherited.overlay(
                                        Style.builder().keepTogether(Keep.AUTO).build())));
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
                    case "canvas" ->
                            result.add(HtmlLayouts.canvas(element, assets, width, height, theme));
                    case "columns" ->
                            result.add(HtmlLayouts.columns(element, assets, width, height, theme));
                    case "box" ->
                            result.add(
                                    HtmlLayouts.box(element, assets, width, height, body, theme));
                    case "group" -> {
                        MarkupSupport.attributes(element);
                        Style style = HtmlStyles.text(null, element);
                        result.add(new GroupBlock(children(element, Style.plain()), style));
                    }
                    case "div" -> {
                        MarkupSupport.attributes(element);
                        Style style = HtmlStyles.text(null, element);
                        boolean hasBlocks =
                                element.children().stream()
                                        .anyMatch(child -> !InlineReader.isInline(child));
                        if (hasBlocks) {
                            MarkupSupport.require(
                                    style.spaceBeforeMm() == null && style.spaceAfterMm() == null,
                                    element,
                                    "Apply paragraph margins to the child text blocks");
                            result.add(
                                    new GroupBlock(
                                            children(element, Style.plain()),
                                            style.overlay(
                                                    Style.builder()
                                                            .keepTogether(Keep.AUTO)
                                                            .build())));
                        } else {
                            result.add(
                                    new Paragraph(
                                            InlineReader.read(element.childNodes(), Style.plain()),
                                            style));
                        }
                    }
                    case "img" ->
                            result.add(
                                    HtmlImages.read(
                                            element, assets, width, height, !body, inherited));
                    case "ul", "ol" -> result.add(list(element, inherited));
                    case "table" ->
                            result.add(
                                    HtmlTables.read(
                                            element, inherited, assets, width, height, theme));
                    case "component" -> {
                        MarkupSupport.attributes(element, "ref");
                        MarkupSupport.require(
                                element.childNodes().isEmpty(), element, "component must be empty");
                        MarkupSupport.require(
                                element.hasAttr("ref"), element, "component requires ref");
                        result.add(
                                new ComponentBlock(
                                        element.attr("ref"), HtmlStyles.text(null, element)));
                    }
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
        Style style = HtmlStyles.text(null, element);
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
        List<TextRun> runs = InlineReader.read(element.childNodes(), Style.plain());
        result.add(
                element.normalName().equals("p")
                        ? new Paragraph(runs, style)
                        : new Heading(
                                runs,
                                HtmlStyles.read(element).style(),
                                HtmlStyles.semantic(element)));
    }

    /**
     * Reads nonempty li children containing inline runs followed by optional nested lists.
     * List-level margins belong to the block; item margins and other block content are rejected.
     * Flat markup retains the original flat-item model representation.
     *
     * @param element parsed element being read and used for diagnostics
     * @param parent parent text style, or null
     * @return ordered or unordered list with immutable inline items
     */
    private ListBlock list(Element element, Style parent) {
        MarkupSupport.attributes(element);
        Style style = HtmlStyles.inherited(parent).overlay(HtmlStyles.text(null, element));
        var items = new ArrayList<List<TextRun>>();
        var contentItems = new ArrayList<ListItem>();
        boolean nested = false;
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
            Style itemStyle = HtmlStyles.text(null, item);
            MarkupSupport.require(
                    itemStyle.spaceBeforeMm() == null && itemStyle.spaceAfterMm() == null,
                    item,
                    "List item margins are unsupported; style the list instead");
            var inline = new ArrayList<Node>();
            var children = new ArrayList<ListBlock>();
            boolean childListsStarted = false;
            for (Node child : item.childNodes()) {
                if (child instanceof Element childList
                        && Set.of("ul", "ol").contains(childList.normalName())) {
                    children.add(list(childList, itemStyle));
                    childListsStarted = true;
                    nested = true;
                } else {
                    boolean indentation =
                            child instanceof Comment
                                    || child instanceof TextNode text && text.isBlank();
                    MarkupSupport.require(
                            !childListsStarted || indentation,
                            child,
                            "List item inline content must precede nested lists");
                    if (!childListsStarted) {
                        inline.add(child);
                    }
                }
            }
            var runs = InlineReader.read(inline, itemStyle);
            items.add(runs);
            contentItems.add(new ListItem(runs, children));
        }
        MarkupSupport.require(!items.isEmpty(), element, "List requires at least one item");
        return nested
                ? ListBlock.nested(contentItems, element.normalName().equals("ol"), style)
                : new ListBlock(items, element.normalName().equals("ol"), style);
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
            output.add(new Paragraph(InlineReader.read(pending, Style.plain()), Style.plain()));
        }
        pending.clear();
    }
}
