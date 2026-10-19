package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.png;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.internal.composition.DocumentPreparation;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.BoxBlock;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ColumnsBlock;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.Column;
import com.nsdev.lettergenerator.model.layout.PositionedContent;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.Border;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.Padding;
import com.nsdev.lettergenerator.model.text.TextRun;
import com.nsdev.lettergenerator.theme.LetterTheme;
import com.nsdev.lettergenerator.theme.Stationery;
import com.nsdev.lettergenerator.theme.TableAppearance;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

/** Regressions confirmed by the independent integration review. */
class V1ReviewRegressionTest {
    @Test
    void explicitSubjectTypographyHasTheSamePriorityWithAndWithoutAnEmptyTheme() throws Exception {
        Style explicit = Style.builder().fontSizePt(10).bold(false).build();
        var plain = Letter.builder().style(explicit).subject("SUBJECT").body("Body").build();
        var themed =
                Letter.builder()
                        .style(explicit)
                        .theme(LetterTheme.builder().build())
                        .subject("SUBJECT")
                        .body("Body")
                        .build();
        for (LetterDocument document : List.of(plain, themed)) {
            var positions = positions(document, "SUBJECT");
            assertEquals(10, positions.get(0).getFontSizeInPt(), 0.01);
            assertEquals("Helvetica", positions.get(0).getFont().getName());
        }
    }

    @Test
    void stationeryFooterDefaultsStayBelowTheThemeRoleJustLikeLocalFooterDefaults()
            throws Exception {
        var theme =
                LetterTheme.builder()
                        .footer(Style.builder().fontSizePt(16).build())
                        .stationery(Stationery.builder().footer("FOOTER").build())
                        .build();
        var inherited = Letter.builder().theme(theme).body("Body").build();
        var local = Letter.builder().theme(theme).body("Body").footer("FOOTER").build();
        for (LetterDocument document : List.of(inherited, local)) {
            assertEquals(16, positions(document, "FOOTER").get(0).getFontSizeInPt(), 0.01);
        }
    }

    @Test
    void themeCellPaddingParticipatesInRichImageFittingForTemplateAndDeferredFragments()
            throws Exception {
        var theme =
                LetterTheme.builder()
                        .tableAppearance(
                                new TableAppearance(
                                        new CellAppearance(null, Padding.uniform(10), null, null),
                                        null,
                                        null))
                        .build();
        String table =
                "<table><tr><td><img src='asset:logo' width='100%' alt='Logo'></td></tr></table>";
        var html =
                Letter.fromHtml("<letter><main>" + table + "</main></letter>")
                        .asset("logo", png())
                        .theme(theme)
                        .build();
        var template =
                Letter.template("<letter><main>" + table + "</main></letter>")
                        .asset("logo", png())
                        .theme(theme)
                        .build();
        var fluent = Letter.builder().asset("logo", png()).bodyHtml(table).theme(theme).build();
        var generator = new LetterPdfGenerator();
        for (LetterDocument document : List.of(html, template, fluent)) {
            var report = generator.validate(document);
            assertTrue(report.isValid(), report.diagnostics().toString());
            assertTrue(generator.generate(document).length > 0);
        }
    }

    @Test
    void themedRichImagesFitDeferredHeaderFooterAndSignatureRegions() throws Exception {
        var theme =
                LetterTheme.builder()
                        .tableAppearance(
                                new TableAppearance(
                                        new CellAppearance(
                                                Border.NONE, new Padding(0, 10, 0, 10), null, null),
                                        null,
                                        null))
                        .build();
        String table =
                "<table><tr><td><img src='asset:logo' width='100%' alt='Logo'></td></tr></table>";
        var header =
                Letter.builder()
                        .asset("logo", png())
                        .theme(theme)
                        .headerHeightMm(100)
                        .headerHtml(table)
                        .body("Body")
                        .build();
        var footer =
                Letter.builder()
                        .asset("logo", png())
                        .theme(theme)
                        .footerHeightMm(100)
                        .footerHtml(table)
                        .body("Body")
                        .build();
        var signature =
                Letter.builder()
                        .asset("logo", png())
                        .theme(theme)
                        .signatureHeightMm(100)
                        .signatureHtml(table)
                        .body("Body")
                        .build();
        var generator = new LetterPdfGenerator();
        for (LetterDocument document : List.of(header, footer, signature)) {
            assertTrue(generator.validate(document).isValid());
            assertTrue(generator.generate(document).length > 0);
        }
    }

