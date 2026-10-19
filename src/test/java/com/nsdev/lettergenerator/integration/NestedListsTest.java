package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.model.block.ListBlock;
import com.nsdev.lettergenerator.model.text.ListItem;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class NestedListsTest {
    @Test
    void htmlNestedListsPreserveHierarchyInlineStylingAndIndependentNumbering() throws Exception {
        var letter =
                Letter.fromHtml(
                                """
<letter><main><ol>
  <li>PARENT_ONE<ul><li>CHILD_ONE<ol><li>GRANDCHILD_ONE</li></ol></li><li>CHILD_TWO</li></ul></li>
  <li>PARENT_TWO</li>
</ol><p>AFTER_LISTS</p></main></letter>
""")
                        .build();
        var positions = new HashMap<String, float[]>();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(letter))) {
            var stripper =
                    new PDFTextStripper() {
                        @Override
                        protected void writeString(String text, List<TextPosition> glyphs) {
                            for (String token :
                                    List.of(
                                            "PARENT_ONE",
                                            "CHILD_ONE",
                                            "GRANDCHILD_ONE",
                                            "CHILD_TWO",
                                            "PARENT_TWO",
                                            "AFTER_LISTS")) {
                                int offset = text.indexOf(token);
                                if (offset >= 0
                                        && (offset == 0
                                                || !Character.isLetterOrDigit(
                                                        text.charAt(offset - 1)))) {
                                    var glyph = glyphs.get(offset);
                                    positions.put(
                                            token,
                                            new float[] {glyph.getXDirAdj(), glyph.getYDirAdj()});
                                }
                            }
                        }
                    };
            stripper.getText(pdf);
        }
        assertEquals(6, positions.size());
        assertTrue(positions.get("CHILD_ONE")[0] > positions.get("PARENT_ONE")[0] + 10);
        assertTrue(positions.get("GRANDCHILD_ONE")[0] > positions.get("CHILD_ONE")[0] + 10);
        assertEquals(positions.get("PARENT_ONE")[0], positions.get("PARENT_TWO")[0], 0.1);
        assertTrue(positions.get("PARENT_TWO")[1] > positions.get("CHILD_TWO")[1]);
        assertTrue(positions.get("AFTER_LISTS")[1] > positions.get("PARENT_TWO")[1]);
    }

    @Test
    void typedNestedListsOwnItemsAndApplyConfiguredFallbackToChildRuns() throws Exception {
        var children = new ArrayList<ListBlock>();
        children.add(
                new ListBlock(List.of(List.of(new TextRun("CHILD Ω", null, null))), false, null));
        var items = new ArrayList<ListItem>();
        items.add(new ListItem(List.of(new TextRun("PARENT", null, null)), children));
        var list = ListBlock.nested(items, true, null);
        children.clear();
        items.clear();
        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                "Unicode", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .build();
        var model = Letter.builder().fallbackFonts("Unicode").block(list).build();
        var result = generator.render(model);
        assertTrue(text(result.pdfBytes()).contains("CHILD Ω"));
        assertTrue(
                result.diagnostics().stream().anyMatch(d -> d.code().equals("FONT_SUBSTITUTION")));
    }

    @Test
    void jsonHierarchicalListsShareTheSameRenderingAndRejectAmbiguousModes() throws Exception {
        String list =
                """
                {"type":"list","ordered":true,"contentItems":[
                  {"runs":[{"text":"JSON_PARENT"}],"children":[
                    {"type":"list","ordered":false,"items":[[{"text":"JSON_CHILD"}]]}]}]}
                """;
        String json =
                "{\"schemaVersion\":1,\"sections\":[{\"name\":\"body\",\"blocks\":["
                        + list
                        + "]}]}";
        assertTrue(text(new LetterPdfGenerator().generateJson(json)).contains("JSON_CHILD"));
        assertThrows(
                InvalidLetterException.class,
                () ->
                        new LetterPdfGenerator()
                                .generateJson(
                                        json.replace(
                                                "\"contentItems\":",
                                                "\"items\":[[{\"text\":\"ambiguous\"}]],\"contentItems\":")));
    }

    @Test
    void listItemsRejectNonListBlockContent() {
        assertThrows(
                InvalidLetterException.class,
                () ->
                        Letter.fromHtml(
                                        "<letter><main><ul><li>A<p>unsupported"
                                                + " paragraph</p></li></ul></main></letter>")
                                .build());
    }
}
