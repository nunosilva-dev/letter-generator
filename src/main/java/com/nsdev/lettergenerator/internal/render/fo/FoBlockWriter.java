package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.content.ImageFormat;
import com.nsdev.lettergenerator.internal.content.SvgImages;
import com.nsdev.lettergenerator.internal.render.resource.ResourceUris;
import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ColumnsBlock;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.ListBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.PageNumber;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Serializes the sealed block vocabulary into native FO text, lists, images and tables.
 *
 * <p>Style resolution follows renderer defaults, document, section, block and run overrides. Images
 * are assigned private memory URIs and collected for the engine allowlist. This writer is mutable
 * and confined to one composition; it does not read files or validate application models.
 */
final class FoBlockWriter {
    /** Minimum physical indentation reserved for list labels. */
    private static final double MINIMUM_LIST_INDENT_MM = 8.0;

    /** Heuristic label width per character and font point, expressed in millimeters. */
    private static final double LABEL_WIDTH_MM_PER_FONT_POINT = 0.25;

    /** Gap between a list label and its text, in millimeters. */
    private static final double LABEL_SEPARATION_MM = 2.0;

    /** Breakable keep priority that keeps a list item together when it fits. */
    private static final int SOFT_KEEP_PRIORITY = 1;

    /** Native Unicode bullet used for unordered list labels. */
    private static final String BULLET = "\u2022";

    /** XML stream owned by the enclosing composition. */
    private final XmlBuilder xml;

    /** Optional document overrides applied before section/block/run styles. */
    private final Style documentStyle;

    /** Images collected under generated memory URIs for the engine resource allowlist. */
    private final Map<URI, byte[]> images = new LinkedHashMap<>();

    /**
     * Shares the current XML stream and optional document-level style defaults.
     *
     * @param xml current composition stream
     * @param documentStyle optional document-level style overrides
     */
    FoBlockWriter(XmlBuilder xml, Style documentStyle) {
        this.xml = xml;
        this.documentStyle = documentStyle;
    }

    /**
     * Returns a read-only snapshot of image URI registrations accumulated during block
     * serialization; image data follows the model's ownership rules.
     *
     * @return immutable URI lookup for images emitted so far
     */
    Map<URI, byte[]> images() {
        return Map.copyOf(images);
    }

    /**
     * Emits blocks in order with the supplied section style. Explicit page breaks become real
     * transition anchors instead of removable empty blocks.
     *
     * @param blocks ordered validated model blocks
     * @param sectionStyle optional section defaults layered after document defaults
     */
    void blocks(List<Block> blocks, Style sectionStyle) {
        blocks(blocks, sectionStyle, null);
    }

    /**
     * Emits a style scope with fallback typography below explicit and semantic styles.
     *
     * @param blocks content
     * @param sectionStyle explicit inherited overrides
     * @param fallback inherited fallback typography
     */
    private void blocks(List<Block> blocks, Style sectionStyle, Style fallback) {
        for (Block block : blocks) {
            if (block instanceof PageBreakBlock) {
                FoPageBreaks.anchor(xml, true);
                continue;
            }
            ResolvedStyle style =
                    ResolvedStyle.defaults(block instanceof Heading)
                            .overlay(fallback)
                            .overlay(documentStyle)
                            .overlay(sectionStyle)
                            .overlay(
                                    block instanceof Heading heading
                                            ? heading.semanticStyle()
                                            : null)
                            .overlay(block.style());
            Style controls =
                    (documentStyle == null ? Style.plain() : documentStyle)
                            .overlay(sectionStyle)
                            .overlay(block.style());
            if (block instanceof GroupBlock group) {
                xml.start("block");
                if (group.style() != null
                        && (group.style().spaceBeforeMm() != null
                                || group.style().spaceAfterMm() != null)) {
                    FoStyleWriter.spacing(
                            xml, ResolvedStyle.defaults(false).overlay(group.style()));
                }
                if (controls.keepTogether() == null) {
                    xml.attribute("keep-together.within-page", SOFT_KEEP_PRIORITY);
                }
                FoPaginationWriter.write(xml, controls);
                Style parent =
                        (sectionStyle == null ? Style.plain() : sectionStyle)
                                .overlay(typography(group.style()));
                Style defaults =
                        (fallback == null ? Style.plain() : fallback).overlay(group.defaultStyle());
                blocks(group.blocks(), parent, defaults);
                xml.end();
            } else if (block instanceof CanvasBlock canvas) {
                layouts(sectionStyle, fallback).canvas(canvas, style, controls);
            } else if (block instanceof ColumnsBlock columns) {
                layouts(sectionStyle, fallback).columns(columns, style, controls);
            } else if (block instanceof BoxBlock box) {
                layouts(sectionStyle, fallback).box(box, style, controls);
            } else if (block instanceof Paragraph paragraph) {
                text(paragraph.runs(), style, false, controls);
            } else if (block instanceof Heading heading) {
                text(heading.runs(), style, true, controls);
            } else if (block instanceof ListBlock list) {
                list(list, style, controls);
            } else if (block instanceof ImageBlock image) {
                image(image, style, controls);
            } else if (block instanceof TableBlock table) {
                new FoTableWriter(
                                xml,
                                (runs, cellStyle) -> text(runs, cellStyle, false),
                                (cellBlocks, cellStyle) -> blocks(cellBlocks, cellStyle.toStyle()))
                        .write(table, style, controls);
            }
        }
    }

