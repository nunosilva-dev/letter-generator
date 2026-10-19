package com.nsdev.lettergenerator.internal.render;

import com.nsdev.lettergenerator.diagnostics.Diagnostic;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.internal.layout.Units;
import com.nsdev.lettergenerator.internal.render.fo.PageMaster;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.layout.Zone;
import com.nsdev.lettergenerator.render.DebugBox;
import com.nsdev.lettergenerator.render.RenderResult;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

/** Draws allocated authoring rectangles after successful layout without adding text or pages. */
final class DebugOverlay {
    /** Thin visible outline in PDF points. */
    private static final float OUTLINE_WIDTH_PT = 0.5f;

    /** Distinct preview color; normal output never uses it. */
    private static final Color OUTLINE_COLOR = new Color(190, 40, 160);

    /** Prevents constructing this postprocessor. */
    private DebugOverlay() {}

    /**
     * Applies declared margin, body and fixed-zone rectangles to accepted PDF pages.
     *
     * @param bytes complete ordinary PDF
     * @param document validated model
     * @param masters actual page states in page order
     * @param diagnostics accepted processing diagnostics
     * @return debug PDF with physical-page geometry
     */
    static RenderResult apply(
            byte[] bytes,
            LetterDocument document,
            List<PageMaster> masters,
            List<Diagnostic> diagnostics) {
        List<DebugBox> boxes = boxes(document, masters);
        try (var pdf = Loader.loadPDF(bytes);
                var output = new ByteArrayOutputStream()) {
            for (DebugBox box : boxes) {
                var page = pdf.getPage(box.pageNumber() - 1);
                try (var stream =
                        new PDPageContentStream(
                                pdf, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    stream.setStrokingColor(OUTLINE_COLOR);
                    stream.setLineWidth(OUTLINE_WIDTH_PT);
                    stream.addRect(
                            (float) Units.millimetersToPoints(box.xMm()),
                            page.getMediaBox().getHeight()
                                    - (float) Units.millimetersToPoints(box.yMm() + box.heightMm()),
                            (float) Units.millimetersToPoints(box.widthMm()),
                            (float) Units.millimetersToPoints(box.heightMm()));
                    stream.stroke();
                }
            }
            pdf.save(output);
            return new RenderResult(output.toByteArray(), masters.size(), diagnostics, boxes);
        } catch (IOException exception) {
            throw new PdfGenerationException("Unable to draw debug geometry", exception);
        }
    }

    /**
     * Computes declared rectangles from the actual selected page masters.
     *
     * @param document validated model
     * @param masters actual page states
     * @return physical-page rectangles, never painted glyph measurements
     */
    private static List<DebugBox> boxes(LetterDocument document, List<PageMaster> masters) {
        var boxes = new ArrayList<DebugBox>();
        var spec = document.page();
        var geometry = PageGeometry.from(spec);
        var margins = spec.marginsMm();
        double x = margins.left();
        double top = margins.top();
        double bottom = spec.heightMm() - margins.bottom() - spec.bottomZoneMm();
        for (int index = 0; index < masters.size(); index++) {
            PageMaster master = masters.get(index);
            int page = index + 1;
            boxes.add(
                    new DebugBox(
                            page,
                            "DECLARED_MARGIN_AREA",
                            x,
                            top,
                            geometry.innerWidthMm(),
                            geometry.innerHeightMm()));
            boxes.add(
                    new DebugBox(
                            page,
                            "DECLARED_PAGE_CONTENT",
                            x,
                            top + spec.topZoneMm(),
                            geometry.innerWidthMm(),
                            master.isLastPage()
                                    ? geometry.lastBodyHeightMm()
                                    : geometry.bodyHeightMm()));
            if (spec.topZoneMm() > 0) {
                boxes.add(
                        new DebugBox(
                                page,
                                "DECLARED_HEADER",
                                x,
                                top,
                                geometry.innerWidthMm(),
                                spec.topZoneMm()));
            }
            if (spec.bottomZoneMm() > 0) {
                boxes.add(
                        new DebugBox(
                                page,
                                "DECLARED_FOOTER",
                                x,
                                bottom,
                                geometry.innerWidthMm(),
                                spec.bottomZoneMm()));
            }
            for (var element : document.fixedElements()) {
                if (master.matches(element.pages())) {
                    var bounds = element.boundsMm();
                    boxes.add(
                            new DebugBox(
                                    page,
                                    "DECLARED_FIXED_ELEMENT",
                                    x + bounds.x(),
                                    (element.zone() == Zone.TOP ? top : bottom) + bounds.y(),
                                    bounds.width(),
                                    bounds.height()));
                }
            }
            if (master.isLastPage() && spec.signatureZoneMm() > 0) {
                boxes.add(
                        new DebugBox(
                                page,
                                "DECLARED_SIGNATURE_ZONE",
                                x,
                                bottom - spec.signatureZoneMm(),
                                geometry.innerWidthMm(),
                                spec.signatureZoneMm()));
            }
            var signature = document.signature();
            if (master.isLastPage()
                    && signature != null
                    && signature.placement() == SignaturePlacement.BOTTOM_LAST) {
                var bounds = signature.boundsMm();
                boxes.add(
                        new DebugBox(
                                page,
                                "DECLARED_SIGNATURE",
                                x + bounds.x(),
                                bottom - spec.signatureZoneMm() + bounds.y(),
                                bounds.width(),
                                bounds.height()));
            }
        }
        return List.copyOf(boxes);
    }
}
