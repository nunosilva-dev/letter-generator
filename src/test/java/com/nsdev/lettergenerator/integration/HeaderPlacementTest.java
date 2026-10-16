package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.png;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.renderFirst;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.*;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class HeaderPlacementTest {
    @Test
    void decimalRightAlignedWidthsFitLetterPaperDespiteFloatingPointRounding() throws Exception {
        var letter =
                Letter.builder()
                        .page(PageSize.LETTER)
                        .header("HEADER")
                        .headerWidthMm(33.3)
                        .headerAlignment(TextAlign.RIGHT)
                        .body("Body")
                        .build();
        byte[] bytes = new LetterPdfGenerator().generate(letter);
        List<TextPosition> positions = positions(bytes, 1, "HEADER");
        TextPosition last = positions.get(positions.size() - 1);
        assertEquals(195.9 * 72 / 25.4, last.getXDirAdj() + last.getWidthDirAdj(), 0.2);
    }

    @Test
    void oversizedLogoReservesItsFittedHeightWithoutAnEmptyHeaderIncreasingIt() throws Exception {
        var letter =
                Letter.builder()
                        .logo(new ImageBlock(png(), "image/png", 300d, 150d, null))
                        .body("BODY")
                        .build();
        assertEquals(90, letter.page().topZoneMm());
        assertTrue(text(new LetterPdfGenerator().generate(letter)).contains("BODY"));
    }

    @Test
    void automaticHeaderWidthsCannotConsumeTheGapBesideALogo() throws Exception {
        byte[] image = png();
        for (TextAlign alignment : List.of(TextAlign.CENTER, TextAlign.RIGHT)) {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            Letter.builder()
                                    .logo(new ImageBlock(image, "image/png", 10d, 10d, null))
                                    .header("HEADER")
                                    .headerWidthMm(160)
                                    .headerAlignment(alignment)
                                    .body("Body")
                                    .build());
        }
    }

    @Test
    void noArgumentGeneratorRendersDefaultTypographyAndWatermarkWithoutFontFiles()
            throws Exception {
        var letter =
                Letter.builder()
                        .heading("Confirmation")
                        .bodyHtml(
                                "<p>Olá João € <strong>bold</strong> <em>italic</em> <strong><em>both</em></strong></p>")
                        .watermark("COPY")
                        .build();
        byte[] bytes = new LetterPdfGenerator().generate(letter);
        assertTrue(text(bytes).contains("Olá João €"));
        assertTrue(text(bytes).contains("COPY"));
        assertTrue(text(LetterPdfGenerator.builder().build().generate(letter)).contains("both"));
    }

    @Test
    void alignedHeaderBoxesAndRenderedTextFollowTheSelectedSide() throws Exception {
        double[] expectedBoxLeft = {0, 45, 90};
        TextAlign[] alignments = {TextAlign.LEFT, TextAlign.CENTER, TextAlign.RIGHT};
        for (int index = 0; index < alignments.length; index++) {
            var letter =
                    Letter.builder()
                            .header("HEADER")
                            .headerWidthMm(80)
                            .headerAlignment(alignments[index])
                            .body("FIRST")
                            .pageBreak()
                            .body("SECOND")
                            .build();
            assertEquals(expectedBoxLeft[index], header(letter).boundsMm().x());
            assertEquals(80, header(letter).boundsMm().width());
            byte[] bytes = new LetterPdfGenerator().generate(letter);
            for (int page = 1; page <= 2; page++) {
                List<TextPosition> positions = positions(bytes, page, "HEADER");
                assertTrue(!positions.isEmpty());
                double first = positions.get(0).getXDirAdj();
                TextPosition last = positions.get(positions.size() - 1);
                double end = last.getXDirAdj() + last.getWidthDirAdj();
                switch (alignments[index]) {
                    case LEFT -> assertEquals(20 * 72 / 25.4, first, 0.2);
                    case CENTER -> assertEquals(105 * 72 / 25.4, (first + end) / 2, 0.2);
                    case RIGHT -> assertEquals(190 * 72 / 25.4, end, 0.2);
                    default -> throw new AssertionError("Unexpected test alignment");
                }
            }
        }
    }

    @Test
    void exactBoundsReserveBodySpaceAndUseFinalPageMargins() throws Exception {
        var letter =
                Letter.builder()
                        .headerHtml("<p>EXACT HEADER</p>")
                        .headerBoundsMm(88, 23, 78, 28)
                        .body("BODY")
                        .margins(new Margins(15, 18, 17, 22))
                        .build();
        assertEquals(new Bounds(88, 23, 78, 28), header(letter).boundsMm());
        assertEquals(56, letter.page().topZoneMm());
        byte[] bytes = new LetterPdfGenerator().generate(letter);
        List<TextPosition> headerText = positions(bytes, 1, "EXACT HEADER");
        assertEquals(110 * 72 / 25.4, headerText.get(0).getXDirAdj(), 0.2);
        assertTrue(headerText.get(0).getYDirAdj() > 38 * 72 / 25.4);
        assertTrue(positions(bytes, 1, "BODY").get(0).getYDirAdj() > 71 * 72 / 25.4);
        Files.createDirectories(Path.of("target/examples"));
        Files.write(Path.of("target/examples/header-exact-position.pdf"), bytes);
        renderFirst(bytes, "header-exact-position");
    }

    @Test
    void presetsUseFinalLetterWidthAndLeaveSpaceBesideTheLogo() throws Exception {
        var letter =
                Letter.builder()
                        .header("HEADER")
                        .headerWidthMm(60)
                        .headerAlignment(TextAlign.CENTER)
                        .logo(new ImageBlock(png(), "image/png", 10d, 10d, null))
                        .page(PageSize.LETTER)
                        .marginsMm(25)
                        .body("Body")
                        .build();
        assertEquals(60.45, header(letter).boundsMm().x(), 0.0001);
        assertEquals(60, header(letter).boundsMm().width());
    }

    @Test
    void imageHeadersFitExactBoundsAndRetainAlignmentAfterStyleChanges() throws Exception {
        var letter =
                Letter.builder()
                        .header(png())
                        .headerBoundsMm(90, 10, 60, 20)
                        .headerWidthMm(50)
                        .headerHeightMm(18)
                        .headerAlignment(TextAlign.RIGHT)
                        .headerStyle(Style.builder().color("#123456").build())
                        .body("Body")
                        .build();
        var region = header(letter);
        assertEquals(new Bounds(90, 10, 50, 18), region.boundsMm());
        var image = (ImageBlock) region.blocks().get(0);
        assertTrue(image.widthMm() <= 50 && image.heightMm() <= 18);
        assertEquals(TextAlign.RIGHT, image.style().textAlign());
        assertTrue(text(new LetterPdfGenerator().generate(letter)).contains("Body"));
    }

    @Test
    void storedTemplatesExposeTheSameAlignedAndExactHeaderPlacement() throws Exception {
        var aligned =
                Letter.template(
                                """
                <letter><header align="${side}" width-mm="${width}" height-mm="30">
                <p>${recipient}</p></header><main><p>BODY</p></main></letter>
                """)
                        .values(Map.of("side", "right", "width", 75, "recipient", "RECIPIENT"))
                        .build();
        assertEquals(new Bounds(95, 0, 75, 30), header(aligned).boundsMm());
        assertEquals(TextAlign.RIGHT, header(aligned).blocks().get(0).style().textAlign());
        assertTrue(text(new LetterPdfGenerator().generate(aligned)).contains("RECIPIENT"));
        var exact =
                Letter.fromHtml(
                                """
                <letter><header x-mm="85" y-mm="20" width-mm="80" height-mm="35">
                <img src="asset:banner" width="100%" height="10mm"></header>
                <main><p>BODY</p></main></letter>
                """)
                        .asset("banner", png())
                        .build();
        assertEquals(new Bounds(85, 20, 80, 35), header(exact).boundsMm());
        assertEquals(60, exact.page().topZoneMm());
        assertTrue(((ImageBlock) header(exact).blocks().get(0)).widthMm() <= 80);
        assertTrue(text(new LetterPdfGenerator().generate(exact)).contains("BODY"));
    }

    @Test
    void invalidOrIncompleteHeaderPositionsFailBeforeRendering() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Letter.builder().headerAlignment(TextAlign.JUSTIFY));
        assertThrows(
                IllegalArgumentException.class, () -> Letter.builder().headerWidthMm(Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> Letter.builder().headerBoundsMm(-1, 0, 80, 30));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Letter.builder()
                                .header("HEADER")
                                .headerBoundsMm(100, 0, 80, 30)
                                .body("Body")
                                .build());
        for (String attributes :
                List.of(
                        "align='justify'",
                        "x-mm='10'",
                        "y-mm='10' width-mm='80'",
                        "x-mm='10' y-mm='10' width-mm='80'",
                        "width-mm='-1'",
                        "x-mm='100' y-mm='0' width-mm='80' height-mm='30'")) {
            assertThrows(
                    InvalidLetterException.class,
                    () ->
                            Letter.fromHtml(
                                            "<letter><header "
                                                    + attributes
                                                    + "><p>HEADER</p></header><main><p>Body</p></main></letter>")
                                    .build(),
                    attributes);
        }
    }

    private static FixedElement header(LetterDocument letter) {
        return letter.fixedElements().stream()
                .filter(element -> element.id().equals("letter.header"))
                .findFirst()
                .orElseThrow();
    }

    private static List<TextPosition> positions(byte[] bytes, int page, String marker)
            throws Exception {
        var result = new ArrayList<TextPosition>();
        try (var pdf = Loader.loadPDF(bytes)) {
            var stripper =
                    new PDFTextStripper() {
                        @Override
                        protected void writeString(String value, List<TextPosition> positions) {
                            if (value.contains(marker)) {
                                result.addAll(positions);
                            }
                        }
                    };
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            stripper.getText(pdf);
        }
        return result;
    }
}
