package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Public-API regressions for defects identified during the 1.0.0 release review. */
class ReleaseReadinessTest {
    @ParameterizedTest
    @CsvSource({"1,0,2,2", "2,0,3,3", "0,1,2,1", "1,1,3,2"})
    void boxedPageBreaksPreserveExplicitBlankPages(
            int leading, int trailing, int expectedPages, int textPage) throws Exception {
        var blocks = new ArrayList<Block>();
        for (int index = 0; index < leading; index++) {
            blocks.add(new PageBreakBlock());
        }
        blocks.add(paragraph("BOXED TEXT"));
        for (int index = 0; index < trailing; index++) {
            blocks.add(new PageBreakBlock());
        }
        var model = Letter.builder().block(new BoxBlock(blocks, null, null)).build();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(model))) {
            assertEquals(expectedPages, pdf.getNumberOfPages());
            var stripper = new PDFTextStripper();
            stripper.setStartPage(textPage);
            stripper.setEndPage(textPage);
            assertTrue(stripper.getText(pdf).contains("BOXED TEXT"));
        }
    }

    @Test
    void htmlCustomPaperKeepsRequestedLandscapeOrientation() throws Exception {
        var model =
                Letter.fromHtml(
                                """
                <letter page-width-mm="180" page-height-mm="240" orientation="landscape">
                  <main><p>LANDSCAPE LETTER</p></main>
                </letter>
                """)
                        .build();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(model))) {
            assertEquals(680.315, pdf.getPage(0).getMediaBox().getWidth(), 0.01);
            assertEquals(510.236, pdf.getPage(0).getMediaBox().getHeight(), 0.01);
        }
    }

    @Test
    void rootLocalePlaceholdersApplyBeforeFormattingAndPreparedBinding() throws Exception {
        var builder =
                Letter.template(
                                """
                <letter language-tag="${language}" currency="${currency}"
                        date-pattern="${pattern}" date-style="${dateStyle}" zone-id="${zone}">
                  <main><p><param key="date" format="date"/> / <param key="amount" format="currency"/></p></main>
                </letter>
                """)
                        .values(
                                Map.of(
                                        "language",
                                        "en-GB",
                                        "currency",
                                        "EUR",
                                        "pattern",
                                        "dd/MM/yyyy",
                                        "dateStyle",
                                        "short",
                                        "zone",
                                        "UTC",
                                        "date",
                                        LocalDate.of(2026, 10, 20),
                                        "amount",
                                        1234.5));
        assertTrue(builder.validate().isValid());
        var prepared = builder.compile();
        assertTrue(prepared.validate().isValid());
        String output = text(new LetterPdfGenerator().generate(prepared.build()));
        assertTrue(output.contains("20/10/2026"), output);
        assertTrue(output.contains("1,234.50"), output);
        var rebound =
                prepared.bind(
                        Map.of(
                                "language",
                                "en-US",
                                "currency",
                                "USD",
                                "pattern",
                                "MM-dd-yyyy",
                                "dateStyle",
                                "long",
                                "zone",
                                "America/New_York",
                                "date",
                                LocalDate.of(2026, 10, 20),
                                "amount",
                                1234.5));
        assertTrue(text(new LetterPdfGenerator().generate(rebound)).contains("10-20-2026"));
    }

    @Test
    void missingLocaleBindingsDoNotHideOtherMissingValuesOrAssets() {
        var report =
                Letter.template(
                                """
                <letter language-tag="${language}"><main><p>${body}</p>
                  <img src="asset:missing" width="10mm"/>
                </main></letter>
                """)
                        .validate();
        assertFalse(report.isValid());
        assertEquals(
                2,
                report.diagnostics().stream()
                        .filter(d -> d.code().equals("MISSING_VARIABLE"))
                        .count());
        assertEquals(
                1,
                report.diagnostics().stream()
                        .filter(d -> d.code().equals("MISSING_ASSET"))
                        .count());
    }

    @Test
    void bindingRootAttributesDoesNotRecursivelyExpandReplacementText() throws Exception {
        var model =
                Letter.template(
                                """
                <letter date-pattern="'${literal}' yyyy"><main><p><param key="date" format="date"/></p></main></letter>
                """)
                        .values(
                                Map.of(
                                        "literal",
                                        "${untouched}",
                                        "untouched",
                                        "EXPANDED",
                                        "date",
                                        LocalDate.of(2026, 10, 20)))
                        .build();
        String output = text(new LetterPdfGenerator().generate(model));
        assertTrue(output.contains("${untouched} 2026"), output);
        assertFalse(output.contains("EXPANDED"), output);
    }
}
