package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.layout;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.png;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class WatermarkTest {
    @org.junit.jupiter.api.BeforeAll
    static void prepareOutput() throws Exception {
        Files.createDirectories(Path.of("target/examples"));
    }

    static LetterDocument with(LetterDocument d, Watermark w) {
        return new LetterDocument(
                d.schemaVersion(),
                d.page(),
                d.defaultStyle(),
                d.sections(),
                d.fixedElements(),
                d.signature(),
                w);
    }

    @Test
    void absentWatermarkLeavesOriginalFopPdfUnprocessed() throws Exception {
        byte[] bytes =
                LetterPdfGenerator.builder()
                        .build()
                        .generate(document(List.of(paragraph("No watermark"))));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals("Letter Generator", pdf.getDocumentInformation().getProducer());
            assertFalse(
                    pdf.getPage(0).getCOSObject().getDictionaryObject(COSName.CONTENTS)
                            instanceof COSArray);
            assertFalse(pdf.getPage(0).getResources().getExtGStateNames().iterator().hasNext());
        }
    }

    @Test
    void textWatermarkSelectorsOpacityRotationAndPrependedContent() throws Exception {
        var generator = LetterPdfGenerator.builder().build();
        var d = layout(170, SignaturePlacement.BOTTOM_LAST);
        for (PageSelector selection : PageSelector.values()) {
            byte[] bytes =
                    generator.generate(
                            with(
                                    d,
                                    new TextWatermark(
                                            "DRAFT",
                                            "Helvetica",
                                            60d,
                                            "#808080",
                                            selection,
                                            0.2,
                                            35d)));
            try (var pdf = Loader.loadPDF(bytes)) {
                for (int p = 0; p < pdf.getNumberOfPages(); p++) {
                    boolean applies =
                            selection == PageSelector.ALL
                                    || (selection == PageSelector.FIRST ? p == 0 : p > 0);
                    var page = pdf.getPage(p);
                    var strip = new PDFTextStripper();
                    strip.setStartPage(p + 1);
                    strip.setEndPage(p + 1);
                    assertEquals(applies, strip.getText(pdf).contains("DRAFT"));
                    if (applies) {
                        List<Object> first =
                                new PDFStreamParser(page.getContentStreams().next().toByteArray())
                                        .parse();
                        assertTrue(
                                first.stream()
                                        .anyMatch(
                                                t ->
                                                        t instanceof Operator op
                                                                && op.getName().equals("Tj")),
                                first.toString());
                        assertTrue(
                                first.stream()
                                        .anyMatch(
                                                t ->
                                                        t instanceof Operator op
                                                                && op.getName().equals("cm")));
                        var gs = page.getResources().getExtGStateNames().iterator().next();
                        assertEquals(
                                0.2f,
                                page.getResources().getExtGState(gs).getNonStrokingAlphaConstant(),
                                0.0001);
                        assertTrue(
                                page.getCOSObject().getDictionaryObject(COSName.CONTENTS)
                                        instanceof COSArray);
                    }
                }
                if (selection == PageSelector.ALL) {
                    Files.write(Path.of("target/examples/watermark-text.pdf"), bytes);
                    ImageIO.write(
                            new PDFRenderer(pdf).renderImageWithDPI(0, 100),
                            "png",
                            new File("target/examples/watermark-text.png"));
                }
            }
        }
    }

    @Test
    void imageWatermarkAndRegisteredFontWatermark() throws Exception {
        var d = document(List.of(paragraph("Image watermark sample")));
        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                "Custom", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .build();
        byte[] image =
                generator.generate(
                        with(
                                d,
                                new ImageWatermark(
                                        png(),
                                        "image/png",
                                        80d,
                                        40d,
                                        PageSelector.ALL,
                                        0.15,
                                        10d)));
        try (var pdf = Loader.loadPDF(image)) {
            var page = pdf.getPage(0);
            assertTrue(page.getResources().getXObjectNames().iterator().hasNext());
            var tokens = new PDFStreamParser(page.getContentStreams().next().toByteArray()).parse();
            assertTrue(
                    tokens.stream()
                            .anyMatch(t -> t instanceof Operator op && op.getName().equals("Do")));
        }
        byte[] font =
                generator.generate(
                        with(
                                d,
                                new TextWatermark(
                                        "João Ω",
                                        "Custom",
                                        48d,
                                        "#808080",
                                        PageSelector.FIRST,
                                        0.2,
                                        0d)));
        assertTrue(text(font).contains("João Ω"));
        Files.write(Path.of("target/examples/watermark-image.pdf"), image);
        Files.write(Path.of("target/examples/watermark-custom-font.pdf"), font);
    }

    @Test
    void malformedWatermarkIsInvalidAndUnsupportedTextPreservesRenderingCause() {
        var d = document(List.of(paragraph("Body")));
        var generator = LetterPdfGenerator.builder().build();
        for (double opacity : List.of(-0.1, 1.1, Double.NaN)) {
            assertEquals(
                    "watermark.opacity",
                    assertThrows(
                                    InvalidLetterException.class,
                                    () ->
                                            generator.generate(
                                                    with(
                                                            d,
                                                            new TextWatermark(
                                                                    "DRAFT",
                                                                    "Helvetica",
                                                                    50d,
                                                                    "#888888",
                                                                    PageSelector.ALL,
                                                                    opacity,
                                                                    0d))))
                            .fieldPath());
        }
        var e =
                assertThrows(
                        PdfGenerationException.class,
                        () ->
                                generator.generate(
                                        with(
                                                d,
                                                new TextWatermark(
                                                        "漢字",
                                                        "Helvetica",
                                                        50d,
                                                        "#888888",
                                                        PageSelector.ALL,
                                                        0.2,
                                                        0d))));
        assertNotNull(e.getCause());
    }
}