    /**
     * Connects specialized container layout to the same recursive style and resource writer.
     *
     * @param sectionStyle nullable inherited explicit overrides
     * @param fallback nullable inherited fallback typography
     * @return a writer confined to this block scope
     */
    private FoLayoutWriter layouts(Style sectionStyle, Style fallback) {
        return new FoLayoutWriter(
                xml,
                (children, local) ->
                        blocks(
                                children,
                                (sectionStyle == null ? Style.plain() : sectionStyle)
                                        .overlay(local),
                                fallback));
    }

    /**
     * Keeps a containing group's pagination policies local while inheriting typography.
     *
     * @param value nullable group style
     * @return inheritable typography
     */
    private static Style typography(Style value) {
        return value == null
                ? null
                : new Style(
                        value.fontFamily(),
                        value.fontSizePt(),
                        value.color(),
                        value.bold(),
                        value.italic(),
                        value.underline(),
                        value.textAlign(),
                        value.lineHeight(),
                        value.spaceBeforeMm(),
                        value.spaceAfterMm());
    }

    /**
     * Writes a fixed box using millimeter bounds and a vertical region offset, with
     * error-on-overflow enabled; its contents do not consume body flow height.
     *
     * @param bounds fixed-box position and size in millimeters relative to its region
     * @param verticalOffset extra top offset in millimeters, used to place footers below signatures
     * @param content validated blocks to paint inside the fixed rectangle
     */
    void positionedBox(Bounds bounds, double verticalOffset, List<Block> content) {
        xml.start("block-container")
                .attribute("absolute-position", "absolute")
                .attribute("left", FoLengths.mm(bounds.x()))
                .attribute("top", FoLengths.mm(bounds.y() + verticalOffset))
                .attribute("width", FoLengths.mm(bounds.width()))
                .attribute("height", FoLengths.mm(bounds.height()))
                .attribute("overflow", "error-if-overflow");
        blocks(content, null);
        xml.end();
    }

    /**
     * Emits native selectable text while preserving explicit newlines and spaces; heading blocks
     * request keeping with the next body block.
     *
     * @param runs ordered native-text runs
     * @param style resolved paragraph appearance
     * @param heading whether to prefer keeping this block with the next block
     */
    private void text(List<TextRun> runs, ResolvedStyle style, boolean heading) {
        text(runs, style, heading, null);
    }

    /**
     * Writes native text using ordinary heading keeps unless an explicit policy overrides them.
     *
     * @param runs ordered inline content
     * @param style effective typography
     * @param heading whether the ordinary heading keep applies
     * @param controls nullable explicit pagination overrides
     */
    private void text(List<TextRun> runs, ResolvedStyle style, boolean heading, Style controls) {
        xml.start("block").attribute("role", heading ? "H1" : "P");
        FoStyleWriter.block(xml, paragraphStyle(runs, style));
        if (heading && (controls == null || controls.keepWithNext() == null)) {
            xml.attribute("keep-with-next.within-page", "always");
        }
        FoPaginationWriter.write(xml, controls);
        xml.attribute("white-space-collapse", false)
                .attribute("white-space-treatment", "preserve")
                .attribute("linefeed-treatment", "preserve");
        for (TextRun run : runs) {
            run(run, style);
        }
        xml.end();
    }

    /**
     * Raises the paragraph line box to the largest resolved inline-run height because FOP does not
     * apply line-height on inline elements.
     *
     * @param runs inline runs whose font sizes/line heights may override the paragraph
     * @param inherited resolved paragraph style
     * @return paragraph style with enough line height for its largest run
     */
    private static ResolvedStyle paragraphStyle(List<TextRun> runs, ResolvedStyle inherited) {
        // FOP ignores fo:inline line-height. Allocate the largest resolved run line box.
        double linePoints = 0;
        for (TextRun run : runs) {
            ResolvedStyle resolved = inherited.overlay(run.style());
            linePoints = Math.max(linePoints, resolved.fontSizePt() * resolved.lineHeight());
        }
        return inherited.withLineHeight(linePoints / inherited.fontSizePt());
    }

