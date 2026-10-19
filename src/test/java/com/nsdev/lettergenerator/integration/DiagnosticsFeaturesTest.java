package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.diagnostics.DiagnosticPhase;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.fixtures.LetterFixtures;
import com.nsdev.lettergenerator.internal.template.TemplateInspector;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.render.RenderOptions;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class DiagnosticsFeaturesTest {
    private final LetterPdfGenerator generator = new LetterPdfGenerator();

    @Test
    void inspectionAggregatesEveryMissingOccurrenceAndAssetFailure() {
        var report =
                TemplateInspector.inspect(
                        "<letter><main><p>${missing} ${missing} ${another}</p>"
                                + "<img src='asset:absent' width='20mm'/><img src='asset:bad' width='20mm'/>"
                                + "<img src='https://example.invalid/image.png' width='20mm'/></main></letter>",
                        Map.of(),
                        Map.of("bad", new byte[] {1, 2, 3}),
                        true,
                        generator);
        assertFalse(report.isValid());
        assertEquals(
                3,
                report.diagnostics().stream()
                        .filter(d -> d.code().equals("MISSING_VARIABLE"))
                        .count());
        assertEquals(
                3, report.diagnostics().stream().filter(d -> d.code().contains("ASSET")).count());
        assertTrue(report.diagnostics().stream().allMatch(d -> d.phase() == DiagnosticPhase.INPUT));
        assertThrows(UnsupportedOperationException.class, () -> report.diagnostics().clear());
    }

    @Test
    void inspectionUsesLiteralMapOnlyBindingAndFindsSemanticErrors() {
        assertTrue(
                TemplateInspector.inspect(
                                "<letter><main><p>${value}</p></main></letter>",
                                Map.of("value", "<script>${unexpanded}</script>"),
                                Map.of(),
                                true,
                                generator)
                        .isValid());
        var report =
                TemplateInspector.inspect(
                        "<letter><main><p>${sys:user.home} ${missing:-default}</p></main></letter>",
                        Map.of(),
                        Map.of(),
                        true,
                        generator);
        assertEquals(2, report.diagnostics().size());
        assertFalse(
                TemplateInspector.inspect(
                                "<letter style='font-family:unregistered'><main><p>Text</p></main></letter>",
                                Map.of(),
                                Map.of(),
                                true,
                                generator)
                        .isValid());
    }

    @Test
    void validationReportsInputWithoutRunningLayoutAndGenerateStillThrows() {
        assertFalse(generator.validate(null).isValid());
        assertEquals(DiagnosticPhase.INPUT, generator.validate(null).diagnostics().get(0).phase());
        assertThrows(InvalidLetterException.class, () -> generator.generate(null));
        var valid = document(List.of(paragraph("Hello")));
        assertTrue(generator.validate(valid).isValid());
    }

    @Test
    void rejectedLayoutIncludesStructuredErrors() {
        var model = Letter.builder().headerHeightMm(1).header("Too tall").body("Body").build();
        var error = assertThrows(PdfGenerationException.class, () -> generator.render(model));
        assertFalse(error.diagnostics().isEmpty());
        assertTrue(
                error.diagnostics().stream()
                        .allMatch(item -> item.phase() == DiagnosticPhase.LAYOUT));
    }

    @Test
    void renderOwnsBytesAndReportsActualMultipageCount() throws Exception {
        var result =
                generator.render(
                        document(
                                List.of(
                                        paragraph("First"),
                                        new PageBreakBlock(),
                                        paragraph("Second"))));
        assertEquals(2, result.pageCount());
        assertTrue(result.debugBoxes().isEmpty());
        byte[] changed = result.pdfBytes();
        changed[0] = 0;
        assertEquals("%PDF", new String(result.pdfBytes(), 0, 4, StandardCharsets.US_ASCII));
        try (var pdf = Loader.loadPDF(result.pdfBytes())) {
            assertEquals(result.pageCount(), pdf.getNumberOfPages());
        }
    }

    @Test
    void debugOverlayReportsDeclaredZonesWithoutChangingPageCountOrText() throws Exception {
        var model =
                LetterFixtures.layout(
                        3, com.nsdev.lettergenerator.model.layout.SignaturePlacement.BOTTOM_LAST);
        var normal = generator.render(model);
        var debug = generator.render(model, new RenderOptions(true));
        assertEquals(normal.pageCount(), debug.pageCount());
        assertTrue(normal.debugBoxes().isEmpty());
        assertTrue(
                debug.debugBoxes().stream()
                        .anyMatch(box -> box.kind().equals("DECLARED_PAGE_CONTENT")));
        assertTrue(
                debug.debugBoxes().stream()
                        .anyMatch(box -> box.kind().equals("DECLARED_SIGNATURE")));
        try (var first = Loader.loadPDF(normal.pdfBytes());
                var second = Loader.loadPDF(debug.pdfBytes())) {
            assertEquals(
                    new PDFTextStripper().getText(first), new PDFTextStripper().getText(second));
            assertTrue(second.getPage(0).getContentStreams().hasNext());
        }
    }
}
