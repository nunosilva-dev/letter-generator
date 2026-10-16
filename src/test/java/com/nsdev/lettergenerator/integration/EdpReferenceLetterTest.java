package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.renderFirst;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.fixtures.EdpReferenceLetter;
import com.nsdev.lettergenerator.fixtures.FluentEdpLetter;
import com.nsdev.lettergenerator.font.FontVariant;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;

class EdpReferenceLetterTest {
    @Test
    void fluentApiRecreatesTheReferenceWithoutManualParagraphOrGeometryRecords() throws Exception {
        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                EdpReferenceLetter.FONT_FAMILY,
                                FontVariant.REGULAR,
                                "fonts/DejaVuSans.ttf")
                        .registerClasspathFont(
                                EdpReferenceLetter.FONT_FAMILY,
                                FontVariant.BOLD,
                                "fonts/DejaVuSans-Bold.ttf")
                        .build();
        byte[] bytes = generator.generate(FluentEdpLetter.document());
        Files.createDirectories(Path.of("target/examples"));
        Files.write(Path.of("target/examples/edp-fluent-letter.pdf"), bytes);
        renderFirst(bytes, "edp-fluent-letter");
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(1, pdf.getNumberOfPages());
        }
        String content = text(bytes);
        assertTrue(content.contains("ANA EXEMPLO"));
        assertTrue(content.contains("0,1632"));
        assertTrue(content.contains("Diretor Comercial"));
    }

    @Test
    void generatesSinglePagePortuguesePriceUpdateWithTableAndSignature() throws Exception {
        byte[] bytes =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                EdpReferenceLetter.FONT_FAMILY,
                                FontVariant.REGULAR,
                                "fonts/DejaVuSans.ttf")
                        .registerClasspathFont(
                                EdpReferenceLetter.FONT_FAMILY,
                                FontVariant.BOLD,
                                "fonts/DejaVuSans-Bold.ttf")
                        .build()
                        .generate(EdpReferenceLetter.document());
        Files.createDirectories(Path.of("target/examples"));
        Files.write(Path.of("target/examples/edp-reference-letter.pdf"), bytes);
        renderFirst(bytes, "edp-reference-letter");

        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(
                    1, pdf.getNumberOfPages(), "Reference letter must fit on a single A4 page");
            assertEquals(595.28, pdf.getPage(0).getMediaBox().getWidth(), 0.02);
            assertEquals(841.89, pdf.getPage(0).getMediaBox().getHeight(), 0.02);
            assertTrue(
                    pdf.getPage(0).getResources().getXObjectNames().iterator().hasNext(),
                    "Logo must be embedded");
        }
        String content = text(bytes).replaceAll("\\s+", " ");
        for (String expected :
                new String[] {
                    "ANA EXEMPLO",
                    "Caro/a cliente",
                    "atualizado em 2024",
                    "descida de 15%",
                    "0,2008",
                    "0,1632",
                    "0,0645",
                    "0,0815",
                    "CPE",
                    "213 53 53 53",
                    "Duarte Bettencourt",
                    "Diretor Comercial"
                }) {
            assertTrue(content.contains(expected), "Missing selectable content: " + expected);
        }
        assertTrue(content.indexOf("0,0815") < content.indexOf("Diretor Comercial"));
    }
}
