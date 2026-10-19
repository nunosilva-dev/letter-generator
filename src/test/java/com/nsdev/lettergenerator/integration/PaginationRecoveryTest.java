package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PaginationRecoveryTest {
    @Test
    void bodyInkOverflowCanMoveToAnOrdinaryPageBeforeTheSignature() throws Exception {
        Style largeText = new Style(null, 45d, null, null, null, null, null, null, null, null);
        Paragraph body = new Paragraph(List.of(new TextRun("I", null, null)), largeText);
        LetterDocument document =
                new LetterDocument(
                        1,
                        new PageSpec(null, null, 0d, 0d, 245d),
                        null,
                        List.of(new Section("body", null, List.of(body))),
                        null,
                        new Signature(
                                SignaturePlacement.BOTTOM_LAST,
                                List.of(paragraph("Signed")),
                                new Bounds(0, 0, 100, 20)),
                        null);
        try (var pdf = Loader.loadPDF(LetterPdfGenerator.builder().build().generate(document))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDFTextStripper text = new PDFTextStripper();
            text.setStartPage(1);
            text.setEndPage(1);
            assertTrue(text.getText(pdf).contains("I"));
            text.setStartPage(2);
            text.setEndPage(2);
            assertTrue(text.getText(pdf).contains("Signed"));
        }
    }
}
