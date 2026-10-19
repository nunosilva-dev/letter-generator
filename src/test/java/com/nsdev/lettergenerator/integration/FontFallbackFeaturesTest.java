package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.model.DocumentSettings;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class FontFallbackFeaturesTest {
    @Test
    void fallbackKeepsCombiningClustersLinksAndPageFields() throws Exception {
        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                "Unicode", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .build();
        var model =
                document(
                                List.of(
                                        new Paragraph(
                                                List.of(
                                                        new TextRun(
                                                                "Latin e\u0301 Ω",
                                                                null,
                                                                "https://example.com"),
                                                        TextRun.pageCount()),
                                                null)))
                        .withSettings(
                                new DocumentSettings(null, null, null, List.of("Unicode"), null));
        var result = generator.render(model);
        assertFalse(result.diagnostics().isEmpty());
        assertTrue(
                result.diagnostics().stream().anyMatch(d -> d.code().equals("FONT_SUBSTITUTION")));
        try (var pdf = Loader.loadPDF(result.pdfBytes())) {
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Ω"));
            assertTrue(text.contains("e\u0301") || text.contains("é"));
            assertTrue(text.contains("1"));
            assertFalse(pdf.getPage(0).getAnnotations().isEmpty());
        }
    }

    @Test
    void invalidFallbackNamesRetainIndexedInputPaths() {
        for (String family : java.util.Arrays.asList(null, "", " ", "Missing")) {
            var model =
                    document(List.of(paragraph("Text")))
                            .withSettings(
                                    new DocumentSettings(
                                            null,
                                            null,
                                            null,
                                            java.util.Arrays.asList(family),
                                            null));
            var error =
                    assertThrows(
                            InvalidLetterException.class,
                            () -> new LetterPdfGenerator().render(model));
            assertEquals("settings.fallbackFonts[0]", error.fieldPath());
        }
    }

    @Test
    void fallbackRequiresTheRequestedBoldFace() {
        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                "Unicode", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .build();
        var model =
                document(
                                List.of(
                                        new com.nsdev.lettergenerator.model.block.Heading(
                                                List.of(new TextRun("Ω", null, null)), null)))
                        .withSettings(
                                new DocumentSettings(null, null, null, List.of("Unicode"), null));
        var error = assertThrows(InvalidLetterException.class, () -> generator.generate(model));
        assertTrue(error.fieldPath().contains("runs"));
        assertTrue(error.getMessage().contains("BOLD"));
    }

    @Test
    void unsupportedConfiguredGlyphAndMissingVariantFailWithInputPaths() {
        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                "Unicode", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .build();
        var model =
                document(List.of(paragraph("Missing \ud83e\udd84")))
                        .withSettings(
                                new DocumentSettings(null, null, null, List.of("Unicode"), null));
        var exception = assertThrows(InvalidLetterException.class, () -> generator.render(model));
        assertTrue(exception.fieldPath().contains("runs"));
        assertEquals(1, generator.render(document(List.of(paragraph("Ordinary")))).pageCount());
    }
}
