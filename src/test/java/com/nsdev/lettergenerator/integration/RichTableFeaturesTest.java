package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.png;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.Table;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.Border;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.Padding;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.TableOptions;
import com.nsdev.lettergenerator.model.table.TableRowGroup;
import com.nsdev.lettergenerator.model.table.TableRowPolicy;
import com.nsdev.lettergenerator.model.table.VerticalAlignment;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/** Acceptance coverage for merged cells, block content and explicit table appearance. */
class RichTableFeaturesTest {
    @Test
    void mergedCellsAndRichBlocksProduceSelectableText() throws Exception {
        byte[] pdf =
                render(
                        """
                <table column-weights="1,1,1">
                  <thead><tr><th colspan="2">Merged header</th><th>Third</th></tr></thead>
                  <tbody>
                    <tr><td rowspan="2"><p>First paragraph</p><p>Second paragraph</p></td>
                        <td colspan="2"><ul><li>Listed value</li></ul></td></tr>
                    <tr><td>Lower middle</td><td>Lower right</td></tr>
                  </tbody>
                </table>
                """);
        String content = text(pdf);
        for (String expected :
                new String[] {
                    "Merged header",
                    "First paragraph",
                    "Second paragraph",
                    "Listed value",
                    "Lower middle",
                    "Lower right"
                }) {
            assertTrue(content.contains(expected), content);
        }
    }

    @Test
    void tableDefaultsAndCellOverridesControlAppearanceAndRowKeeps() throws Exception {
        var table =
                Letter.fromHtml(
                                """
                <letter><main><table style="padding:3mm; border:0.5pt solid #123456; vertical-align:top"
                      row-keep="auto">
                  <tbody keep-together="always"><tr><td style="padding:1mm 2mm; vertical-align:bottom">A</td>
                  <td style="border:none">B</td></tr><tr row-keep="always"><td>C</td><td>D</td></tr></tbody>
                </table></main></letter>
                """)
                        .build();
        assertTrue(text(LetterPdfGenerator.builder().build().generate(table)).contains("A"));
        assertEquals(1, table.sections().get(0).blocks().size());
    }

    @Test
    void imagesUseTheMergedCellInnerWidth() throws Exception {
        var letter =
                Letter.fromHtml(
                                """
                <letter><main><table column-weights="1,1,1" style="padding:2mm">
                  <tr><td colspan="2"><img src="asset:logo" width="100%"/></td><td>Image neighbour</td></tr>
                </table></main></letter>
                """)
                        .asset("logo", png())
                        .build();
        var table = (TableBlock) letter.sections().get(0).blocks().get(0);
        var image = (ImageBlock) table.rows().get(0).get(0).blocks().get(0);
        assertEquals(109.156944, image.widthMm(), 0.000001);
        byte[] pdf = LetterPdfGenerator.builder().build().generate(letter);
        assertTrue(text(pdf).contains("Image neighbour"));
    }

    @Test
    void javaBuilderPreservesRichCellOptionsAndSnapshotsRows() throws Exception {
        var source =
                new java.util.ArrayList<>(
                        List.of(
                                TableCell.content(
                                                List.of(
                                                        new Paragraph(
                                                                List.of(
                                                                        new TextRun(
                                                                                "Rich Java",
                                                                                null,
                                                                                null)),
                                                                null)))
                                        .span(2, 1)));
        var table =
                Table.builder("One", "Two")
                        .headerCells(TableCell.text("Wide header").span(2, 1))
                        .cellAppearance(
                                new CellAppearance(
                                        Border.NONE,
                                        Padding.uniform(2),
                                        VerticalAlignment.TOP,
                                        "#123456"))
                        .rowKeepTogether(Keep.AUTO)
                        .row(source)
                        .rowGroup(new TableRowGroup(0, 1, Keep.PREFER))
                        .build();
        source.clear();
        byte[] pdf =
                LetterPdfGenerator.builder()
                        .build()
                        .generate(Letter.builder().table(table).build());
        assertTrue(text(pdf).contains("Rich Java"));
        try (var document = Loader.loadPDF(pdf)) {
            var image = new PDFRenderer(document).renderImageWithDPI(0, 72);
            int filled = 0;
            for (int y = 50; y < 150; y++) {
                for (int x = 55; x < 540; x++) {
                    if ((image.getRGB(x, y) & 0xFFFFFF) == 0x123456) {
                        filled++;
                    }
                }
            }
            assertTrue(filled > 1000, "Expected inherited dark cell fill");
        }
    }

    @Test
    void jsonAcceptsRichCellsAndSpanGeometry() throws Exception {
        byte[] pdf =
                LetterPdfGenerator.builder()
                        .build()
                        .generateJson(
                                """
                {"schemaVersion":1,"sections":[{"name":"data","blocks":[
                  {"type":"table","columnWeights":[1,1],
                   "options":{"cellAppearance":{"padding":{"topMm":2,"rightMm":3,"bottomMm":2,"leftMm":3},
                         "verticalAlignment":"TOP"},"rowKeepTogether":"AUTO"},
                   "rows":[[{"columnSpan":2,"blocks":[{"type":"paragraph","runs":[{"text":"Rich JSON"}]}]}]]}
                ]}]}
                """);
        assertTrue(text(pdf).contains("Rich JSON"));
    }

    @Test
    void fullyOccupiedContinuationRowsRemainValid() throws Exception {
        assertTrue(
                text(render(
                                """
                <table><tr><td rowspan="2"><p>Spanning every column</p></td></tr><tr></tr></table>
                """))
                        .contains("Spanning every column"));
    }

