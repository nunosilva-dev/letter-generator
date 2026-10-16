package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FontRenderingTest {
    @org.junit.jupiter.api.BeforeAll
    static void prepareOutput() throws Exception {
        Files.createDirectories(Path.of("target/examples"));
    }

    @TempDir Path temp;

    @Test
    void loadsPathAndClasspathFontsAndSnapshotsBytesAtBuild() throws Exception {
        Path path = temp.resolve("regular.ttf");
        Files.copy(Path.of("src/test/resources/fonts/DejaVuSans.ttf"), path);
        var generator =
                LetterPdfGenerator.builder()
                        .registerFont("Custom", FontVariant.REGULAR, path)
                        .registerClasspathFont(
                                "Custom", FontVariant.BOLD, "fonts/DejaVuSans-Bold.ttf")
                        .build();
        Files.delete(path);
        var style = new Style("Custom", null, null, null, null, null, null, null, null, null);
        var d =
                new LetterDocument(
                        1,
                        null,
                        style,
                        List.of(
                                new Section(
                                        "body",
                                        null,
                                        List.of(
                                                new Heading(
                                                        List.of(
                                                                new TextRun(
                                                                        "Custom heading Ω",
                                                                        null,
                                                                        null)),
                                                        null),
                                                paragraph("João € Ω – embedded fonts")))),
                        null,
                        null,
                        null);
        byte[] pdf = generator.generate(d);
        assertTrue(text(pdf).contains("João € Ω"));
        try (var loaded = Loader.loadPDF(pdf)) {
            for (var font : loaded.getPage(0).getResources().getFontNames()) {
                assertTrue(loaded.getPage(0).getResources().getFont(font).isEmbedded());
            }
        }
        Files.write(Path.of("target/examples/custom-font.pdf"), pdf);
    }

    @Test
    void missingRequestedVariantAndUnsupportedGlyphNeverSilentlySubstitute() {
        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                "Custom", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .build();
        var style = new Style("Custom", null, null, null, null, null, null, null, null, null);
        var d =
                new LetterDocument(
                        1,
                        null,
                        style,
                        List.of(
                                new Section(
                                        "body",
                                        null,
                                        List.of(
                                                new Heading(
                                                        List.of(
                                                                new TextRun(
                                                                        "Needs bold", null, null)),
                                                        null)))),
                        null,
                        null,
                        null);
        assertThrows(InvalidLetterException.class, () -> generator.generate(d));
        assertThrows(
                PdfGenerationException.class,
                () ->
                        LetterPdfGenerator.builder()
                                .build()
                                .generate(document(List.of(paragraph("漢字")))));
    }
}