    /**
     * Writes one escaped inline run and optional PDF link, applying run overrides after the
     * inherited paragraph style.
     *
     * @param run text, optional link and optional inline overrides
     * @param inherited resolved enclosing paragraph appearance
     */
    private void run(TextRun run, ResolvedStyle inherited) {
        if (run.href() != null) {
            xml.start("basic-link").attribute("external-destination", linkDestination(run.href()));
        }
        xml.start("inline");
        FoStyleWriter.inline(xml, inherited.overlay(run.style()));
        if (run.pageNumber() == PageNumber.CURRENT) {
            xml.start("page-number").end();
        } else if (run.pageNumber() == PageNumber.TOTAL) {
            xml.start("page-number-citation-last")
                    .attribute("ref-id", FoPageLayoutWriter.PAGE_SEQUENCE_ID)
                    .end();
        } else {
            xml.text(run.text());
        }
        xml.end();
        if (run.href() != null) {
            xml.end();
        }
    }

    /**
     * Encodes a validated URI for the FO url(...) expression, escaping delimiters as well as
     * non-ASCII URI characters.
     *
     * @param href validated absolute HTTP, HTTPS or mailto URI
     * @return safely delimited FO external-destination value
     */
    private static String linkDestination(String href) {
        String escapedUri =
                URI.create(href)
                        .toASCIIString()
                        .replace("'", "%27")
                        .replace("(", "%28")
                        .replace(")", "%29");
        return "url('" + escapedUri + "')";
    }

    /**
     * Allocates label width from item-count digits/font size and serializes each item with a soft
     * keep-together request.
     *
     * @param list validated ordered or unordered items
     * @param style resolved list appearance
     * @param controls explicit pagination policy
     */
    private void list(ListBlock list, ResolvedStyle style, Style controls) {
        int itemCount =
                list.contentItems() == null ? list.items().size() : list.contentItems().size();
        int labelCharacters = Integer.toString(itemCount).length() + 1;
        double indent =
                Math.max(
                        MINIMUM_LIST_INDENT_MM,
                        labelCharacters * style.fontSizePt() * LABEL_WIDTH_MM_PER_FONT_POINT
                                + LABEL_SEPARATION_MM);
        xml.start("list-block");
        FoStyleWriter.block(xml, style);
        FoPaginationWriter.write(xml, controls);
        xml.attribute("provisional-distance-between-starts", FoLengths.mm(indent))
                .attribute("provisional-label-separation", FoLengths.mm(LABEL_SEPARATION_MM));
        for (int index = 0; index < itemCount; index++) {
            xml.start("list-item").attribute("keep-together.within-page", SOFT_KEEP_PRIORITY);
            xml.start("list-item-label").attribute("end-indent", "label-end()");
            xml.start("block").text(list.ordered() ? (index + 1) + "." : BULLET).end();
            xml.end();
            xml.start("list-item-body").attribute("start-indent", "body-start()");
            if (list.contentItems() == null) {
                text(list.items().get(index), style, false);
            } else {
                var item = list.contentItems().get(index);
                text(item.runs(), style, false);
                for (ListBlock child : item.children()) {
                    Style nestedControls = child.style() == null ? Style.plain() : child.style();
                    list(child, style.overlay(child.style()), nestedControls);
                }
            }
            xml.end().end();
        }
        xml.end();
    }

    /**
     * Registers supplied image bytes and emits a uniformly fitted graphic with zero-height text
     * metrics so line leading does not add space.
     *
     * @param image validated bytes and fitted millimeter dimensions
     * @param style resolved image-block spacing/alignment
     * @param controls explicit pagination policy
     */
    private void image(ImageBlock image, ResolvedStyle style, Style controls) {
        URI resource = ResourceUris.image(images.size());
        byte[] data = image.data();
        images.put(
                resource,
                ImageFormat.SVG.mimeType().equals(image.mimeType())
                        ? SvgImages.renderingBytes(data)
                        : data);
        xml.start("block");
        FoStyleWriter.spacing(xml, style);
        FoPaginationWriter.write(xml, controls);
        xml.attribute("text-align", style.textAlign().name().toLowerCase(Locale.ROOT))
                .attribute("font-size", FoLengths.pt(0))
                .attribute("line-height", FoLengths.pt(0));
        xml.start("external-graphic");
        if (image.altText() != null) {
            xml.attribute("fox:alt-text", image.altText());
        }
        xml.attribute("src", "url(" + resource + ")")
                .attribute("width", FoLengths.mm(image.widthMm()))
                .attribute("height", FoLengths.mm(image.heightMm()))
                .attribute("content-width", "scale-to-fit")
                .attribute("content-height", "scale-to-fit")
                .attribute("scaling", "uniform")
                .end();
        xml.end();
    }
}
