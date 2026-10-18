package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class TableRenderingTest {
    private static final String TABLE =
            """
            {"type":"table","columnWeights":[1,1,1],
             "header":[
               {"runs":[{"text":"Energia e Estrutura Comercial"}],"backgroundColor":"#10384A","style":{"color":"#97BF6A"}},
               {"runs":[{"text":"Até 31 de dezembro de 2023"}],"backgroundColor":"#10384A","style":{"color":"#97BF6A"}},
               {"runs":[{"text":"Desde 1 de janeiro de 2024"}],"backgroundColor":"#10384A","style":{"color":"#97BF6A"}}],
             "rows":[
               [{"runs":[{"text":"Energia (€/kWh)"}]},{"runs":[{"text":"0,2008"}]},{"runs":[{"text":"0,1632"}]}],
               [{"runs":[{"text":"Potência (€/dia)"}]},{"runs":[{"text":"0,0645"}]},{"runs":[{"text":"0,0815"}]}]]}
            """;

    @Test
    void tableRendersSelectableTextAndHeaderBackgrounds() throws Exception {
        byte[] bytes = LetterPdfGenerator.builder().build().generateJson(jsonDocument(TABLE));
        String content = text(bytes);
        for (String value :
                new String[] {"Energia", "2023", "2024", "0,2008", "0,1632", "0,0645", "0,0815"}) {
            assertTrue(content.contains(value), content);
        }
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(1, pdf.getNumberOfPages());
            BufferedImage image = new PDFRenderer(pdf).renderImageWithDPI(0, 72);
            int headerPixels = 0;
            for (int y = 50; y < 130; y++) {
                for (int x = 55; x < 540; x++) {
                    if ((image.getRGB(x, y) & 0xFFFFFF) == 0x10384A) {
                        headerPixels++;
                    }
                }
            }
            assertTrue(headerPixels > 3000, "Expected a painted dark header, got " + headerPixels);
        }
    }

    @Test
    void rejectsInvalidTableGeometryAndCellValuesAtTheirFieldPaths() {
        Map<String, String> invalid =
                Map.of(
                        TABLE.replace("[1,1,1]", "[1,0,1]"), "columnWeights[1]",
                        TABLE.replace("[1,1,1]", "[]"), "columnWeights",
                        TABLE.replace("[1,1,1]", "[1,1]"), "header",
                        TABLE.replace("#10384A", "red"), "header[0].backgroundColor",
                        TABLE.replace("0,2008", "\\u0000"), "rows[0][1].runs[0].text");
        var generator = LetterPdfGenerator.builder().build();
        invalid.forEach(
                (table, expected) ->
                        assertEquals(
                                "sections[0].blocks[0]." + expected,
                                assertThrows(
                                                InvalidLetterException.class,
                                                () -> generator.generateJson(jsonDocument(table)))
                                        .fieldPath()));
    }

    private static String jsonDocument(String block) {
        return "{\"schemaVersion\":1,\"sections\":[{\"name\":\"prices\",\"blocks\":["
                + block
                + "]}]}";
    }

    @Test
    void longTablePreservesRowsAndRepeatsItsHeaderOnEveryPage() throws Exception {
        var rows = new ArrayList<List<TableCell>>();
        for (int index = 0; index < 100; index++) {
            rows.add(List.of(cell("ROW_" + index), cell("Value " + index)));
        }
        var table =
                new TableBlock(
                        List.of(1d, 1d), List.of(cell("COLUMN_A"), cell("COLUMN_B")), rows, null);
        byte[] bytes = LetterPdfGenerator.builder().build().generate(document(List.of(table)));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertTrue(pdf.getNumberOfPages() >= 3);
            var stripper = new PDFTextStripper();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                assertTrue(
                        stripper.getText(pdf).contains("COLUMN_A"),
                        "Missing header on page " + page);
            }
        }
        String content = text(bytes);
        assertEquals(100, content.lines().filter(line -> line.startsWith("ROW_")).count());
        assertTrue(content.contains("ROW_99"));
    }

    @Test
    void tableSnapshotsItsColumnsRowsAndCellContentWithoutAHeader() throws Exception {
        var weights = new ArrayList<>(List.of(1d));
        var runs = new ArrayList<>(List.of(new TextRun("ORIGINAL", null, null)));
        var row = new ArrayList<>(List.of(new TableCell(runs, null, null)));
        var rows = new ArrayList<List<TableCell>>(List.of(row));
        var table = new TableBlock(weights, null, rows, null);
        weights.clear();
        runs.clear();
        row.clear();
        rows.clear();
        assertTrue(
                text(LetterPdfGenerator.builder().build().generate(document(List.of(table))))
                        .contains("ORIGINAL"));
        assertThrows(UnsupportedOperationException.class, () -> table.rows().get(0).clear());
        assertThrows(UnsupportedOperationException.class, () -> table.columnWeights().clear());
    }

    private static TableCell cell(String content) {
        return new TableCell(List.of(new TextRun(content, null, null)), null, null);
    }
}
