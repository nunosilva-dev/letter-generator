package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.layout;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.layout.Zone;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class FixedLayoutTest {
    @org.junit.jupiter.api.BeforeAll
    static void prepareOutput() throws Exception {
        Files.createDirectories(Path.of("target/examples"));
    }

    @Test
    void fixedSelectorsAndLastSignatureStayInsideReservedZones() throws Exception {
        var generator = LetterPdfGenerator.builder().build();
        for (int lines : List.of(2, 250)) {
            byte[] bytes = generator.generate(layout(lines, SignaturePlacement.BOTTOM_LAST));
            Files.createDirectories(Path.of("target/examples"));
            Files.write(Path.of("target/examples/layout-" + lines + ".pdf"), bytes);
            try (var pdf = Loader.loadPDF(bytes)) {
                int count = pdf.getNumberOfPages();
                assertTrue(lines == 2 ? count == 1 : count >= 5);
                for (int page = 1; page <= count; page++) {
                    final int p = page;
                    var stripper =
                            new PDFTextStripper() {
                                @Override
                                protected void writeString(String text, List<TextPosition> pos)
                                        throws IOException {
                                    if (text.contains("BODY_")) {
                                        for (var t : pos) {
                                            assertTrue(
                                                    t.getYDirAdj() > 127, "body enters top zone");
                                            assertTrue(
                                                    t.getYDirAdj() < (p == count ? 652.1 : 742.8),
                                                    "body enters bottom/signature zone");
                                        }
                                    }
                                    if (text.contains("SIGNATURE")) {
                                        assertTrue(
                                                pos.get(0).getYDirAdj() > 657
                                                        && pos.get(0).getYDirAdj() < 731);
                                    }
                                    if (text.contains("FOOTER")) {
                                        assertTrue(
                                                pos.get(0).getYDirAdj() > 742
                                                        && pos.get(0).getYDirAdj() < 785);
                                    }
                                    super.writeString(text, pos);
                                }
                            };
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String text = stripper.getText(pdf);
                    assertTrue(text.contains(page == 1 ? "FIRST HEADER" : "FOLLOWING HEADER"));
                    assertFalse(text.contains(page == 1 ? "FOLLOWING HEADER" : "FIRST HEADER"));
                    assertTrue(text.contains("REPEATED FOOTER"));
                    assertEquals(page == count, text.contains("SIGNATURE START"));
                }
                var renderer = new PDFRenderer(pdf);
                for (int p : new TreeSet<>(List.of(0, count / 2, count - 1))) {
                    ImageIO.write(
                            renderer.renderImageWithDPI(p, 100),
                            "png",
                            new File(
                                    "target/examples/layout-"
                                            + lines
                                            + "-page-"
                                            + (p + 1)
                                            + ".png"));
                }
            }
        }
    }

    @Test
    void rejectsVerticalHorizontalAndSignatureOverflowWithoutPartialPdf() throws Exception {
        for (String content : List.of("short", "W".repeat(200))) {
            var d = layout(2, SignaturePlacement.BOTTOM_LAST);
            var f =
                    new FixedElement(
                            "tiny",
                            Zone.TOP,
                            PageSelector.ALL,
                            new Bounds(0, 0, 15, 1),
                            List.of(paragraph(content)));
            var doc =
                    new LetterDocument(
                            1, d.page(), null, d.sections(), List.of(f), d.signature(), null);
            var e =
                    assertThrows(
                            PdfGenerationException.class,
                            () -> LetterPdfGenerator.builder().build().generate(doc));
            assertNotNull(e.getCause());
        }
        var d = layout(2, SignaturePlacement.BOTTOM_LAST);
        var s =
                new Signature(
                        SignaturePlacement.BOTTOM_LAST,
                        List.of(paragraph("Does not fit")),
                        new Bounds(0, 0, 100, 1));
        assertThrows(
                PdfGenerationException.class,
                () ->
                        LetterPdfGenerator.builder()
                                .build()
                                .generate(
                                        new LetterDocument(
                                                1,
                                                d.page(),
                                                null,
                                                d.sections(),
                                                d.fixedElements(),
                                                s,
                                                null)));
    }

    @Test
    void flowSignatureMovesTogetherAtPageBoundary() throws Exception {
        // 48 lines fill most of the ordinary body's 217 mm; the 2-line signature must move as a
        // unit.
        var d = layout(48, SignaturePlacement.FLOW);
        byte[] bytes = LetterPdfGenerator.builder().build().generate(d);
        try (var pdf = Loader.loadPDF(bytes)) {
            var strip = new PDFTextStripper();
            int signaturePage = -1;
            for (int p = 1; p <= pdf.getNumberOfPages(); p++) {
                strip.setStartPage(p);
                strip.setEndPage(p);
                String text = strip.getText(pdf);
                assertEquals(text.contains("SIGNATURE START"), text.contains("SIGNATURE END"));
                if (text.contains("SIGNATURE START")) {
                    signaturePage = p;
                }
            }
            assertEquals(pdf.getNumberOfPages(), signaturePage);
        }
        Files.write(Path.of("target/examples/flow-signature.pdf"), bytes);
    }

    @Test
    void longParagraphContinuesAcrossPagesWithoutTruncation() throws Exception {
        String content =
                "A paragraph continues naturally over page boundaries. ".repeat(1600)
                        + "END_OF_PARAGRAPH";
        byte[] bytes =
                LetterPdfGenerator.builder()
                        .build()
                        .generate(document(List.of(paragraph(content))));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertTrue(pdf.getNumberOfPages() > 5);
        }
        assertTrue(text(bytes).contains("END_OF_PARAGRAPH"));
    }
}
