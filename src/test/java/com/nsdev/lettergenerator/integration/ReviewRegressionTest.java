package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.onePageJson;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.png;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.layout.Zone;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class ReviewRegressionTest {
    @Test
    void signatureNeverDisappearsAtOnlyFirstAndMiddleLastBoundaries() throws Exception {
        var generator = LetterPdfGenerator.builder().build();
        for (int count : List.of(33, 34, 35, 54, 55, 56, 88, 89, 90, 109, 110, 111)) {
            List<Block> body = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                body.add(paragraph("BODY_" + i));
            }
            var doc =
                    new LetterDocument(
                            1,
                            new PageSpec(null, null, 0d, 0d, 100d),
                            null,
                            List.of(new Section("body", null, body)),
                            null,
                            new Signature(
                                    SignaturePlacement.BOTTOM_LAST,
                                    List.of(paragraph("REQUIRED SIGNATURE")),
                                    new Bounds(0, 0, 170, 50)),
                            null);
            try (var pdf = Loader.loadPDF(generator.generate(doc))) {
                var stripper = new PDFTextStripper();
                String all = stripper.getText(pdf);
                assertEquals(1, all.split("REQUIRED SIGNATURE", -1).length - 1, "count=" + count);
                stripper.setStartPage(pdf.getNumberOfPages());
                stripper.setEndPage(pdf.getNumberOfPages());
                assertTrue(stripper.getText(pdf).contains("REQUIRED SIGNATURE"), "count=" + count);
                for (int i = 0; i < count; i++) {
                    assertTrue(all.contains("BODY_" + i + "\n"), "body=" + i);
                }
            }
        }
    }

    @Test
    void imageThatFitsOrdinaryPageCanPrecedeSeparateSignaturePage() throws Exception {
        var doc =
                new LetterDocument(
                        1,
                        new PageSpec(null, null, 0d, 0d, 100d),
                        null,
                        List.of(
                                new Section(
                                        "body",
                                        null,
                                        List.of(
                                                new ImageBlock(
                                                        png(), "image/png", 100d, 180d, null)))),
                        null,
                        new Signature(
                                SignaturePlacement.BOTTOM_LAST,
                                List.of(paragraph("REQUIRED SIGNATURE")),
                                new Bounds(0, 0, 170, 50)),
                        null);
        try (var pdf = Loader.loadPDF(LetterPdfGenerator.builder().build().generate(doc))) {
            assertEquals(2, pdf.getNumberOfPages());
            assertTrue(pdf.getPage(0).getResources().getXObjectNames().iterator().hasNext());
            var strip = new PDFTextStripper();
            strip.setStartPage(2);
            assertTrue(strip.getText(pdf).contains("REQUIRED SIGNATURE"));
        }
    }

    @Test
    void fixedGlyphInkCannotEscapeLogicalLineBoxOrItalicSideBearing() {
        var generator = LetterPdfGenerator.builder().build();
        for (boolean italic : List.of(false, true)) {
            var style =
                    new Style(
                            italic ? "Times" : null,
                            60d,
                            null,
                            null,
                            italic,
                            null,
                            null,
                            italic ? null : 0.1,
                            null,
                            null);
            var f =
                    new FixedElement(
                            "ink",
                            Zone.TOP,
                            PageSelector.ALL,
                            italic ? new Bounds(30, 0, 100, 40) : new Bounds(0, 20, 170, 5),
                            List.of(
                                    new Paragraph(
                                            List.of(new TextRun(italic ? "j" : "Ágj", null, null)),
                                            style)));
            var doc =
                    new LetterDocument(
                            1,
                            new PageSpec(null, null, 50d, 0d, 0d),
                            null,
                            List.of(new Section("body", null, List.of(paragraph("Body")))),
                            List.of(f),
                            null,
                            null);
            assertThrows(
                    PdfGenerationException.class,
                    () -> generator.generate(doc),
                    "italic=" + italic);
        }
    }

    @Test
    void bodyGlyphInkMustStayInsideItsReservedRegion() {
        var style = new Style(null, 60d, null, null, null, null, null, 0.1, null, null);
        var paragraph = new Paragraph(List.of(new TextRun("Ágj", null, null)), style);
        assertThrows(
                PdfGenerationException.class,
                () -> LetterPdfGenerator.builder().build().generate(document(List.of(paragraph))));
    }

    @Test
    void jsonEnumOrdinalsAreRejectedAtTheirFieldPaths() throws Exception {
        String base = onePageJson();
        var mapper = new ObjectMapper();
        for (String field : List.of("size", "zone", "pages", "placement", "textAlign")) {
            var root = (ObjectNode) mapper.readTree(base);
            String path;
            switch (field) {
                case "size" -> {
                    ((ObjectNode) root.get("page")).put(field, 0);
                    path = "page.size";
                }
                case "placement" -> {
                    ((ObjectNode) root.get("signature")).put(field, 0);
                    path = "signature.placement";
                }
                case "textAlign" -> {
                    ((ObjectNode) root.get("defaultStyle")).put(field, 0);
                    path = "defaultStyle.textAlign";
                }
                default -> {
                    ((ObjectNode) root.get("fixedElements").get(0)).put(field, 0);
                    path = "fixedElements[0]." + field;
                }
            }
            assertEquals(
                    path,
                    assertThrows(
                                    InvalidLetterException.class,
                                    () ->
                                            LetterPdfGenerator.builder()
                                                    .build()
                                                    .generateJson(root.toString()))
                            .fieldPath());
        }
    }

    private float runLineDistance(double multiplier) throws Exception {
        var style = new Style(null, null, null, null, null, null, null, multiplier, null, null);
        var doc =
                document(
                        List.of(
                                new Paragraph(
                                        List.of(new TextRun("First\nSecond", style, null)), null)));
        var lines = new TreeSet<Float>();
        try (var pdf = Loader.loadPDF(LetterPdfGenerator.builder().build().generate(doc))) {
            new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition p) {
                    lines.add(p.getYDirAdj());
                }
            }.getText(pdf);
        }
        return lines.last() - lines.first();
    }

    @Test
    void runLineHeightOverridesInheritedBlockLineHeight() throws Exception {
        assertTrue(runLineDistance(4) > runLineDistance(1.2) * 2);
    }
}
