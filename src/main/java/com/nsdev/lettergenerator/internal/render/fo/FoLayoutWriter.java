package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ColumnsBlock;
import com.nsdev.lettergenerator.model.layout.BoxAppearance;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.Border;
import com.nsdev.lettergenerator.model.table.Padding;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/** Emits bounded canvas geometry, independent columns and decorated flowing boxes as native FO. */
final class FoLayoutWriter {
    /** Current document-local XML output stream. */
    private final XmlBuilder xml;

    /** Recursively paints rich content with additional nullable inherited typography. */
    private final BiConsumer<List<Block>, Style> content;

    /**
     * Binds a container writer to its parent block writer without retaining document state
     * globally.
     *
     * @param xml current composition stream
     * @param content callback for rich children and local inherited style
     */
    FoLayoutWriter(XmlBuilder xml, BiConsumer<List<Block>, Style> content) {
        this.xml = xml;
        this.content = content;
    }

    /**
     * Creates one nonshrinking flow unit with absolute children in source/list painting order.
     *
     * @param canvas validated fixed-size canvas
     * @param resolved effective outer spacing
     * @param controls explicit outer pagination policies
     */
    void canvas(CanvasBlock canvas, ResolvedStyle resolved, Style controls) {
        xml.start("block-container")
                .attribute("width", FoLengths.mm(canvas.widthMm()))
                .attribute("height", FoLengths.mm(canvas.heightMm()))
                .attribute("overflow", "error-if-overflow")
                .attribute("keep-together.within-page", "always");
        FoStyleWriter.spacing(xml, resolved);
        if (controls.keepWithNext() != null) {
            FoPaginationWriter.write(
                    xml, Style.builder().keepWithNext(controls.keepWithNext()).build());
        }
        for (var element : canvas.elements()) {
            var bounds = element.boundsMm();
            xml.start("block-container")
                    .attribute("absolute-position", "absolute")
                    .attribute("left", FoLengths.mm(bounds.x()))
                    .attribute("top", FoLengths.mm(bounds.y()))
                    .attribute("width", FoLengths.mm(bounds.width()))
                    .attribute("height", FoLengths.mm(bounds.height()))
                    .attribute("overflow", "error-if-overflow");
            content.accept(
                    element.blocks(),
                    typography(canvas.style()).overlay(typography(element.style())));
            xml.end();
        }
        xml.end();
    }

    /**
     * Writes weighted independent columns separated by fixed physical gaps. Native table layout
     * permits long columns to continue across physical pages in the ordinary flow.
     *
     * @param columns validated weighted column content
     * @param resolved effective outer spacing
     * @param controls explicit outer pagination policies
     */
    void columns(ColumnsBlock columns, ResolvedStyle resolved, Style controls) {
        xml.start("block");
        FoStyleWriter.spacing(xml, resolved);
        FoPaginationWriter.write(xml, controls);
        xml.start("table")
                .attribute("table-layout", "fixed")
                .attribute("width", "100%")
                .attribute("border-collapse", "collapse");
        for (int index = 0; index < columns.columns().size(); index++) {
            if (index > 0) {
                xml.start("table-column")
                        .attribute("column-width", FoLengths.mm(columns.gapMm()))
                        .end();
            }
            xml.start("table-column")
                    .attribute(
                            "column-width",
                            "proportional-column-width("
                                    + columns.columns().get(index).weight()
                                    + ")")
                    .end();
        }
        xml.start("table-body").start("table-row");
        for (int index = 0; index < columns.columns().size(); index++) {
            if (index > 0) {
                xml.start("table-cell").start("block").end().end();
            }
            var column = columns.columns().get(index);
            xml.start("table-cell");
            content.accept(
                    column.blocks(),
                    typography(columns.style()).overlay(typography(column.style())));
            xml.end();
        }
        xml.end().end().end().end();
    }

    /**
     * Emits flow decoration and rich children without fixing or clipping the content height.
     *
     * @param box validated flowing box
     * @param resolved effective outer spacing
     * @param controls explicit outer pagination policies
     */
    void box(BoxBlock box, ResolvedStyle resolved, Style controls) {
        xml.start("block");
        FoStyleWriter.spacing(xml, resolved);
        FoPaginationWriter.write(xml, controls);
        xml.start("table")
                .attribute("table-layout", "fixed")
                .attribute("width", "100%")
                .attribute("border-collapse", "separate")
                .attribute("border-separation", "0pt");
        xml.start("table-column").attribute("column-width", "proportional-column-width(1)").end();
        xml.start("table-body").start("table-row").start("table-cell");
        appearance(box.appearance());
        content.accept(box.blocks(), typography(box.style()));
        xml.end().end().end().end().end();
    }

    /**
     * Emits physical box decoration, retaining independent edge declarations.
     *
     * @param appearance nullable validated decoration
     */
    private void appearance(BoxAppearance appearance) {
        if (appearance == null) {
            return;
        }
        Padding padding = appearance.padding();
        if (padding != null) {
            xml.attribute("padding-top", FoLengths.mm(padding.topMm()))
                    .attribute("padding-right", FoLengths.mm(padding.rightMm()))
                    .attribute("padding-bottom", FoLengths.mm(padding.bottomMm()))
                    .attribute("padding-left", FoLengths.mm(padding.leftMm()));
        }
        if (appearance.backgroundColor() != null) {
            xml.attribute("background-color", appearance.backgroundColor());
        }
        if (appearance.borders() != null) {
            border("top", appearance.borders().top());
            border("right", appearance.borders().right());
            border("bottom", appearance.borders().bottom());
            border("left", appearance.borders().left());
        }
    }

    /**
     * Emits one optional physical border in the engine's finite line-style vocabulary.
     *
     * @param edge physical edge name
     * @param border nullable validated edge appearance
     */
    private void border(String edge, Border border) {
        if (border != null) {
            xml.attribute(
                    "border-" + edge,
                    FoLengths.pt(border.widthPt())
                            + " "
                            + border.style().name().toLowerCase(Locale.ROOT)
                            + " "
                            + border.color());
        }
    }

    /**
     * Inherits typography while retaining container spacing and pagination on the outer unit only.
     *
     * @param style nullable container typography
     * @return nonnull inheritable typography
     */
    private static Style typography(Style style) {
        return style == null
                ? Style.plain()
                : new Style(
                        style.fontFamily(),
                        style.fontSizePt(),
                        style.color(),
                        style.bold(),
                        style.italic(),
                        style.underline(),
                        style.textAlign(),
                        style.lineHeight(),
                        null,
                        null);
    }
}
