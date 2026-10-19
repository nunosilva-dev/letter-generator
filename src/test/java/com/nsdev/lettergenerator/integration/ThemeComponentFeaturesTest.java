package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.component.LetterComponent;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.composition.DocumentPreparation;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.ComponentBlock;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import com.nsdev.lettergenerator.theme.LetterTheme;
import com.nsdev.lettergenerator.theme.StyleRole;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

/** Acceptance coverage for per-document theme resolution and reusable component expansion. */
class ThemeComponentFeaturesTest {
    @Test
    void nestedInlineNamedStylesRetainEachAncestorAndChildPrecedence() {
        var theme =
                LetterTheme.builder()
                        .named("red", Style.builder().color("#FF0000").build())
                        .named("bold", Style.builder().bold().build())
                        .build();
        var document =
                Letter.fromHtml(
                                """
                <letter><main>
                  <p><span style-name="red"><span style-name="bold">Inherited red</span></span></p>
                  <p><span style="color:green;font-weight:bold"><span style-name="red">Child red</span></span></p>
                </main></letter>
                """)
                        .theme(theme)
                        .build();
        var prepared = DocumentPreparation.prepare(document);
        for (var block : leaves(prepared.sections().get(0).blocks())) {
            var run = ((Paragraph) block).runs().get(0);
            assertEquals("#FF0000", run.style().color());
            assertEquals(Boolean.TRUE, run.style().bold());
        }
    }

    @Test
    void unknownAncestorInlineNamedStylesAreStillValidated() {
        var document =
                Letter.fromHtml(
                                """
                <letter><main><p><span style-name="missing"><span style-name="bold">Text</span></span></p></main></letter>
                """)
                        .theme(
                                LetterTheme.builder()
                                        .named("bold", Style.builder().bold().build())
                                        .build())
                        .build();
        assertThrows(InvalidLetterException.class, () -> DocumentPreparation.prepare(document));
    }

    @Test
    void tableSectionRowAndCellNamedStylesRetainTheirIndependentLayers() {
        var document =
                Letter.fromHtml(
                                """
                <letter><main><table><tbody style-name="red"><tr style-name="bold">
                  <td style-name="large">Layered cell</td>
                </tr></tbody></table></main></letter>
                """)
                        .theme(
                                LetterTheme.builder()
                                        .named("red", Style.builder().color("#FF0000").build())
                                        .named("bold", Style.builder().bold().build())
                                        .named("large", Style.builder().fontSizePt(22).build())
                                        .build())
                        .build();
        var table =
                (TableBlock)
                        leaves(DocumentPreparation.prepare(document).sections().get(0).blocks())
                                .get(0);
        var style = table.rows().get(0).get(0).style();
        assertEquals("#FF0000", style.color());
        assertEquals(Boolean.TRUE, style.bold());
        assertEquals(22d, style.fontSizePt());
    }

