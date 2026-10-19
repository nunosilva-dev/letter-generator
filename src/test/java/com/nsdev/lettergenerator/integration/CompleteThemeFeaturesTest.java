package com.nsdev.lettergenerator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.Table;
import com.nsdev.lettergenerator.internal.composition.DocumentPreparation;
import com.nsdev.lettergenerator.internal.json.JsonInput;
import com.nsdev.lettergenerator.internal.table.TableCells;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.table.Border;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.Padding;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.VerticalAlignment;
import com.nsdev.lettergenerator.theme.LetterTheme;
import com.nsdev.lettergenerator.theme.Stationery;
import com.nsdev.lettergenerator.theme.TableAppearance;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Shared appearance and stationery inheritance across every authoring path. */
class CompleteThemeFeaturesTest {
    @Test
    void sharedTableDefaultsAndHeaderPaintAreConsistentAcrossFourApis() throws Exception {
        var theme =
                LetterTheme.builder()
                        .tableAppearance(
                                new TableAppearance(
                                        new CellAppearance(
                                                null, Padding.uniform(2), null, "#EEEEEE"),
                                        new CellAppearance(null, null, null, "#005500"),
                                        Keep.ALWAYS))
                        .build();
        var typedTable =
                new TableBlock(
                        List.of(1d),
                        List.of(TableCell.text("Header")),
                        List.of(List.of(TableCell.text("Value"))),
                        null);
        var typed =
                new LetterDocument(
                                1,
                                null,
                                null,
                                List.of(new Section("Body", null, List.of(typedTable))),
                                null,
                                null,
                                null)
                        .withTheme(theme);
        var fluent =
                Letter.builder()
                        .theme(theme)
                        .table(Table.builder("Header").row("Value").build())
                        .build();
        var html =
                Letter.fromHtml(
                                "<letter><main><table><tr><th>Header</th></tr><tr><td>Value</td></tr></table></main></letter>")
                        .theme(theme)
                        .build();
        var json = JsonInput.read(new ObjectMapper().writeValueAsString(typed));
        for (var model : List.of(typed, fluent, html, json)) {
            var prepared = DocumentPreparation.prepare(model);
            var table = (TableBlock) leaves(prepared.sections().get(0).blocks()).get(0);
            assertEquals(
                    "#005500",
                    TableCells.appearance(table.options(), table.header().get(0))
                            .backgroundColor());
            assertEquals(
                    "#EEEEEE",
                    TableCells.appearance(table.options(), table.rows().get(0).get(0))
                            .backgroundColor());
            assertEquals(
                    2d,
                    TableCells.appearance(table.options(), table.header().get(0))
                            .padding()
                            .leftMm());
            assertEquals(Keep.ALWAYS, table.options().rowKeepTogether());
            assertTrue(
                    com.nsdev.lettergenerator.fixtures.PdfTestSupport.text(
                                    new LetterPdfGenerator().generate(model))
                            .contains("Value"));
        }
    }

    @Test
    void explicitTableAndCellAppearanceWinsOverThemeDefaults() {
        var theme =
                LetterTheme.builder()
                        .tableAppearance(
                                new TableAppearance(
                                        new CellAppearance(
                                                null, Padding.uniform(3), null, "#EEEEEE"),
                                        new CellAppearance(null, null, null, "#005500"),
                                        Keep.ALWAYS))
                        .build();
        var local =
                new CellAppearance(
                        Border.NONE, Padding.uniform(0), VerticalAlignment.TOP, "#FFFFFF");
        var table =
                Table.builder("Header")
                        .row("Value")
                        .cellAppearance(local)
                        .headerBackground("#FF0000")
                        .rowKeepTogether(Keep.AUTO)
                        .build();
        var prepared =
                DocumentPreparation.prepare(Letter.builder().theme(theme).table(table).build());
        var resolved = (TableBlock) leaves(prepared.sections().get(0).blocks()).get(0);
        assertEquals(
                "#FF0000",
                TableCells.appearance(resolved.options(), resolved.header().get(0))
                        .backgroundColor());
        assertEquals(
                0d,
                TableCells.appearance(resolved.options(), resolved.header().get(0))
                        .padding()
                        .leftMm());
        assertEquals(
                Border.NONE,
                TableCells.appearance(resolved.options(), resolved.rows().get(0).get(0)).border());
        assertEquals(Keep.AUTO, resolved.options().rowKeepTogether());
    }

    @Test
    void stationeryReusesRichRegionsAndLocalOverridesRetainOtherRegions() throws Exception {
        var stationery =
                Stationery.builder()
                        .header("Inherited header")
                        .footerHtml("<p><strong>Inherited footer</strong></p>")
                        .build();
        var theme = LetterTheme.builder().stationery(stationery).build();
        var typed =
                new LetterDocument(
                                1,
                                null,
                                null,
                                List.of(
                                        new Section(
                                                "Body",
                                                null,
                                                List.of(
                                                        new Paragraph(
                                                                List.of(
                                                                        new com.nsdev
                                                                                .lettergenerator
                                                                                .model.text.TextRun(
                                                                                "Body", null,
                                                                                null)),
                                                                null)))),
                                null,
                                null,
                                null)
                        .withTheme(theme);
        var fluent = Letter.builder().theme(theme).body("Body").build();
        var html = Letter.fromHtml("<letter><main>Body</main></letter>").theme(theme).build();
        var json = JsonInput.read(new ObjectMapper().writeValueAsString(typed));
        for (var model : List.of(typed, fluent, html, json)) {
            var prepared = DocumentPreparation.prepare(model);
            assertEquals(2, prepared.fixedElements().size());
            assertEquals(stationery.topZoneMm(), prepared.page().topZoneMm());
            String text =
                    com.nsdev.lettergenerator.fixtures.PdfTestSupport.text(
                            new LetterPdfGenerator().generate(model));
            assertTrue(text.contains("Inherited header"));
            assertTrue(text.contains("Inherited footer"));
            assertEquals(prepared, DocumentPreparation.prepare(prepared));
        }
        var override = Letter.builder().theme(theme).header("Local header").body("Body").build();
        String text =
                com.nsdev.lettergenerator.fixtures.PdfTestSupport.text(
                        new LetterPdfGenerator().generate(override));
        assertTrue(text.contains("Local header"));
        assertFalse(text.contains("Inherited header"));
        assertTrue(text.contains("Inherited footer"));
    }

    private static List<Block> leaves(List<Block> source) {
        return source.stream()
                .flatMap(
                        block ->
                                block instanceof GroupBlock group
                                        ? leaves(group.blocks()).stream()
                                        : java.util.stream.Stream.of(block))
                .toList();
    }
}
