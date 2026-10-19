package com.nsdev.lettergenerator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.layout.Zone;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PaginationFeaturesTest {
    @Test
    void pageFieldsRenderCurrentAndTotalPagesInRepeatedFooters() throws Exception {
        var letter =
                Letter.fromHtml(
                                """
                                <letter><main>
                                  <p>FIRST</p><page-break></page-break>
                                  <p>SECOND</p><page-break></page-break><p>THIRD</p>
                                </main><footer>
                                  <p>Page <page-number></page-number> of <page-count></page-count></p>
                                </footer></letter>
                                """)
                        .build();
        byte[] bytes = new LetterPdfGenerator().generate(letter);
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(3, pdf.getNumberOfPages());
            for (int page = 1; page <= 3; page++) {
                assertTrue(pageText(pdf, page).contains("Page " + page + " of 3"));
            }
        }
        Files.createDirectories(Path.of("target/examples"));
        Files.write(Path.of("target/examples/pagination-stationery.pdf"), bytes);
    }

    @Test
    void firstAndFollowingStationeryReplaceTheAllPageFallbackWithoutDuplicates() throws Exception {
        var letter =
                Letter.fromHtml(
                                """
                                <letter>
                                  <header><p>DEFAULT HEADER</p></header>
                                  <header pages="first"><p>FIRST HEADER</p></header>
                                  <footer pages="first"><p>FIRST FOOTER</p></footer>
                                  <footer pages="following"><p>CONTINUATION FOOTER</p></footer>
                                  <main><p>ONE</p><page-break></page-break><p>TWO</p></main>
                                </letter>
                                """)
                        .build();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(letter))) {
            assertEquals(2, pdf.getNumberOfPages());
            String first = pageText(pdf, 1);
            String following = pageText(pdf, 2);
            assertTrue(first.contains("FIRST HEADER"));
            assertTrue(first.contains("FIRST FOOTER"));
            assertFalse(first.contains("DEFAULT HEADER"));
            assertFalse(first.contains("CONTINUATION FOOTER"));
            assertTrue(following.contains("DEFAULT HEADER"));
            assertTrue(following.contains("CONTINUATION FOOTER"));
            assertFalse(following.contains("FIRST HEADER"));
            assertFalse(following.contains("FIRST FOOTER"));
        }
    }

    @Test
    void explicitGroupMovesAllItsParagraphsToTheNextPage() throws Exception {
        var letter =
                Letter.builder()
                        .body("FILLER\n".repeat(49) + "FILLER END")
                        .bodyHtml(
                                """
                                <group style="keep-together: always; font-size: 20pt">
                                  <p>GROUP FIRST</p><p>GROUP MIDDLE</p><p>GROUP LAST</p>
                                </group>
                                """)
                        .build();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(letter))) {
            assertEquals(2, pdf.getNumberOfPages());
            assertFalse(pageText(pdf, 1).contains("GROUP FIRST"));
            assertTrue(pageText(pdf, 2).contains("GROUP FIRST"));
            assertTrue(pageText(pdf, 2).contains("GROUP LAST"));
        }
    }

    @Test
    void explicitKeepWithNextMovesAnOrdinaryParagraphWithItsSuccessor() throws Exception {
        var letter =
                Letter.builder()
                        .margins(new Margins(120, 20, 120, 20))
                        .style(Style.builder().fontSizePt(20).build())
                        .body("F1\nF2\nF3\nF4")
                        .bodyHtml(
                                """
                                <p style="keep-with-next: always">KEEP THIS</p>
                                <p style="keep-together:always">NEXT1<br>NEXT2<br>NEXT3</p>
                                """)
                        .build();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(letter))) {
            assertEquals(2, pdf.getNumberOfPages());
            assertFalse(pageText(pdf, 1).contains("KEEP THIS"));
            assertTrue(pageText(pdf, 2).contains("KEEP THIS"));
            assertTrue(pageText(pdf, 2).contains("NEXT3"));
        }
    }

    @Test
    void orphanControlMovesAParagraphWhenTooFewOpeningLinesWouldFit() throws Exception {
        var letter =
                Letter.builder()
                        .margins(new Margins(120, 70, 120, 70))
                        .style(Style.builder().fontFamily("Courier").fontSizePt(20).build())
                        .body("F1\nF2\nF3\nF4")
                        .bodyHtml(
                                "<p style='orphans: 3; widows: 3'>P01xxx P02xxx P03xxx P04xxx P05xxx P06xxx P07xxx P08xxx P09xxx P10xxx P11xxx P12xxx</p>")
                        .build();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(letter))) {
            assertEquals(2, pdf.getNumberOfPages());
            assertFalse(pageText(pdf, 1).contains("P01xxx"));
            assertTrue(pageText(pdf, 2).contains("P01xxx"));
            assertTrue(pageText(pdf, 2).contains("P12xxx"));
        }
    }

    @Test
    void invalidLineCountsAndDuplicateStationerySelectionsAreRejected() {
        for (String property : new String[] {"widows: 0", "orphans: 1.5", "keep-together: wrong"}) {
            assertThrows(
                    InvalidLetterException.class,
                    () ->
                            Letter.fromHtml(
                                            "<letter><main><p style='"
                                                    + property
                                                    + "'>X</p></main></letter>")
                                    .build());
        }
        assertThrows(
                InvalidLetterException.class,
                () ->
                        Letter.fromHtml(
                                        "<letter><header pages='first'>A</header><header pages='FIRST'>B</header><main><p>X</p></main></letter>")
                                .build());
    }

    @Test
    void pageFieldsWorkThroughFluentTypedAndJsonInputs() throws Exception {
        var counter =
                new Paragraph(
                        List.of(
                                new TextRun("Page ", null, null),
                                TextRun.currentPage(),
                                new TextRun(" of ", null, null),
                                TextRun.pageCount()),
                        null);
        var fluent =
                Letter.builder()
                        .body("FIRST", Letter.PAGE_BREAK, "SECOND")
                        .footerBlocks(counter)
                        .build();
        var typed =
                new LetterDocument(
                        1,
                        new PageSpec(null, null, 0d, 10d, 0d),
                        null,
                        List.of(
                                new Section(
                                        "Body",
                                        null,
                                        List.of(
                                                new Paragraph(
                                                        List.of(new TextRun("FIRST", null, null)),
                                                        null),
                                                new PageBreakBlock(),
                                                new Paragraph(
                                                        List.of(new TextRun("SECOND", null, null)),
                                                        null)))),
                        List.of(
                                new FixedElement(
                                        "counter",
                                        Zone.BOTTOM,
                                        PageSelector.ALL,
                                        new Bounds(0, 0, 170, 10),
                                        List.of(counter))),
                        null,
                        null);
        String json =
                """
                {"schemaVersion":1,"page":{"bottomZoneMm":10},
                 "sections":[{"name":"Body","blocks":[
                   {"type":"paragraph","runs":[{"text":"FIRST"}]},{"type":"pageBreak"},
                   {"type":"paragraph","runs":[{"text":"SECOND"}]}]}],
                 "fixedElements":[{"id":"counter","zone":"BOTTOM","pages":"ALL",
                   "boundsMm":{"x":0,"y":0,"width":170,"height":10},"blocks":[
                     {"type":"paragraph","runs":[{"text":"Page "},{"text":"","pageNumber":"CURRENT"},
                       {"text":" of "},{"text":"","pageNumber":"TOTAL"}]}]}]}
                """;
        var generator = new LetterPdfGenerator();
        for (byte[] bytes :
                List.of(
                        generator.generate(fluent),
                        generator.generate(typed),
                        generator.generateJson(json))) {
            try (var pdf = Loader.loadPDF(bytes)) {
                assertEquals(2, pdf.getNumberOfPages());
                assertTrue(pageText(pdf, 1).contains("Page 1 of 2"));
                assertTrue(pageText(pdf, 2).contains("Page 2 of 2"));
            }
        }
    }

    @Test
    void explicitEmptyContinuationHeaderSuppressesFallback() throws Exception {
        var document =
                Letter.builder()
                        .header("FIRST ONLY")
                        .headerBlocks(PageSelector.FOLLOWING)
                        .body("First", Letter.PAGE_BREAK, "Following")
                        .build();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(document))) {
            assertTrue(pageText(pdf, 1).contains("FIRST ONLY"));
            assertFalse(pageText(pdf, 2).contains("FIRST ONLY"));
        }
    }

    @Test
    void pageTotalsAndDiagnosticsIncludeARecoveredSignaturePage() throws Exception {
        var counter =
                new Paragraph(
                        List.of(
                                new TextRun("Page ", null, null),
                                TextRun.currentPage(),
                                new TextRun(" of ", null, null),
                                TextRun.pageCount()),
                        null);
        var model =
                new LetterDocument(
                        1,
                        new PageSpec(null, null, 0d, 10d, 245d),
                        null,
                        List.of(
                                new Section(
                                        "Body",
                                        null,
                                        List.of(
                                                new Paragraph(
                                                        List.of(new TextRun("BODY", null, null)),
                                                        Style.builder().fontSizePt(45).build())))),
                        List.of(
                                new FixedElement(
                                        "counter",
                                        Zone.BOTTOM,
                                        PageSelector.ALL,
                                        new Bounds(0, 0, 170, 10),
                                        List.of(counter))),
                        new Signature(
                                SignaturePlacement.BOTTOM_LAST,
                                List.of(
                                        new Paragraph(
                                                List.of(new TextRun("SIGNED", null, null)), null)),
                                new Bounds(0, 0, 100, 20)),
                        null);
        var result = new LetterPdfGenerator().render(model);
        assertEquals(2, result.pageCount());
        assertTrue(
                result.diagnostics().stream()
                        .anyMatch(item -> item.code().equals("SIGNATURE_PAGE_RECOVERY")));
        try (var pdf = Loader.loadPDF(result.pdfBytes())) {
            assertTrue(pageText(pdf, 1).contains("Page 1 of 2"));
            assertTrue(pageText(pdf, 2).contains("Page 2 of 2"));
            assertTrue(pageText(pdf, 2).contains("SIGNED"));
        }
    }

    @Test
    void widowControlLeavesEnoughTrailingWrappedLines() throws Exception {
        var model =
                Letter.builder()
                        .margins(new Margins(120, 70, 120, 70))
                        .style(Style.builder().fontFamily("Courier").fontSizePt(20).build())
                        .body("F1\nF2")
                        .bodyHtml(
                                "<p style='widows:3;orphans:1'>P01xxx P02xxx P03xxx P04xxx P05xxx P06xxx P07xxx P08xxx P09xxx P10xxx P11xxx P12xxx</p>")
                        .build();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(model))) {
            assertEquals(2, pdf.getNumberOfPages());
            assertTrue(pageText(pdf, 1).contains("P06xxx"));
            assertFalse(pageText(pdf, 1).contains("P07xxx"));
            assertTrue(pageText(pdf, 2).contains("P07xxx"));
            assertTrue(pageText(pdf, 2).contains("P12xxx"));
        }
    }

    private static String pageText(org.apache.pdfbox.pdmodel.PDDocument pdf, int page)
            throws Exception {
        var reader = new PDFTextStripper();
        reader.setStartPage(page);
        reader.setEndPage(page);
        return reader.getText(pdf).replaceAll("\\s+", " ");
    }
}
