package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.renderFirst;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.component.LetterComponent;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.render.RenderOptions;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/** A complete synthetic letter demonstrating composition features together. */
class CompositionExampleTest {
    @Test
    void combinedFeaturesProduceAnInspectableMultipageLetterAndDebugPreview() throws Exception {
        var rows = new StringBuilder();
        for (int row = 1; row <= 50; row++) {
            rows.append("<tr><td>Service period ")
                    .append(row)
                    .append("</td><td>Standard supply</td><td>EUR 12.50</td></tr>");
        }
        String source =
                """
                <letter header-height-mm="25" footer-height-mm="12">
                  <header pages="first"><p style="font-size:16pt;font-weight:bold">EXAMPLE UTILITIES</p>
                    <p>Customer Services<br>100 Example Avenue, Sample City</p></header>
                  <header pages="following"><p>EXAMPLE UTILITIES | Account statement continued</p></header>
                  <main>
                    <group><h1 style-name="title">Your account statement</h1>
                      <p>Dear ${recipient},</p><p>Please find the details of your service below.</p></group>
                    <table column-weights="2,2,1" style="border:0.4pt solid #B8C9CF;padding:2mm;vertical-align:top">
                      <thead style="background-color:#10384A;color:white"><tr><th>Period</th><th>Description</th><th>Amount</th></tr></thead>
                      <tbody keep-together="always">
                        <tr><td rowspan="2"><p style="font-weight:bold">Account summary</p><p>Reference EX-1001</p></td>
                            <td colspan="2"><p>All data in this letter is fictional.</p></td></tr>
                        <tr><td><ul><li>Electricity</li><li>Customer service</li></ul></td><td>Included</td></tr>
                      </tbody><tbody>%s</tbody>
                    </table>
                    <group style="keep-together:always"><h2>Questions about your statement?</h2>
                      <p>Contact our customer services team using the details below.</p></group>
                  </main>
                  <footer><p>Page <page-number></page-number> of <page-count></page-count></p>
                    <component ref="legal"></component></footer>
                  <signature placement="bottom-last" height-mm="25"><p>Kind regards,<br>Example Customer Services</p></signature>
                </letter>
                """
                        .formatted(rows);
        var theme =
                LetterTheme.builder()
                        .body(
                                Style.builder()
                                        .fontSizePt(10)
                                        .color("#24363D")
                                        .spaceAfterMm(3)
                                        .build())
                        .heading(Style.builder().fontSizePt(13).bold().build())
                        .named("title", Style.builder().fontSizePt(18).color("#10384A").build())
                        .header(Style.builder().fontSizePt(9).build())
                        .footer(Style.builder().fontSizePt(8).spaceAfterMm(0).build())
                        .signature(Style.builder().fontSizePt(10).build())
                        .tableHeader(Style.builder().fontSizePt(9).bold().build())
                        .tableCell(Style.builder().fontSizePt(9).spaceAfterMm(0).build())
                        .build();
        var template =
                Letter.template(source)
                        .values(Map.of("recipient", "Alex Example"))
                        .theme(theme)
                        .component(
                                "legal",
                                LetterComponent.text(
                                        "Example Utilities | example.invalid | Synthetic demonstration"));
        var generator = new LetterPdfGenerator();
        assertTrue(template.validate(generator).isValid());
        var model = template.build();
        var result = generator.render(model);
        var debug = generator.render(model, RenderOptions.debugPreview());
        assertTrue(result.pageCount() >= 2);
        assertEquals(result.pageCount(), debug.pageCount());
        assertTrue(debug.debugBoxes().size() > result.pageCount());
        try (var pdf = Loader.loadPDF(result.pdfBytes())) {
            var text = new PDFTextStripper();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                text.setStartPage(page);
                text.setEndPage(page);
                String content = text.getText(pdf);
                assertTrue(content.contains("Page " + page + " of " + result.pageCount()));
                if (page == pdf.getNumberOfPages()) {
                    assertTrue(content.contains("Kind regards,"));
                }
            }
        }
        Files.createDirectories(Path.of("target/examples"));
        Files.write(Path.of("target/examples/composition-features.pdf"), result.pdfBytes());
        Files.write(Path.of("target/examples/composition-debug.pdf"), debug.pdfBytes());
        renderFirst(result.pdfBytes(), "composition-features");
        renderFirst(debug.pdfBytes(), "composition-debug");
    }
}
