package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.png;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.renderFirst;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.*;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.Table;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.signature.ImageSignature;
import com.nsdev.lettergenerator.signature.TextSignature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FluentLetterTest {
    @TempDir Path temporary;

    @Test
    void everyExplicitBreakAllocatesAPageIncludingLeadingAndConsecutiveBreaks() throws Exception {
        String[] inputs = {"\fB", "A\f\fB", "A\f"};
        int[] expectedPages = {2, 3, 2};
        var generator = LetterPdfGenerator.builder().build();
        for (int index = 0; index < inputs.length; index++) {
            try (var pdf =
                    Loader.loadPDF(
                            generator.generate(Letter.builder().body(inputs[index]).build()))) {
                assertEquals(
                        expectedPages[index], pdf.getNumberOfPages(), "Input at index " + index);
            }
        }
    }

    @Test
    void plainBodyAcceptsNewlinesParagraphsAndExplicitPageBreaksWithA4Defaults() throws Exception {
        var letter =
                Letter.builder()
                        .body(
                                "Line one\r\nLine two\r\n\r\nAnother paragraph",
                                Letter.PAGE_BREAK,
                                "AFTER BREAK")
                        .build();
        assertEquals(PageSize.A4, letter.page().size());
        assertEquals(20, letter.page().marginsMm().left());
        assertEquals(0, letter.page().topZoneMm());
        assertEquals(0, letter.page().bottomZoneMm());
        assertEquals(4, letter.sections().get(0).blocks().size());
        assertEquals(
                "Line one\nLine two",
                ((Paragraph) letter.sections().get(0).blocks().get(0)).runs().get(0).text());
        try (var pdf = Loader.loadPDF(LetterPdfGenerator.builder().build().generate(letter))) {
            assertEquals(2, pdf.getNumberOfPages());
            assertEquals(595.28, pdf.getPage(0).getMediaBox().getWidth(), 0.02);
            var stripper = new PDFTextStripper();
            stripper.setEndPage(1);
            String first = stripper.getText(pdf);
            assertTrue(first.contains("Line one\nLine two"), first);
            assertTrue(first.contains("Another paragraph"));
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertTrue(stripper.getText(pdf).contains("AFTER BREAK"));
        }
    }

    @Test
    void completeFluentChainRendersLetterPaperTableRepeatedRegionsSignaturesAndWatermark()
            throws Exception {
        Path image = temporary.resolve("signature.png");
        Files.write(image, png());
        var letter =
                Letter.builder()
                        .page(PageSize.LETTER)
                        .body("BEFORE TABLE")
                        .table(Table.builder("Item", "Price").row("Service", "25.00").build())
                        .pageBreak()
                        .body("AFTER BREAK")
                        .signature(
                                TextSignature.of("Jane Smith\nCustomer Services"),
                                ImageSignature.of(image))
                        .header("EXAMPLE COMPANY")
                        .footer("REGISTRY 12345")
                        .logo(png())
                        .watermark("DRAFT")
                        .build();
        byte[] bytes = LetterPdfGenerator.builder().build().generate(letter);
        Files.createDirectories(Path.of("target/examples"));
        Files.write(Path.of("target/examples/fluent-letter.pdf"), bytes);
        renderFirst(bytes, "fluent-letter");
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(2, pdf.getNumberOfPages());
            assertEquals(612, pdf.getPage(0).getMediaBox().getWidth(), 0.02);
            assertEquals(792, pdf.getPage(0).getMediaBox().getHeight(), 0.02);
            var stripper = new PDFTextStripper();
            for (int page = 1; page <= 2; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String content = stripper.getText(pdf);
                assertTrue(content.contains("EXAMPLE COMPANY"));
                assertTrue(content.contains("REGISTRY 12345"));
                assertTrue(content.contains("DRAFT"));
                assertTrue(
                        pdf.getPage(page - 1)
                                .getResources()
                                .getXObjectNames()
                                .iterator()
                                .hasNext());
                assertTrue(page == 1 ? content.contains("25.00") : content.contains("Jane Smith"));
            }
        }
        String content = text(bytes);
        assertTrue(content.indexOf("BEFORE TABLE") < content.indexOf("25.00"));
        assertTrue(content.indexOf("25.00") < content.indexOf("AFTER BREAK"));
    }

    @Test
    void buildersSnapshotTheirContentAndImageBytes() throws Exception {
        var table = Table.builder("Column").row("ORIGINAL ROW");
        var firstTable = table.build();
        table.row("LATER ROW");
        var letter = Letter.builder().body("ORIGINAL BODY").table(firstTable);
        byte[] image = png();
        var signature = ImageSignature.of(image);
        letter.logo(image).signature(signature);
        Arrays.fill(image, (byte) 0);
        var first = letter.build();
        letter.body("LATER BODY");
        String content = text(LetterPdfGenerator.builder().build().generate(first));
        assertTrue(content.contains("ORIGINAL BODY"));
        assertTrue(content.contains("ORIGINAL ROW"));
        assertTrue(!content.contains("LATER"));
        var block = (ImageBlock) signature.blocks().get(0);
        assertEquals(40, block.widthMm());
        assertEquals(20, block.heightMm());
        assertThrows(
                UnsupportedOperationException.class,
                () -> first.sections().get(0).blocks().clear());
    }

    @Test
    void optionalMarginsAndStyleOverridesKeepTheSimpleDefaults() {
        var letter =
                Letter.builder()
                        .marginsMm(15)
                        .style(Style.builder().fontSizePt(12).color("#123456").bold().build())
                        .body("Hello")
                        .build();
        assertEquals(15, letter.page().marginsMm().right());
        assertEquals(12, letter.defaultStyle().fontSizePt());
        assertEquals("#123456", letter.defaultStyle().color());
        assertTrue(letter.defaultStyle().bold());
        assertEquals(3, letter.defaultStyle().spaceAfterMm());
    }

    @Test
    void factoriesApplyWatermarkDefaultsAndPreserveImageAspectRatio() throws Exception {
        TextWatermark text = TextWatermark.of("DRAFT");
        assertEquals(0.12, text.opacity());
        assertEquals(45, text.rotationDegrees());
        ImageWatermark image = ImageWatermark.of(png());
        assertEquals("image/png", image.mimeType());
        assertEquals(80, image.widthMm());
        assertEquals(40, image.heightMm());
        var signature = (ImageBlock) ImageSignature.of(png()).sizeMm(30, 10).blocks().get(0);
        assertEquals(20, signature.widthMm());
        assertEquals(10, signature.heightMm());
    }

    @Test
    void fluentApiRejectsInvalidRowsImagesAndUnusableLayout() {
        assertThrows(IllegalStateException.class, () -> Letter.builder().build());
        assertThrows(
                IllegalArgumentException.class,
                () -> Letter.builder().body("Hello").marginsMm(200).build());
        assertThrows(IllegalArgumentException.class, () -> Letter.builder().header("bad\fheader"));
        assertThrows(IllegalArgumentException.class, () -> Table.builder("A", "B").row("one"));
        assertThrows(IllegalArgumentException.class, () -> ImageSignature.of(new byte[] {1, 2, 3}));
        assertThrows(
                IllegalArgumentException.class, () -> Letter.builder().headerHeightMm(Double.NaN));
    }
}
