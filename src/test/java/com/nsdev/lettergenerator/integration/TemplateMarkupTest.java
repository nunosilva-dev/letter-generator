package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.png;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.renderFirst;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.style.TextAlign;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class TemplateMarkupTest {
    @Test
    void tableStructureCannotSilentlyDiscardText() {
        for (String table :
                new String[] {
                    "<table>LOST TEXT<tr><td>CELL</td></tr></table>",
                    "<table><tbody>LOST TEXT<tr><td>CELL</td></tr></tbody></table>",
                    "<table><thead>LOST TEXT<tr><th>HEADER</th></tr></thead><tr><td>CELL</td></tr></table>",
                    "<table><tr>LOST TEXT<td>CELL</td></tr></table>"
                }) {
            var error =
                    assertThrows(
                            InvalidLetterException.class,
                            () ->
                                    Letter.fromHtml("<letter><main>" + table + "</main></letter>")
                                            .build(),
                            table);
            assertTrue(error.fieldPath().startsWith("template"));
        }
    }

    @Test
    void parserCannotSilentlyRemoveUnsupportedElementsOrAttributes() {
        for (String markup :
                new String[] {
                    "<td colspan='3'>LOST CELL</td>",
                    "<body style='color:red'><p>BODY</p></body>",
                    "<html style='color:red'><p>BODY</p></html>"
                }) {
            assertThrows(
                    InvalidLetterException.class,
                    () -> Letter.fromHtml("<letter><main>" + markup + "</main></letter>").build(),
                    markup);
            assertThrows(
                    InvalidLetterException.class,
                    () -> Letter.builder().bodyHtml(markup).build(),
                    markup);
        }
    }

    @Test
    void inlineElementsRejectBlockMarginsInsteadOfDroppingThem() {
        for (String tag :
                new String[] {"span", "strong", "em", "a href='https://example.com'", "br"}) {
            String name = tag.split(" ")[0];
            String markup =
                    "<"
                            + tag
                            + " style='margin-top:100mm'>"
                            + (name.equals("br") ? "" : "BODY</" + name + ">");
            var error =
                    assertThrows(
                            InvalidLetterException.class,
                            () -> Letter.builder().bodyHtml(markup).build(),
                            markup);
            assertTrue(error.fieldPath().startsWith("template"));
        }
    }

    @Test
    void explicitTopMarginIsRetainedOnTheFirstHeaderBlock() throws Exception {
        String template =
                "<letter header-height-mm='40'><header><p style='margin-top:%smm'>HEADER</p></header><main><p>Body</p></main></letter>";
        float[] baseline = new float[2];
        for (int index = 0; index < baseline.length; index++) {
            var letter = Letter.fromHtml(template.formatted(index * 20)).build();
            try (var pdf = Loader.loadPDF(LetterPdfGenerator.builder().build().generate(letter))) {
                final int position = index;
                var stripper =
                        new PDFTextStripper() {
                            @Override
                            protected void writeString(
                                    String value,
                                    java.util.List<org.apache.pdfbox.text.TextPosition> positions) {
                                if (value.contains("HEADER")) {
                                    baseline[position] = positions.get(0).getYDirAdj();
                                }
                            }
                        };
                stripper.getText(pdf);
            }
        }
        assertEquals(20 * 72 / 25.4, baseline[1] - baseline[0], 0.1);
    }

    @Test
    void resolvedHtmlCannotSilentlyIgnoreASuppliedValueMap() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Letter.fromHtml("<letter><main><p>${name}</p></main></letter>")
                                .values(Map.of("name", "Example")));
    }

    @Test
    void imageFooterFitsTheFinalMarginsRegardlessOfFluentCallOrder() throws Exception {
        var banner =
                new java.awt.image.BufferedImage(
                        200, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var data = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(banner, "png", data);
        var letter = Letter.builder().body("Body").footer(data.toByteArray()).marginsMm(30).build();
        var footer =
                letter.fixedElements().stream()
                        .filter(element -> element.id().equals("letter.footer"))
                        .findFirst()
                        .orElseThrow();
        assertEquals(150, ((ImageBlock) footer.blocks().get(0)).widthMm());
        assertTrue(text(LetterPdfGenerator.builder().build().generate(letter)).contains("Body"));
    }

    @Test
    void substitutionsAreMapOnlyAndAttributesRemainTyped() {
        assertThrows(
                InvalidLetterException.class,
                () ->
                        Letter.template(
                                        "<letter><main><p style='font-size:${size}'>Text</p></main></letter>")
                                .values(Map.of("size", "12pt;font-weight:bold"))
                                .build());
        assertThrows(
                InvalidLetterException.class,
                () ->
                        Letter.template("<letter><main><p>${sys:user.home}</p></main></letter>")
                                .build());
        assertThrows(
                InvalidLetterException.class,
                () ->
                        Letter.template("<letter><main><p>${missing:-fallback}</p></main></letter>")
                                .build());
        assertThrows(
                InvalidLetterException.class,
                () ->
                        Letter.template(
                                        "<letter><main><p style='font-size:${size}'>Text</p></main></letter>")
                                .values(Map.of("size", "12pt;position:absolute"))
                                .build());
    }

    @Test
    void mapValuesRemainLiteralTextAndAreNotRecursivelySubstituted() throws Exception {
        var letter =
                Letter.template(
                                "<letter><main><p>Hello <strong>${customer}</strong>: ${price}</p></main></letter>")
                        .values(
                                Map.of(
                                        "customer",
                                        "Ana <strong> & ${untouched}",
                                        "price",
                                        "0,1632"))
                        .build();
        String content = text(LetterPdfGenerator.builder().build().generate(letter));
        assertTrue(content.contains("Ana <strong> & ${untouched}"), content);
        assertTrue(content.contains("0,1632"));
        assertEquals(1, letter.sections().get(0).blocks().size());
        Paragraph paragraph = (Paragraph) letter.sections().get(0).blocks().get(0);
        assertTrue(
                paragraph.runs().stream()
                        .anyMatch(
                                run ->
                                        run.text().contains("Ana")
                                                && Boolean.TRUE.equals(run.style().bold())));
    }

    @Test
    void styledFooterImageAndHeaderRepeatAroundOrderedTablesAndPageBreaks() throws Exception {
        String markup =
                """
                <letter page="LETTER" style="font-size:10pt" footer-height-mm="18">
                  <header style="color:#123456"><p>HEADER ${name}</p></header>
                  <main>
                    <p>BEFORE <strong>TABLE</strong></p>
                    <table column-weights="2,1">
                      <thead style="color:white;background-color:#10384A"><tr><th>Item</th><th>Price</th></tr></thead>
                      <tbody><tr><td>Energy</td><td>${price}</td></tr></tbody>
                    </table>
                    <page-break></page-break><p>SECOND PAGE</p>
                  </main>
                  <signature><p style="font-style:italic">Jane Smith</p><img src="asset:signature" height="5mm"></signature>
                  <footer style="font-size:7pt;color:#556677;text-align:center">
                    <img src="asset:banner" width="120mm" height="6mm">
                    <p style="margin-top:1mm;margin-bottom:0">STYLED FOOTER</p>
                  </footer>
                </letter>
                """;
        var letter =
                Letter.template(markup)
                        .values(Map.of("name", "Example", "price", "0,1632"))
                        .asset("banner", png())
                        .asset("signature", png())
                        .build();
        assertEquals(PageSize.LETTER, letter.page().size());
        var footer =
                letter.fixedElements().stream()
                        .filter(element -> element.id().equals("letter.footer"))
                        .findFirst()
                        .orElseThrow();
        assertTrue(footer.blocks().get(0) instanceof ImageBlock);
        assertEquals(6, ((ImageBlock) footer.blocks().get(0)).heightMm());
        assertEquals(TextAlign.CENTER, footer.blocks().get(1).style().textAlign());
        byte[] bytes = LetterPdfGenerator.builder().build().generate(letter);
        Files.createDirectories(Path.of("target/examples"));
        Files.write(Path.of("target/examples/template-features.pdf"), bytes);
        renderFirst(bytes, "template-features");
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(2, pdf.getNumberOfPages());
            var stripper = new PDFTextStripper();
            for (int page = 1; page <= 2; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String content = stripper.getText(pdf);
                assertTrue(content.contains("HEADER Example"));
                assertTrue(content.contains("STYLED FOOTER"));
                assertTrue(
                        pdf.getPage(page - 1)
                                .getResources()
                                .getXObjectNames()
                                .iterator()
                                .hasNext());
            }
        }
        String content = text(bytes);
        assertTrue(content.indexOf("BEFORE") < content.indexOf("0,1632"));
        assertTrue(content.indexOf("0,1632") < content.indexOf("SECOND PAGE"));
    }

    @Test
    void nestedInlineStylesAndExplicitNormalOverridesSurviveMapping() {
        var letter =
                Letter.fromHtml(
                                """
                <letter><main><p style="color:#224466;text-align:right;font-size:12pt">
                regular <strong>bold <em>both <span style="font-weight:normal">italic only</span></em></strong>
                <u>underlined</u><br>next line</p></main></letter>
                """)
                        .build();
        Paragraph paragraph = (Paragraph) letter.sections().get(0).blocks().get(0);
        assertEquals(TextAlign.RIGHT, paragraph.style().textAlign());
        assertEquals(12, paragraph.style().fontSizePt());
        assertTrue(
                paragraph.runs().stream()
                        .anyMatch(
                                run ->
                                        run.text().contains("both")
                                                && Boolean.TRUE.equals(run.style().bold())
                                                && Boolean.TRUE.equals(run.style().italic())));
        assertTrue(
                paragraph.runs().stream()
                        .anyMatch(
                                run ->
                                        run.text().contains("italic only")
                                                && Boolean.FALSE.equals(run.style().bold())
                                                && Boolean.TRUE.equals(run.style().italic())));
        assertTrue(
                paragraph.runs().stream()
                        .anyMatch(
                                run ->
                                        run.text().contains("underlined")
                                                && Boolean.TRUE.equals(run.style().underline())));
        assertTrue(paragraph.runs().stream().anyMatch(run -> run.text().contains("\n")));
    }

    @Test
    void mixedFluentFragmentsResolveLateAssetsAndPreserveInsertionOrder() throws Exception {
        var letter =
                Letter.builder()
                        .body("BEFORE")
                        .bodyHtml("Freeform <strong>bold</strong><br>second line")
                        .body("AFTER")
                        .headerHtml("<p style='color:#123456'>Header</p>")
                        .footerHtml(
                                "<img src='asset:footer' height='6mm'><p style='font-size:7pt'>Footer</p>")
                        .signatureHtml("<p><em>Jane Smith</em></p>")
                        .asset("footer", png())
                        .build();
        String content = text(LetterPdfGenerator.builder().build().generate(letter));
        assertTrue(content.indexOf("BEFORE") < content.indexOf("Freeform"));
        assertTrue(content.indexOf("Freeform") < content.indexOf("AFTER"));
        assertTrue(content.contains("Jane Smith"));
    }

    @Test
    void valuesAndAssetsAreSnapshotsAndAlreadyResolvedHtmlIsNotSubstitutedAgain() throws Exception {
        var values = new HashMap<String, String>();
        values.put("name", "ORIGINAL");
        byte[] image = png();
        var builder =
                Letter.template(
                                "<letter><main><p>${name}</p></main><footer><img src='asset:banner' height='5mm'></footer></letter>")
                        .values(values)
                        .asset("banner", image);
        values.put("name", "MUTATED");
        Arrays.fill(image, (byte) 0);
        var original = builder.build();
        builder.values(Map.of("name", "SECOND"));
        var generator = LetterPdfGenerator.builder().build();
        assertTrue(text(generator.generate(original)).contains("ORIGINAL"));
        assertTrue(text(generator.generate(builder.build())).contains("SECOND"));
        var resolved = Letter.fromHtml("<letter><main><p>${literal}</p></main></letter>").build();
        assertTrue(text(generator.generate(resolved)).contains("${literal}"));
    }

    @Test
    void missingVariablesUnsupportedMarkupAndExternalImagesFailWithLocations() {
        var missing =
                assertThrows(
                        InvalidLetterException.class,
                        () ->
                                Letter.template("<letter><main><p>${missing}</p></main></letter>")
                                        .build());
        assertTrue(missing.getMessage().contains("missing"));
        for (String content :
                new String[] {
                    "<script>alert(1)</script>", "<p style='position:absolute'>Text</p>",
                    "<img src='file:///etc/passwd'>", "<p onclick='bad()'>Text</p>",
                    "<table><tr><td colspan='2'>No spans</td></tr></table>"
                }) {
            var error =
                    assertThrows(
                            InvalidLetterException.class,
                            () ->
                                    Letter.fromHtml("<letter><main>" + content + "</main></letter>")
                                            .build(),
                            content);
            assertTrue(error.fieldPath().startsWith("template"), error.getMessage());
        }
        assertThrows(
                InvalidLetterException.class,
                () ->
                        Letter.fromHtml(
                                        "<letter><main><p>Body</p></main><footer><page-break></page-break></footer></letter>")
                                .build());
    }

    @Test
    void plainFluentFooterCanAlsoBeAnImage() throws Exception {
        var letter = Letter.builder().body("Body").footer(png()).build();
        var footer =
                letter.fixedElements().stream()
                        .filter(element -> element.id().equals("letter.footer"))
                        .findFirst()
                        .orElseThrow();
        assertTrue(footer.blocks().get(0) instanceof ImageBlock);
        assertFalse(text(LetterPdfGenerator.builder().build().generate(letter)).isBlank());
    }
}
