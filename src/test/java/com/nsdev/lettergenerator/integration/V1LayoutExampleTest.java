package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsdev.lettergenerator.Canvas;
import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.document.PdfAttachment;
import com.nsdev.lettergenerator.letter.LetterElement;
import com.nsdev.lettergenerator.model.DocumentMetadata;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import com.nsdev.lettergenerator.signature.TextSignature;
import com.nsdev.lettergenerator.theme.LetterTheme;
import com.nsdev.lettergenerator.theme.Stationery;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/** Executable visual example combining reusable stationery, SVG, free layout and PDF packs. */
class V1LayoutExampleTest {
    @Test
    void freeLayoutLetterAndAppendixRenderFromFluentAndJsonWithTheSameGeometry() throws Exception {
        var generator = new LetterPdfGenerator();
        byte[] svg =
                """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 40">
              <rect width="120" height="40" rx="6" fill="#10384A"/>
              <path d="M12 28 L22 10 L32 28 Z" fill="#55BBA6"/>
              <text x="40" y="26" font-family="sans-serif" font-size="14" fill="white">EXAMPLE</text>
            </svg>
            """
                        .getBytes(StandardCharsets.UTF_8);
        var stationery =
                Stationery.builder()
                        .logo(svg)
                        .header("Customer services")
                        .headerAlignment(TextAlign.RIGHT)
                        .footerHtml(
                                "<p>Example Company | Page <page-number></page-number> of <page-count></page-count></p>")
                        .build();
        var company =
                LetterTheme.builder()
                        .stationery(stationery)
                        .body(Style.builder().color("#243746").fontSizePt(10).build())
                        .build();
        var panel =
                Canvas.builder(170, 60)
                        .text(
                                "recipient",
                                new Bounds(0, 20, 80, 30),
                                "Alex Example\n24 Sample Lane\nExample Town EX2 3CD")
                        .text(
                                "reference",
                                new Bounds(110, 0, 60, 20),
                                "Reference EX-1001\n19 October 2026")
                        .build();
        byte[] appendix =
                generator.generate(
                        Letter.builder()
                                .heading("Supporting information")
                                .body("This appendix retains its own page content and layout.")
                                .build());
        var model =
                Letter.builder()
                        .theme(company)
                        .metadata(
                                new DocumentMetadata(
                                        "Free layout example", "Example Company", null, null))
                        .part(LetterElement.RECIPIENT, List.of(panel))
                        .subject("Your service information")
                        .body(
                                "Dear Alex,\nThe following information is arranged using reusable layout components.")
                        .bodyHtml(
                                """
                    <columns gap-mm="10">
                      <column weight="1"><h2>Contact</h2><p>care@example.invalid<br>Reference EX-1001</p></column>
                      <column weight="2"><h2>Included services</h2><ul>
                        <li>Home service<ul><li>Account support</li><li>Monthly statements</li></ul></li>
                        <li>Paperless correspondence</li>
                      </ul></column>
                    </columns>
                    <box style="padding:4mm;border-left:2pt solid #55BBA6;background-color:#EFF7F5">
                      <p>Please keep your customer reference when contacting our team.</p>
                    </box>
                    """)
                        .pageBreak()
                        .heading("Next steps")
                        .body(
                                "Review your information and contact us with any questions.\nThe supporting document follows this letter.")
                        .signature(TextSignature.of("Customer Services\nExample Company"))
                        .signaturePlacement(SignaturePlacement.BOTTOM_LAST)
                        .signatureHeightMm(25)
                        .attach(PdfAttachment.of(appendix))
                        .build();
        var result = generator.render(model);
        var jsonResult = generator.renderJson(new ObjectMapper().writeValueAsString(model));
        assertEquals(3, result.pageCount());
        assertEquals(2, result.parts().size());
        assertEquals(3, result.parts().get(1).firstPage());
        assertEquals(text(result.pdfBytes()), text(jsonResult.pdfBytes()));
        Files.createDirectories(Path.of("target/examples"));
        try (var pdf = Loader.loadPDF(result.pdfBytes())) {
            var renderer = new org.apache.pdfbox.rendering.PDFRenderer(pdf);
            for (int index = 0; index < pdf.getNumberOfPages(); index++) {
                javax.imageio.ImageIO.write(
                        renderer.renderImageWithDPI(index, 100),
                        "png",
                        Path.of(
                                        index == 0
                                                ? "target/examples/v1-layout-pack.png"
                                                : "target/examples/v1-layout-pack-"
                                                        + (index + 1)
                                                        + ".png")
                                .toFile());
            }
            var stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String lastLetterPage = stripper.getText(pdf);
            assertTrue(lastLetterPage.contains("Customer Services"));
            assertTrue(lastLetterPage.contains("Page 2 of 2"));
            stripper.setStartPage(3);
            stripper.setEndPage(3);
            String appended = stripper.getText(pdf);
            assertTrue(appended.contains("Supporting information"));
            assertFalse(appended.contains("Page 3 of"));
        }
        Files.createDirectories(Path.of("target/examples"));
        Files.write(Path.of("target/examples/v1-layout-pack.pdf"), result.pdfBytes());
    }
}
