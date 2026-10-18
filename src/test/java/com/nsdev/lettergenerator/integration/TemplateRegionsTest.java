package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.png;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class TemplateRegionsTest {
    @Test
    void regionSourceOrderDoesNotChangeLayoutOrFinalPageSignature() throws Exception {
        String logo = "<logo><img src='asset:logo' width='12mm' height='8mm'></logo>";
        String header =
                "<header align='right' x-mm='80' y-mm='5' width-mm='80' height-mm='20'><p>HEADER</p></header>";
        String main = "<main><p>FIRST BODY</p><page-break></page-break><p>FINAL BODY</p></main>";
        String footer = "<footer style='font-size:8pt'><p>FOOTER</p></footer>";
        String signature =
                "<signature placement='bottom-last' height-mm='25'><p>SIGNED</p></signature>";
        String watermark =
                "<watermark pages='first' opacity='0.2' rotation='10' style='font-size:22pt'>COPY</watermark>";
        String root =
                "<letter page='LETTER' margins-mm='15' header-height-mm='30' footer-height-mm='18' style='font-size:10pt'>%s</letter>";
        byte[] image = png();
        var canonical =
                Letter.fromHtml(
                                root.formatted(
                                        logo + main + header + footer + signature + watermark))
                        .asset("logo", image)
                        .build();
        var reordered =
                Letter.fromHtml(
                                root.formatted(
                                        signature + footer + watermark + header + main + logo))
                        .asset("logo", image)
                        .build();
        assertEquals(canonical, reordered);
        assertEquals(30, canonical.page().topZoneMm());
        assertEquals(23, canonical.page().bottomZoneMm());
        assertEquals(25, canonical.page().signatureZoneMm());
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(reordered))) {
            assertEquals(2, pdf.getNumberOfPages());
            for (int page = 1; page <= 2; page++) {
                var reader = new PDFTextStripper();
                reader.setStartPage(page);
                reader.setEndPage(page);
                String text = reader.getText(pdf);
                assertTrue(text.contains("HEADER"));
                assertTrue(text.contains("FOOTER"));
                assertEquals(page == 2, text.contains("SIGNED"));
                assertEquals(page == 1, text.contains("COPY"));
            }
        }
    }

    @Test
    void imageRegionsKeepTheirOwnDefaultsAndStyles() throws Exception {
        var letter =
                Letter.fromHtml(
                                """
                <letter style="font-size:11pt;font-weight:bold;color:red">
                  <main><p>Body</p></main>
                  <logo><img src="asset:image"></logo>
                  <watermark pages="following" opacity="0.3"><img src="asset:image"></watermark>
                </letter>
                """)
                        .asset("image", png())
                        .build();
        var logo = (ImageBlock) letter.fixedElements().get(0).blocks().get(0);
        assertEquals(40, logo.widthMm());
        assertEquals(20, logo.heightMm());
        assertEquals("#FF0000", logo.style().color());
        var watermark = (ImageWatermark) letter.watermark();
        assertEquals(80, watermark.widthMm());
        assertEquals(40, watermark.heightMm());
        assertEquals(0, watermark.rotationDegrees());
        assertEquals(0.3, watermark.opacity());
        assertEquals(PageSelector.FOLLOWING, watermark.pages());
    }

    @Test
    void unknownDuplicateAndMissingRegionsFailAtTheirSourceLocation() {
        for (String region :
                List.of("main", "header", "footer", "signature", "logo", "watermark")) {
            String input =
                    "<letter><main><p>Body</p></main><"
                            + region
                            + "></"
                            + region
                            + ">"
                            + (region.equals("main") ? "" : "<" + region + "></" + region + ">")
                            + "</letter>";
            var error =
                    assertThrows(
                            InvalidLetterException.class, () -> Letter.fromHtml(input).build());
            assertTrue(error.getMessage().contains("Duplicate letter region: " + region));
            assertTrue(error.fieldPath().contains(region));
        }
        var unknown =
                assertThrows(
                        InvalidLetterException.class,
                        () ->
                                Letter.fromHtml(
                                                "<letter><main><p>Body</p></main><custom></custom></letter>")
                                        .build());
        assertTrue(unknown.fieldPath().contains("custom"));
        var missing =
                assertThrows(
                        InvalidLetterException.class,
                        () ->
                                Letter.fromHtml("<letter><header><p>Header</p></header></letter>")
                                        .build());
        assertTrue(missing.getMessage().contains("Letter requires one main element"));
    }
}
