package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.*;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.renderFirst;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.*;
import com.nsdev.lettergenerator.model.layout.*;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class BodyRenderingTest {
    @org.junit.jupiter.api.BeforeAll
    static void prepareOutput() throws Exception {
        Files.createDirectories(Path.of("target/examples"));
    }

    @Test
    void javaAndJsonHaveEquivalentContentStructure() throws Exception {
        var generator = LetterPdfGenerator.builder().build();
        byte[] json = generator.generateJson(onePageJson());
        var javaDoc =
                new LetterDocument(
                        1,
                        new PageSpec(PageSize.A4, new Margins(20, 20, 20, 20), 25d, 15d, 32d),
                        new Style(
                                "Helvetica",
                                11d,
                                "#222222",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null),
                        List.of(
                                new Section(
                                        "body",
                                        null,
                                        List.of(
                                                new Heading(
                                                        List.of(
                                                                new TextRun(
                                                                        "Subject: confirmation",
                                                                        null,
                                                                        null)),
                                                        null),
                                                new Paragraph(
                                                        List.of(
                                                                new TextRun(
                                                                        "Dear Sir or Madam, we ",
                                                                        null,
                                                                        null),
                                                                new TextRun(
                                                                        "confirm",
                                                                        new Style(
                                                                                null, null, null,
                                                                                true, null, null,
                                                                                null, null, null,
                                                                                null),
                                                                        null),
                                                                new TextRun(
                                                                        " receipt of your request.",
                                                                        null,
                                                                        null)),
                                                        null)))),
                        List.of(
                                new FixedElement(
                                        "header",
                                        Zone.TOP,
                                        PageSelector.ALL,
                                        new Bounds(0, 0, 170, 20),
                                        List.of(paragraph("Organization")))),
                        new Signature(
                                SignaturePlacement.BOTTOM_LAST,
                                List.of(paragraph("The Director")),
                                new Bounds(0, 2, 100, 25)),
                        null);
        byte[] java = generator.generate(javaDoc);
        assertEquals(text(json), text(java));
        try (var a = Loader.loadPDF(json);
                var b = Loader.loadPDF(java)) {
            assertEquals(1, a.getNumberOfPages());
            assertEquals(a.getNumberOfPages(), b.getNumberOfPages());
            assertEquals(
                    a.getPage(0).getMediaBox().getWidth(),
                    b.getPage(0).getMediaBox().getWidth(),
                    0.01);
            assertEquals(
                    a.getPage(0).getMediaBox().getHeight(),
                    b.getPage(0).getMediaBox().getHeight(),
                    0.01);
        }
        Files.createDirectories(Path.of("target/examples"));
        Files.write(Path.of("target/examples/one-page.pdf"), json);
        renderFirst(json, "one-page");
    }

    @Test
    void rendersAccentsEscapedTextListsLinksAndEmbeddedImages() throws Exception {
        List<Block> blocks =
                List.of(
                        new Heading(
                                List.of(new TextRun("Confirmação & <pedido>", null, null)), null),
                        new Paragraph(
                                List.of(
                                        new TextRun("Olá João, ação > obrigação. ", null, null),
                                        new TextRun(
                                                "Website", null, "https://example.com/?x=1&y=2")),
                                null),
                        new ListBlock(
                                List.of(
                                        List.of(new TextRun("First item", null, null)),
                                        List.of(new TextRun("Second item", null, null))),
                                true,
                                null),
                        new ListBlock(
                                List.of(List.of(new TextRun("Bullet item", null, null))),
                                false,
                                null),
                        new ImageBlock(png(), "image/png", 40d, 20d, null));
        byte[] result = LetterPdfGenerator.builder().build().generate(document(blocks));
        String text = text(result);
        for (String expected :
                List.of(
                        "Confirmação & <pedido>",
                        "Olá João, ação > obrigação.",
                        "1.",
                        "2.",
                        "First item",
                        "Second item",
                        "Bullet item")) {
            assertTrue(text.contains(expected), text);
        }
        try (var pdf = Loader.loadPDF(result)) {
            assertEquals(1, pdf.getPage(0).getAnnotations().size());
            assertTrue(pdf.getPage(0).getResources().getXObjectNames().iterator().hasNext());
        }
        Files.write(Path.of("target/examples/body-features.pdf"), result);
        renderFirst(result, "body-features");
    }

    @Test
    void styleInheritanceHonorsRunOverridesAndHeadingDefaults() throws Exception {
        Style base = new Style("Courier", 10d, "#123456", null, null, null, null, null, null, null);
        Style section = new Style(null, 12d, null, null, true, null, null, null, null, null);
        Style block = new Style(null, 16d, null, true, null, null, null, null, null, null);
        Style run = new Style(null, 18d, null, false, false, true, null, null, null, null);
        var doc =
                new LetterDocument(
                        1,
                        null,
                        base,
                        List.of(
                                new Section(
                                        "body",
                                        section,
                                        List.of(
                                                new Paragraph(
                                                        List.of(
                                                                new TextRun(
                                                                        "Inherited", null, null),
                                                                new TextRun("Override", run, null)),
                                                        block)))),
                        null,
                        null,
                        null);
        var positions = new ArrayList<TextPosition>();
        try (var pdf = Loader.loadPDF(LetterPdfGenerator.builder().build().generate(doc))) {
            new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition p) {
                    positions.add(p);
                    super.processTextPosition(p);
                }
            }.getText(pdf);
        }
        assertEquals(16f, positions.get(0).getFontSizeInPt(), 0.01);
        assertTrue(positions.get(0).getFont().getName().contains("BoldOblique"));
        var end = positions.get(positions.size() - 1);
        assertEquals(18f, end.getFontSizeInPt(), 0.01);
        assertEquals("Courier", end.getFont().getName());
        positions.clear();
        try (var pdf =
                Loader.loadPDF(
                        LetterPdfGenerator.builder()
                                .build()
                                .generate(
                                        document(
                                                List.of(
                                                        new Heading(
                                                                List.of(
                                                                        new TextRun(
                                                                                "Heading", null,
                                                                                null)),
                                                                null)))))) {
            new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition p) {
                    positions.add(p);
                    super.processTextPosition(p);
                }
            }.getText(pdf);
        }
        assertEquals(14f, positions.get(0).getFontSizeInPt(), 0.01);
        assertTrue(positions.get(0).getFont().getName().contains("Bold"));
    }

    @Test
    void severalDozenPagesPreserveEveryMarkerExactlyOnceInOrder() throws Exception {
        var blocks = new ArrayList<Block>();
        for (int i = 0; i < 1800; i++) {
            blocks.add(paragraph(String.format("MARKER_%04d Portuguese content & details.", i)));
        }
        byte[] result = LetterPdfGenerator.builder().build().generate(document(blocks));
        try (var pdf = Loader.loadPDF(result)) {
            assertTrue(pdf.getNumberOfPages() >= 30);
            var renderer = new PDFRenderer(pdf);
            for (int page :
                    new TreeSet<>(
                            List.of(0, pdf.getNumberOfPages() / 2, pdf.getNumberOfPages() - 1))) {
                ImageIO.write(
                        renderer.renderImageWithDPI(page, 90),
                        "png",
                        new File("target/examples/long-body-page-" + (page + 1) + ".png"));
            }
        }
        String text = text(result);
        int previous = -1;
        for (int i = 0; i < 1800; i++) {
            String marker = String.format("MARKER_%04d", i);
            int index = text.indexOf(marker);
            assertTrue(index > previous, marker);
            assertEquals(index, text.lastIndexOf(marker));
            previous = index;
        }
        Files.write(Path.of("target/examples/long-body.pdf"), result);
    }

    @Test
    void generatorIsReusableAcrossConcurrentCalls() throws Exception {
        var generator = LetterPdfGenerator.builder().build();
        var executor = Executors.newFixedThreadPool(4);
        try {
            var futures = new ArrayList<Future<byte[]>>();
            for (int i = 0; i < 12; i++) {
                int id = i;
                futures.add(
                        executor.submit(
                                () ->
                                        generator.generate(
                                                document(List.of(paragraph("Concurrent " + id))))));
            }
            for (int i = 0; i < futures.size(); i++) {
                assertTrue(text(futures.get(i).get()).contains("Concurrent " + i));
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
