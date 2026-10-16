package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.*;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.*;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.*;
import com.nsdev.lettergenerator.model.layout.*;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class BoundaryValidationTest {
    @Test
    void rejectsFloatingSchemaVersionInsteadOfTruncating() {
        String json =
                "{\"schemaVersion\":1.9,\"sections\":[{\"name\":\"body\",\"blocks\":[{\"type\":\"paragraph\",\"runs\":[{\"text\":\"hello\"}]}]}]}";
        assertEquals(
                "schemaVersion",
                assertThrows(
                                InvalidLetterException.class,
                                () -> LetterPdfGenerator.builder().build().generateJson(json))
                        .fieldPath());
    }

    @Test
    void rejectsTrailingJsonDuplicateKeysAndUnknownNestedProperties() {
        var generator = LetterPdfGenerator.builder().build();
        for (String json :
                List.of(
                        "{} {}",
                        "{\"schemaVersion\":1,\"schemaVersion\":1}",
                        "{\"page\":{\"bad\":true}}")) {
            assertThrows(InvalidLetterException.class, () -> generator.generateJson(json));
        }
    }

    @Test
    void geometryRejectsEveryOutsideEdgeAndAllowsDisjointPageSelectors() {
        var generator = LetterPdfGenerator.builder().build();
        var d = layout(1, SignaturePlacement.BOTTOM_LAST);
        for (Bounds b :
                List.of(
                        new Bounds(-1, 0, 10, 10),
                        new Bounds(0, -1, 10, 10),
                        new Bounds(165, 0, 10, 10),
                        new Bounds(0, 24, 10, 10),
                        new Bounds(0, 0, 0, 1),
                        new Bounds(0, 0, Double.POSITIVE_INFINITY, 1))) {
            var f =
                    new FixedElement(
                            "bad", Zone.TOP, PageSelector.ALL, b, List.of(paragraph("Invalid")));
            assertThrows(
                    InvalidLetterException.class,
                    () ->
                            generator.generate(
                                    new LetterDocument(
                                            1,
                                            d.page(),
                                            null,
                                            d.sections(),
                                            List.of(f),
                                            null,
                                            null)));
        }
        assertDoesNotThrow(
                () ->
                        generator.generate(
                                d)); // FIRST and FOLLOWING boxes share their coordinates legally.
    }

    @Test
    void jpegAndBase64PngWorkButMimeMismatchFails() throws Exception {
        var g = LetterPdfGenerator.builder().build();
        String data = Base64.getEncoder().encodeToString(png());
        String json =
                "{\"schemaVersion\":1,\"sections\":[{\"name\":\"body\",\"blocks\":[{\"type\":\"image\",\"data\":\""
                        + data
                        + "\",\"mimeType\":\"image/png\",\"widthMm\":20,\"heightMm\":10}]}]}";
        try (var pdf = Loader.loadPDF(g.generateJson(json))) {
            assertTrue(pdf.getPage(0).getResources().getXObjectNames().iterator().hasNext());
        }
        var bitmap = new BufferedImage(20, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var out = new ByteArrayOutputStream();
        ImageIO.write(bitmap, "jpeg", out);
        assertDoesNotThrow(
                () ->
                        g.generate(
                                document(
                                        List.of(
                                                new ImageBlock(
                                                        out.toByteArray(),
                                                        "image/jpeg",
                                                        20d,
                                                        10d,
                                                        null)))));
        assertThrows(
                InvalidLetterException.class,
                () ->
                        g.generate(
                                document(
                                        List.of(
                                                new ImageBlock(
                                                        out.toByteArray(),
                                                        "image/png",
                                                        20d,
                                                        10d,
                                                        null)))));
    }

    @Test
    void suppliedTextCannotBecomeFoXmlOrAResourceUri() throws Exception {
        String attack =
                "<fo:external-graphic src=\"file:///etc/passwd\"/> & <!DOCTYPE x SYSTEM 'https://example.com/evil'>";
        String extracted =
                text(
                        LetterPdfGenerator.builder()
                                .build()
                                .generate(document(List.of(paragraph(attack)))));
        assertTrue(extracted.contains("<fo:external-graphic"));
        assertTrue(extracted.contains("file:///etc/passwd"));
        assertTrue(extracted.replace("\n", "").contains("https://example.com/evil"), extracted);
    }

    @Test
    void allFixedBlockTypesMustFitTheirBoxAndBodyImagesMustFitThePage() throws Exception {
        var d = layout(1, SignaturePlacement.BOTTOM_LAST);
        var g = LetterPdfGenerator.builder().build();
        for (Block block :
                List.of(
                        new Heading(List.of(new TextRun("Heading", null, null)), null),
                        new ListBlock(
                                List.of(List.of(new TextRun("Item", null, null))), true, null),
                        new ImageBlock(png(), "image/png", 10d, 10d, null))) {
            var f =
                    new FixedElement(
                            "tiny",
                            Zone.TOP,
                            PageSelector.ALL,
                            new Bounds(0, 0, 20, 1),
                            List.of(block));
            assertThrows(
                    PdfGenerationException.class,
                    () ->
                            g.generate(
                                    new LetterDocument(
                                            1,
                                            d.page(),
                                            null,
                                            d.sections(),
                                            List.of(f),
                                            null,
                                            null)));
        }
        assertThrows(
                PdfGenerationException.class,
                () ->
                        g.generate(
                                document(
                                        List.of(
                                                new ImageBlock(
                                                        png(), "image/png", 100d, 400d, null)))));
    }

    @Test
    void fixedTextWithVerySmallLineHeightStillCannotEscapeItsBox() {
        var d = layout(1, SignaturePlacement.BOTTOM_LAST);
        var style = new Style(null, 30d, null, null, null, null, null, 0.1, null, null);
        var f =
                new FixedElement(
                        "tiny",
                        Zone.TOP,
                        PageSelector.ALL,
                        new Bounds(0, 0, 100, 1),
                        List.of(
                                new Paragraph(
                                        List.of(new TextRun("Tall glyphs", null, null)), style)));
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
                                                List.of(f),
                                                null,
                                                null)));
    }

    @Test
    void letterPageAndFlowSignatureWithNoSignatureZoneWork() throws Exception {
        var d =
                new LetterDocument(
                        1,
                        new PageSpec(PageSize.LETTER, null, null, null, null),
                        null,
                        List.of(new Section("body", null, List.of(paragraph("US Letter")))),
                        null,
                        new Signature(
                                SignaturePlacement.FLOW,
                                List.of(paragraph("Flow signature")),
                                null),
                        null);
        try (var pdf = Loader.loadPDF(LetterPdfGenerator.builder().build().generate(d))) {
            assertEquals(612f, pdf.getPage(0).getMediaBox().getWidth(), 0.02);
            assertEquals(792f, pdf.getPage(0).getMediaBox().getHeight(), 0.02);
            assertTrue(new PDFTextStripper().getText(pdf).contains("Flow signature"));
        }
    }
}