    @Test
    void rowGroupsMoveTheirMembersTogether() throws Exception {
        var letter =
                Letter.builder()
                        .margins(new Margins(120, 20, 120, 20))
                        .style(Style.builder().fontSizePt(20).build())
                        .body("F1\nF2\nF3\nF4")
                        .bodyHtml(
                                """
                        <table><tbody keep-together="always">
                          <tr><td>GROUP FIRST<br>GROUP DETAIL</td></tr><tr><td>GROUP LAST</td></tr>
                        </tbody></table>
                        """)
                        .build();
        try (var pdf = Loader.loadPDF(LetterPdfGenerator.builder().build().generate(letter))) {
            assertEquals(2, pdf.getNumberOfPages());
            var stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            assertFalse(stripper.getText(pdf).contains("GROUP FIRST"));
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String content = stripper.getText(pdf);
            assertTrue(content.contains("GROUP FIRST"), content);
            assertTrue(content.contains("GROUP LAST"), content);
        }
    }

    @Test
    void cellsRejectConflictingContentAndImagesWiderThanTheirInnerSpan() {
        var both =
                new TableCell(
                        List.of(new TextRun("inline", null, null)),
                        null,
                        null,
                        List.of(new Paragraph(List.of(new TextRun("block", null, null)), null)),
                        null,
                        null,
                        null);
        var invalid = new TableBlock(List.of(1d), List.of(), List.of(List.of(both)), null);
        assertEquals(
                "sections[0].blocks[0].rows[0][0]",
                assertThrows(
                                InvalidLetterException.class,
                                () ->
                                        LetterPdfGenerator.builder()
                                                .build()
                                                .generate(Letter.builder().table(invalid).build()))
                        .fieldPath());
        assertThrows(
                InvalidLetterException.class,
                () ->
                        Letter.fromHtml(
                                        """
                <letter><main><table style="padding:2mm" column-weights="1,1"><tr>
                  <td><img src="asset:logo" width="100mm"/></td><td>Narrow</td>
                </tr></table></main></letter>
                """)
                                .asset("logo", png())
                                .build());
    }

    @Test
    void splittableRichRowsRepeatTheirSpannedHeaderOnFollowingPages() throws Exception {
        String lines =
                java.util.stream.IntStream.range(0, 90)
                        .mapToObj(index -> "Rich line " + index)
                        .collect(java.util.stream.Collectors.joining("<br>"));
        byte[] bytes =
                render(
                        "<table column-weights='1,1' row-keep='auto'>"
                                + "<thead><tr><th colspan='2'>REPEATED RICH HEADER</th></tr></thead>"
                                + "<tr><td colspan='2'><p>"
                                + lines
                                + "</p></td></tr></table>");
        try (var pdf = Loader.loadPDF(bytes)) {
            assertTrue(pdf.getNumberOfPages() >= 2);
            var stripper = new PDFTextStripper();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                assertTrue(stripper.getText(pdf).contains("REPEATED RICH HEADER"));
            }
        }
        assertTrue(text(bytes).contains("Rich line 89"));
    }

    @Test
    void invalidAndOverlappingBodyRowPoliciesHaveFieldPaths() {
        var rows = List.of(List.of(TableCell.text("First")), List.of(TableCell.text("Second")));
        var invalidOptions =
                List.of(
                        new TableOptions(
                                null, null, List.of(new TableRowPolicy(2, Keep.AUTO, null)), null),
                        new TableOptions(
                                null,
                                null,
                                List.of(
                                        new TableRowPolicy(0, Keep.AUTO, null),
                                        new TableRowPolicy(0, Keep.ALWAYS, null)),
                                null),
                        new TableOptions(
                                null,
                                null,
                                null,
                                List.of(
                                        new TableRowGroup(0, 2, Keep.ALWAYS),
                                        new TableRowGroup(1, 1, Keep.PREFER))),
                        new TableOptions(
                                null, null, null, List.of(new TableRowGroup(0, 3, Keep.PREFER))));
        for (var options : invalidOptions) {
            var table = new TableBlock(List.of(1d), List.of(), rows, null, options);
            var error =
                    assertThrows(
                            InvalidLetterException.class,
                            () ->
                                    LetterPdfGenerator.builder()
                                            .build()
                                            .generate(Letter.builder().table(table).build()));
            assertTrue(error.fieldPath().contains(".options.row"), error.fieldPath());
        }
    }

    @Test
    void invalidSpanCoverageOverflowAndHeaderCrossingAreRejected() {
        String[] invalid = {
            "<table column-weights='1,1'><tr><td colspan='3'>Overflow</td></tr></table>",
            "<table column-weights='1,1'><tr><td>A</td><td>B</td></tr><tr><td>Hole</td></tr></table>",
            "<table column-weights='1,1'><tr><td rowspan='2'>Too far</td><td>B</td></tr></table>",
            "<table column-weights='1,1'><thead><tr><th rowspan='2'>Crossing</th><th>B</th></tr></thead><tr><td>A</td><td>B</td></tr></table>",
            "<table><tr><td colspan='0'>Zero</td></tr></table>",
            "<table column-weights='1,1,1'><tr><td>A</td><td rowspan='2'>B</td><td>C</td></tr><tr><td colspan='2'>Overlap</td></tr></table>"
        };
        for (String table : invalid) {
            assertThrows(InvalidLetterException.class, () -> render(table), table);
        }
    }

    @Test
    void nestedTablesAndPageBreaksRemainForbiddenInCells() {
        for (String content :
                new String[] {"<table><tr><td>Nested</td></tr></table>", "<page-break/>"}) {
            assertThrows(
                    InvalidLetterException.class,
                    () -> render("<table><tr><td>" + content + "</td></tr></table>"));
        }
    }

    private static byte[] render(String table) {
        return LetterPdfGenerator.builder()
                .build()
                .generate(Letter.fromHtml("<letter><main>" + table + "</main></letter>").build());
    }
}
