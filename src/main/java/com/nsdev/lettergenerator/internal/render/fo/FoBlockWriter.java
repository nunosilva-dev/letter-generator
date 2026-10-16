package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.resource.ResourceUris;
import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.model.block.*;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Emits letter blocks and assigns private in-memory resource identifiers to images. */
final class FoBlockWriter {
    private static final double MINIMUM_LIST_INDENT_MM = 8.0;
    private static final double LABEL_WIDTH_MM_PER_FONT_POINT = 0.25;
    private static final double LABEL_SEPARATION_MM = 2.0;
    private static final int SOFT_KEEP_PRIORITY = 1;
    private static final String BULLET = "\u2022";
    private final XmlBuilder xml;
    private final Style documentStyle;
    private final Map<URI, byte[]> images = new LinkedHashMap<>();

    FoBlockWriter(XmlBuilder xml, Style documentStyle) {
        this.xml = xml;
        this.documentStyle = documentStyle;
    }

    Map<URI, byte[]> images() {
        return Map.copyOf(images);
    }

    void blocks(List<Block> blocks, Style sectionStyle) {
        for (Block block : blocks) {
            if (block instanceof PageBreakBlock) {
                FoPageBreaks.anchor(xml, true);
                continue;
            }
            ResolvedStyle style =
                    ResolvedStyle.defaults(block instanceof Heading)
                            .overlay(documentStyle)
                            .overlay(sectionStyle)
                            .overlay(block.style());
            if (block instanceof Paragraph paragraph) {
                text(paragraph.runs(), style, false);
            } else if (block instanceof Heading heading) {
                text(heading.runs(), style, true);
            } else if (block instanceof ListBlock list) {
                list(list, style);
            } else if (block instanceof ImageBlock image) {
                image(image, style);
            } else if (block instanceof TableBlock table) {
                new FoTableWriter(xml, (runs, cellStyle) -> text(runs, cellStyle, false))
                        .write(table, style);
            }
        }
    }

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

    private void text(List<TextRun> runs, ResolvedStyle style, boolean heading) {
        xml.start("block");
        FoStyleWriter.block(xml, paragraphStyle(runs, style));
        if (heading) {
            xml.attribute("keep-with-next.within-page", "always");
        }
        xml.attribute("white-space-collapse", false)
                .attribute("white-space-treatment", "preserve")
                .attribute("linefeed-treatment", "preserve");
        for (TextRun run : runs) {
            run(run, style);
        }
        xml.end();
    }

    private static ResolvedStyle paragraphStyle(List<TextRun> runs, ResolvedStyle inherited) {
        // FOP ignores fo:inline line-height. Allocate the largest resolved run line box.
        double linePoints = 0;
        for (TextRun run : runs) {
            ResolvedStyle resolved = inherited.overlay(run.style());
            linePoints = Math.max(linePoints, resolved.fontSizePt() * resolved.lineHeight());
        }
        return inherited.withLineHeight(linePoints / inherited.fontSizePt());
    }

    private void run(TextRun run, ResolvedStyle inherited) {
        if (run.href() != null) {
            xml.start("basic-link").attribute("external-destination", linkDestination(run.href()));
        }
        xml.start("inline");
        FoStyleWriter.inline(xml, inherited.overlay(run.style()));
        xml.text(run.text()).end();
        if (run.href() != null) {
            xml.end();
        }
    }

    private static String linkDestination(String href) {
        String escapedUri =
                URI.create(href)
                        .toASCIIString()
                        .replace("'", "%27")
                        .replace("(", "%28")
                        .replace(")", "%29");
        return "url('" + escapedUri + "')";
    }

    private void list(ListBlock list, ResolvedStyle style) {
        int labelCharacters = Integer.toString(list.items().size()).length() + 1;
        double indent =
                Math.max(
                        MINIMUM_LIST_INDENT_MM,
                        labelCharacters * style.fontSizePt() * LABEL_WIDTH_MM_PER_FONT_POINT
                                + LABEL_SEPARATION_MM);
        xml.start("list-block");
        FoStyleWriter.block(xml, style);
        xml.attribute("provisional-distance-between-starts", FoLengths.mm(indent))
                .attribute("provisional-label-separation", FoLengths.mm(LABEL_SEPARATION_MM));
        for (int index = 0; index < list.items().size(); index++) {
            xml.start("list-item").attribute("keep-together.within-page", SOFT_KEEP_PRIORITY);
            xml.start("list-item-label").attribute("end-indent", "label-end()");
            xml.start("block").text(list.ordered() ? (index + 1) + "." : BULLET).end();
            xml.end();
            xml.start("list-item-body").attribute("start-indent", "body-start()");
            text(list.items().get(index), style, false);
            xml.end().end();
        }
        xml.end();
    }

    private void image(ImageBlock image, ResolvedStyle style) {
        URI resource = ResourceUris.image(images.size());
        images.put(resource, image.data());
        xml.start("block");
        FoStyleWriter.spacing(xml, style);
        xml.attribute("text-align", style.textAlign().name().toLowerCase(Locale.ROOT))
                .attribute("font-size", FoLengths.pt(0))
                .attribute("line-height", FoLengths.pt(0));
        xml.start("external-graphic")
                .attribute("src", "url(" + resource + ")")
                .attribute("width", FoLengths.mm(image.widthMm()))
                .attribute("height", FoLengths.mm(image.heightMm()))
                .attribute("content-width", "scale-to-fit")
                .attribute("content-height", "scale-to-fit")
                .attribute("scaling", "uniform")
                .end();
        xml.end();
    }
}