    @Test
    void retainedStylesWithoutAThemeResolveTypographyAndPaginationBeforeRendering()
            throws Exception {
        var base = Style.builder().fontSizePt(17).keepTogether(Keep.ALWAYS).build();
        var layered =
                new Style(
                        null, null, "#FF0000", null, null, null, null, null, null, null, null, null,
                        null, null, null, base);
        var document =
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
                                                                new TextRun(
                                                                        "Layered model",
                                                                        null,
                                                                        null)),
                                                        layered)))),
                        null,
                        null,
                        null);
        var prepared = DocumentPreparation.prepare(document);
        var style = prepared.sections().get(0).blocks().get(0).style();
        assertEquals(17d, style.fontSizePt());
        assertEquals(Keep.ALWAYS, style.keepTogether());
        assertEquals("#FF0000", style.color());
        assertTrue(
                samples(new LetterPdfGenerator().generate(document)).stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Layered model")
                                                && sample.size() == 17));
    }

    @Test
    void htmlNamedHeadingStylesOverrideSemanticSizesAndLocalCssWins() throws Exception {
        LetterTheme theme =
                LetterTheme.builder()
                        .body(Style.builder().fontSizePt(15).build())
                        .heading(Style.builder().fontSizePt(18).build())
                        .named("title", Style.builder().fontSizePt(22).build())
                        .build();
        LetterDocument document =
                Letter.fromHtml(
                                """
                <letter><main>
                  <h1 style-name="title">Named heading</h1>
                  <h2 style-name="title" style="font-size:17pt">Local heading</h2>
                  <p>Theme paragraph</p>
                </main></letter>
                """)
                        .theme(theme)
                        .build();
        var samples = samples(LetterPdfGenerator.builder().build().generate(document));
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Named heading")
                                                && sample.size() == 22),
                samples.toString());
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Local heading")
                                                && sample.size() == 17),
                samples.toString());
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Theme paragraph")
                                                && sample.size() == 15),
                samples.toString());
    }

    @Test
    void htmlComponentReferencesUseHeaderFooterAndSignatureRoleDefaults() throws Exception {
        LetterTheme theme =
                LetterTheme.builder()
                        .body(Style.builder().fontSizePt(15).build())
                        .header(Style.builder().fontSizePt(10).build())
                        .footer(Style.builder().fontSizePt(8).build())
                        .signature(Style.builder().fontSizePt(12).build())
                        .build();
        LetterDocument document =
                Letter.fromHtml(
                                """
                <letter><header><component ref="header"></component></header>
                  <main><p>Body content</p></main>
                  <footer><component ref="footer"></component></footer>
                  <signature><component ref="signature"></component></signature>
                </letter>
                """)
                        .theme(theme)
                        .build()
                        .withComponents(
                                Map.of(
                                        "header", LetterComponent.text("Shared header"),
                                        "footer", LetterComponent.text("Shared footer"),
                                        "signature", LetterComponent.text("Shared signature")));
        var samples = samples(LetterPdfGenerator.builder().build().generate(document));
        for (Sample expected :
                List.of(
                        new Sample("Shared header", 10, ""),
                        new Sample("Shared footer", 8, ""),
                        new Sample("Shared signature", 12, ""))) {
            assertTrue(
                    samples.stream()
                            .anyMatch(
                                    sample ->
                                            sample.text().contains(expected.text())
                                                    && sample.size() == expected.size()),
                    samples.toString());
        }
    }

    @Test
    void javaCompositionSnapshotsCallerCollectionsAndCanBePreparedRepeatedly() throws Exception {
        List<Block> content =
                new ArrayList<>(
                        List.of(
                                new Paragraph(
                                        List.of(new TextRun("Owned reusable text", null, null)),
                                        Style.builder().named("notice").bold(false).build())));
        Map<String, Style> names = new LinkedHashMap<>();
        names.put("notice", Style.builder().fontSizePt(17).bold().build());
        Map<StyleRole, Style> roles = new LinkedHashMap<>();
        roles.put(StyleRole.BODY, Style.builder().fontSizePt(20).build());
        LetterTheme theme = new LetterTheme(roles, names);
        Map<String, LetterComponent> components = new LinkedHashMap<>();
        components.put("notice", new LetterComponent(content));
        LetterDocument document =
                new LetterDocument(
                                1,
                                null,
                                null,
                                List.of(
                                        new Section(
                                                "Body",
                                                null,
                                                List.of(new ComponentBlock("notice", null)))),
                                null,
                                null,
                                null)
                        .withTheme(theme)
                        .withComponents(components);
        content.clear();
        names.clear();
        roles.clear();
        components.clear();
        LetterDocument prepared = DocumentPreparation.prepare(document);
        byte[] pdf =
                LetterPdfGenerator.builder()
                        .build()
                        .generate(DocumentPreparation.prepare(prepared));
        assertTrue(text(pdf).contains("Owned reusable text"));
        assertTrue(
                samples(pdf).stream()
                        .anyMatch(
                                sample -> sample.size() == 17 && !sample.font().contains("Bold")));
        assertThrows(UnsupportedOperationException.class, () -> document.components().clear());
        assertThrows(UnsupportedOperationException.class, () -> theme.namedStyles().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> document.components().get("notice").blocks().clear());
    }

    @Test
    void jsonThemesApplyRoleAndNamedDefaultsBelowExplicitOverrides() throws Exception {
        byte[] pdf =
                LetterPdfGenerator.builder()
                        .build()
                        .generateJson(
                                """
                {"schemaVersion":1,
                 "theme":{"roles":{"BODY":{"fontSizePt":19,"bold":true}},
                          "namedStyles":{"small":{"fontSizePt":13,"bold":true}}},
                 "sections":[{"name":"Body","blocks":[
                    {"type":"paragraph","runs":[{"text":"Role text"}]},
                    {"type":"paragraph","style":{"styleName":"small","bold":false,
                      "spaceAfterMm":0},"runs":[{"text":"Explicit text"}]}]}]}
                """);
        var samples = samples(pdf);
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Role text")
                                                && sample.size() == 19
                                                && sample.font().contains("Bold")),
                samples.toString());
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Explicit text")
                                                && sample.size() == 13
                                                && !sample.font().contains("Bold")),
                samples.toString());
    }

    @Test
    void jsonComponentsExpandNestedReferencesInReadingOrder() throws Exception {
        byte[] pdf =
                LetterPdfGenerator.builder()
                        .build()
                        .generateJson(
                                """
                {"schemaVersion":1,"components":{
                   "address":{"blocks":[{"type":"paragraph","runs":[{"text":"Shared address"}]}]},
                   "letterhead":{"blocks":[{"type":"component","name":"address"},
                      {"type":"paragraph","runs":[{"text":"Shared legal text"}]}]}},
                 "sections":[{"name":"Body","blocks":[
                   {"type":"component","name":"letterhead"},
                   {"type":"paragraph","runs":[{"text":"Letter body"}]},
                   {"type":"component","name":"address"}]}]}
                """);
        String content = text(pdf);
        assertEquals(2, content.split("Shared address", -1).length - 1, content);
        assertTrue(
                content.indexOf("Shared address") < content.indexOf("Shared legal text"), content);
        assertTrue(content.indexOf("Shared legal text") < content.indexOf("Letter body"), content);
    }

    @Test
    void unknownNamedStylesAndComponentsHaveUsefulValidationErrors() {
        for (String block :
                new String[] {
                    "{\"type\":\"paragraph\",\"style\":{\"styleName\":\"missing-style\"},\"runs\":[{\"text\":\"Text\"}]}",
                    "{\"type\":\"component\",\"name\":\"missing-component\"}"
                }) {
            var error =
                    assertThrows(
                            InvalidLetterException.class,
                            () ->
                                    LetterPdfGenerator.builder()
                                            .build()
                                            .generateJson(
                                                    "{\"schemaVersion\":1,\"sections\":[{\"name\":\"Body\",\"blocks\":["
                                                            + block
                                                            + "]}]}"));
            assertTrue(error.getMessage().contains("Unknown"), error.getMessage());
            assertTrue(error.fieldPath().contains("blocks[0]"), error.fieldPath());
        }
    }

    @Test
    void componentCyclesAreRejectedBeforeRendering() {
        var error =
                assertThrows(
                        InvalidLetterException.class,
                        () ->
                                LetterPdfGenerator.builder()
                                        .build()
                                        .generateJson(
                                                """
                    {"schemaVersion":1,"components":{
                      "first":{"blocks":[{"type":"component","name":"second"}]},
                      "second":{"blocks":[{"type":"component","name":"first"}]}},
                     "sections":[{"name":"Body","blocks":[{"type":"component","name":"first"}]}]}
                    """));
        assertTrue(error.getMessage().contains("cycle"), error.getMessage());
        assertTrue(error.getMessage().contains("first"), error.getMessage());
    }

    @Test
    void namedStyleCyclesAreRejectedEvenWhenUnused() {
        var error =
                assertThrows(
                        InvalidLetterException.class,
                        () ->
                                LetterPdfGenerator.builder()
                                        .build()
                                        .generateJson(
                                                """
                    {"schemaVersion":1,"theme":{"namedStyles":{
                      "first":{"styleName":"second"},"second":{"styleName":"first"}}},
                     "sections":[{"name":"Body","blocks":[
                       {"type":"paragraph","runs":[{"text":"Body"}]}]}]}
                    """));
        assertTrue(error.getMessage().contains("cycle"), error.getMessage());
        assertTrue(error.getMessage().contains("first"), error.getMessage());
    }

    @Test
    void tableRolesAndComponentsPreserveRichCellContentSpansAndAppearance() throws Exception {
        byte[] pdf =
                LetterPdfGenerator.builder()
                        .build()
                        .generateJson(
                                """
                {"schemaVersion":1,
                 "theme":{"roles":{"TABLE":{"fontSizePt":22},
                    "TABLE_HEADER":{"fontSizePt":15},"TABLE_CELL":{"fontSizePt":12}}},
                 "components":{"notice":{"blocks":[
                    {"type":"paragraph","runs":[{"text":"Included cell content"}]}]}},
                 "sections":[{"name":"Body","blocks":[{"type":"table","columnWeights":[1,1],
                    "header":[{"runs":[{"text":"Merged header"}],"columnSpan":2}],
                    "rows":[[{"blocks":[{"type":"component","name":"notice"}],"columnSpan":2,
                       "appearance":{"padding":{"topMm":2,"rightMm":2,"bottomMm":2,"leftMm":2}}}]],
                    "options":{"rowKeepTogether":"AUTO"}}]}]}
                """);
        assertTrue(text(pdf).contains("Included cell content"));
        var samples = samples(pdf);
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Merged header")
                                                && sample.size() == 15),
                samples.toString());
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Included cell content")
                                                && sample.size() == 12),
                samples.toString());
    }

    @Test
    void htmlTableRolesOverrideTagDefaultsWhileExplicitCellCssWins() throws Exception {
        var theme =
                LetterTheme.builder()
                        .table(Style.builder().fontSizePt(22).build())
                        .tableHeader(Style.builder().fontSizePt(13).bold(false).build())
                        .tableCell(Style.builder().fontSizePt(12).build())
                        .build();
        var document =
                Letter.fromHtml(
                                """
                <letter><main><table><tr><th>Role header</th><th style="font-weight:bold">Local header</th></tr>
                  <tr><td>Role cell</td><td>Other cell</td></tr></table></main></letter>
                """)
                        .theme(theme)
                        .build();
        var samples = samples(new LetterPdfGenerator().generate(document));
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Role header")
                                                && sample.size() == 13
                                                && !sample.font().contains("Bold")),
                samples.toString());
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Local header")
                                                && sample.size() == 13
                                                && sample.font().contains("Bold")),
                samples.toString());
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Role cell") && sample.size() == 12),
                samples.toString());
    }

    @Test
    void fluentComponentsCanBeReusedAcrossAllLetterRegions() throws Exception {
        var shared = LetterComponent.text("Shared component");
        var model =
                Letter.builder()
                        .theme(
                                LetterTheme.builder()
                                        .body(Style.builder().fontSizePt(10).build())
                                        .build())
                        .component("notice", shared)
                        .header(shared)
                        .footer(shared)
                        .signature(shared)
                        .body(shared)
                        .include("notice")
                        .build();
        String content = text(new LetterPdfGenerator().generate(model));
        assertEquals(5, content.split("Shared component", -1).length - 1);
    }

    @Test
    void fluentTableHeaderDefaultsRemainBelowThemeRoles() throws Exception {
        var theme =
                LetterTheme.builder()
                        .tableHeader(
                                Style.builder().fontSizePt(13).bold(false).color("#123456").build())
                        .build();
        var table = com.nsdev.lettergenerator.Table.builder("Theme header").row("Value").build();
        var model = Letter.builder().theme(theme).table(table).build();
        var samples = samples(new LetterPdfGenerator().generate(model));
        assertTrue(
                samples.stream()
                        .anyMatch(
                                sample ->
                                        sample.text().contains("Theme header")
                                                && sample.size() == 13
                                                && !sample.font().contains("Bold")),
                samples.toString());
    }

    private static List<Block> leaves(List<Block> blocks) {
        return blocks.stream()
                .flatMap(
                        block ->
                                block instanceof GroupBlock group
                                        ? leaves(group.blocks()).stream()
                                        : java.util.stream.Stream.of(block))
                .toList();
    }

    private static List<Sample> samples(byte[] bytes) throws IOException {
        List<Sample> result = new ArrayList<>();
        try (var pdf = Loader.loadPDF(bytes)) {
            new PDFTextStripper() {
                @Override
                protected void writeString(String value, List<TextPosition> positions) {
                    if (!positions.isEmpty()) {
                        var position = positions.get(0);
                        result.add(
                                new Sample(
                                        value,
                                        position.getFontSizeInPt(),
                                        position.getFont().getName()));
                    }
                }
            }.getText(pdf);
        }
        return result;
    }

    private record Sample(String text, float size, String font) {}
}
