package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.model.DocumentSettings;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.PositionedContent;
import com.nsdev.lettergenerator.model.layout.TextDirection;
import com.nsdev.lettergenerator.model.style.Style;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class DirectionAndHyphenationTest {
    private static final Style UNICODE =
            new Style("Unicode", null, null, null, null, null, null, null, null, null);

    @Test
    void nativeBidiUsesExplicitAndLanguageDerivedBaseDirection() throws Exception {
        var generator = unicodeGenerator();
        assertLatinBeforeHebrew(generator, TextDirection.LTR, "he-IL", true);
        assertLatinBeforeHebrew(generator, TextDirection.RTL, "en-GB", false);
        assertLatinBeforeHebrew(generator, TextDirection.AUTO, "he-IL", false);
        assertLatinBeforeHebrew(generator, TextDirection.AUTO, "en-GB", true);
        assertLatinBeforeHebrew(generator, null, "he-IL", true);
    }

    @Test
    void explicitlyRegisteredExceptionsActuallyBreakNativeWords() throws Exception {
        byte[] pattern =
                ("<hyphenation-info><classes>aA bB cC dD eE fF gG hH iI jJ kK lL "
                                + "mM nN oO pP qQ rR sS tT uU vV wW xX yY zZ</classes>"
                                + "<exceptions>rep-re-sen-ta-tion</exceptions></hyphenation-info>")
                        .getBytes(StandardCharsets.UTF_8);
        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                "Unicode", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .hyphenationPatterns("en-GB", pattern)
                        .build();
        var canvas =
                new CanvasBlock(
                        22,
                        25,
                        List.of(
                                new PositionedContent(
                                        "hyphenation",
                                        new Bounds(0, 0, 22, 25),
                                        List.of(paragraph("representation")),
                                        null)),
                        null);
        var model =
                new LetterDocument(
                                1,
                                null,
                                UNICODE,
                                List.of(new Section("body", null, List.of(canvas))),
                                null,
                                null,
                                null)
                        .withSettings(
                                new DocumentSettings(
                                        new LocaleSettings("en-GB"), null, null, null, true));
        try (var pdf = Loader.loadPDF(generator.generate(model))) {
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("-\n") || text.contains("-\r\n"), text);
            assertEquals("representation", text.replace("-", "").replaceAll("\\s", ""));
        }
        var otherCountry =
                model.withSettings(
                        new DocumentSettings(new LocaleSettings("en-US"), null, null, null, true));
        assertThrows(InvalidLetterException.class, () -> generator.generate(otherCountry));
    }

    private static LetterPdfGenerator unicodeGenerator() {
        return LetterPdfGenerator.builder()
                .registerClasspathFont("Unicode", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                .build();
    }

    private static void assertLatinBeforeHebrew(
            LetterPdfGenerator generator,
            TextDirection direction,
            String language,
            boolean latinFirst)
            throws Exception {
        var plain = document(List.of(paragraph("ABC אבג")));
        var model =
                new LetterDocument(1, plain.page(), UNICODE, plain.sections(), null, null, null)
                        .withSettings(
                                new DocumentSettings(
                                        new LocaleSettings(language), null, direction, null, null));
        try (var pdf = Loader.loadPDF(generator.generate(model))) {
            var positions = new PositionedText();
            positions.getText(pdf);
            float latin = positions.x("A");
            float hebrew = positions.x("א");
            assertEquals(
                    latinFirst,
                    latin < hebrew,
                    direction + " / " + language + ": A=" + latin + ", alef=" + hebrew);
        }
    }

    private static final class PositionedText extends PDFTextStripper {
        private final List<TextPosition> positions = new ArrayList<>();

        PositionedText() throws java.io.IOException {}

        @Override
        protected void processTextPosition(TextPosition position) {
            positions.add(position);
            super.processTextPosition(position);
        }

        float x(String text) {
            return positions.stream()
                    .filter(item -> item.getUnicode().equals(text))
                    .findFirst()
                    .orElseThrow()
                    .getXDirAdj();
        }
    }
}
