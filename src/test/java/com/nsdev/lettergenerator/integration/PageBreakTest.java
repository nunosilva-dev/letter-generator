package com.nsdev.lettergenerator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PageBreakTest {
    @Test
    void explicitPageBreakMovesFollowingContentToTheNextPage() throws Exception {
        String json =
                """
                {"schemaVersion":1,"sections":[{"name":"body","blocks":[
                  {"type":"paragraph","runs":[{"text":"FIRST PAGE"}]},
                  {"type":"pageBreak"},
                  {"type":"paragraph","runs":[{"text":"SECOND PAGE"}]}]}]}
                """;
        byte[] bytes = LetterPdfGenerator.builder().build().generateJson(json);
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(2, pdf.getNumberOfPages());
            var stripper = new PDFTextStripper();
            stripper.setEndPage(1);
            assertTrue(stripper.getText(pdf).contains("FIRST PAGE"));
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertTrue(stripper.getText(pdf).contains("SECOND PAGE"));
        }
    }

    @Test
    void pageBreakCannotBePlacedInsideAHeaderOrSignature() {
        String prefix =
                """
                {"schemaVersion":1,"sections":[{"name":"body","blocks":[
                  {"type":"paragraph","runs":[{"text":"Body"}]}]}],
                """;
        String header =
                prefix
                        + """
                "page":{"topZoneMm":20},"fixedElements":[{"id":"header","zone":"TOP",
                "pages":"ALL","boundsMm":{"x":0,"y":0,"width":100,"height":20},
                "blocks":[{"type":"pageBreak"}]}]}
                """;
        String signature =
                prefix
                        + """
                "signature":{"placement":"FLOW","blocks":[{"type":"pageBreak"}]}}
                """;
        var generator = LetterPdfGenerator.builder().build();
        assertEquals(
                "fixedElements[0].blocks[0]",
                assertThrows(InvalidLetterException.class, () -> generator.generateJson(header))
                        .fieldPath());
        assertEquals(
                "signature.blocks[0]",
                assertThrows(InvalidLetterException.class, () -> generator.generateJson(signature))
                        .fieldPath());
    }
}