    @Test
    void explicitCellPaddingOverridesThemeBeforePercentageImageFitting() throws Exception {
        var theme =
                LetterTheme.builder()
                        .tableAppearance(
                                new TableAppearance(
                                        new CellAppearance(
                                                Border.NONE, new Padding(0, 10, 0, 10), null, null),
                                        null,
                                        null))
                        .build();
        String table =
                "<table><tr><td style='padding:0mm 2mm'><img src='asset:logo' width='100%' alt='Logo'></td></tr></table>";
        var document =
                Letter.fromHtml("<letter><main>" + table + "</main></letter>")
                        .asset("logo", png())
                        .theme(theme)
                        .build();
        var prepared = DocumentPreparation.prepare(document);
        ImageBlock image = firstImage(prepared.sections().get(0).blocks());
        assertEquals(166, image.widthMm(), 0.000001);
        assertEquals("Logo", image.altText());
        assertTrue(new LetterPdfGenerator().generate(document).length > 0);
    }

    @Test
    void canvasInsideBorderedBoxMustFitTheRemainingParentHeightBeforeRendering() {
        var document =
                Letter.fromHtml(
                                "<letter><main><canvas width-mm='100' height-mm='20'><box id='outer' width-mm='100' height-mm='20'><box style='border-top:5pt solid #000000;border-bottom:5pt solid #000000'><canvas width-mm='50' height-mm='20'><box id='inner' width-mm='50' height-mm='20'><p>Inside</p></box></canvas></box></box></canvas></main></letter>")
                        .build();
        var report = new LetterPdfGenerator().validate(document);
        assertFalse(
                report.isValid(),
                "Twenty millimeters of content plus two borders cannot fit a twenty-millimeter box");
        assertTrue(
                report.diagnostics().stream()
                        .anyMatch(diagnostic -> diagnostic.path().endsWith("heightMm")),
                report.diagnostics().toString());
    }

    @Test
    void headingsInsideEveryContainerRequireTheirActualBoldFaceDuringValidation() {
        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                "RegularOnly", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .build();
        var heading = new Heading(List.of(new TextRun("Heading", null, null)), null);
        var canvas =
                new CanvasBlock(
                        100,
                        30,
                        List.of(
                                new PositionedContent(
                                        "heading",
                                        new Bounds(0, 0, 100, 30),
                                        List.of(heading),
                                        null)),
                        null);
        var columns = new ColumnsBlock(List.of(new Column(1, List.of(heading), null)), 0, null);
        var box = new BoxBlock(List.of(heading), null, null);
        for (Block block : List.of(heading, canvas, columns, box)) {
            var document =
                    Letter.builder()
                            .style(Style.builder().fontFamily("RegularOnly").build())
                            .block(block)
                            .build();
            var report = generator.validate(document);
            assertFalse(report.isValid(), block.getClass().getSimpleName());
            assertTrue(
                    report.diagnostics().stream()
                            .anyMatch(diagnostic -> diagnostic.message().contains("BOLD")),
                    report.diagnostics().toString());
        }
    }

    /** Finds a rendered rich-table image without deriving its expected physical width. */
    private static ImageBlock firstImage(List<Block> blocks) {
        for (Block block : blocks) {
            if (block instanceof ImageBlock image) {
                return image;
            }
            if (block instanceof GroupBlock group) {
                return firstImage(group.blocks());
            }
            if (block instanceof TableBlock table) {
                return firstImage(table.rows().get(0).get(0).blocks());
            }
        }
        throw new AssertionError("Expected image in fixture");
    }

    /** Collects actual PDF glyph positions matching an independently supplied phrase. */
    private static List<TextPosition> positions(LetterDocument document, String phrase)
            throws Exception {
        var result = new ArrayList<TextPosition>();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(document))) {
            var stripper =
                    new PDFTextStripper() {
                        @Override
                        protected void writeString(String value, List<TextPosition> positions) {
                            if (value.contains(phrase)) {
                                result.addAll(positions);
                            }
                        }
                    };
            stripper.getText(pdf);
        }
        assertTrue(!result.isEmpty(), phrase);
        return result;
    }
}
