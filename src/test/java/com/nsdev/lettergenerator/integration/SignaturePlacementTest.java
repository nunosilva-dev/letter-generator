package com.nsdev.lettergenerator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.signature.TextSignature;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class SignaturePlacementTest {
    private static final double SIGNATURE_HEIGHT_MM = 25;
    private static final double POINTS_PER_MM = 72.0 / 25.4;
    private static final double SIGNATURE_TOP_MM = 232;
    private static final double SIGNATURE_BOTTOM_MM = 257;

    @Test
    void fluentAndHtmlSignaturesAppearOnlyAtTheBottomOfTheLastPage() throws Exception {
        for (boolean multiplePages : List.of(false, true)) {
            var fluent =
                    Letter.builder()
                            .signaturePlacement(SignaturePlacement.BOTTOM_LAST)
                            .signatureHeightMm(SIGNATURE_HEIGHT_MM)
                            .body("FIRST BODY")
                            .footer("REPEATED FOOTER")
                            .signature(TextSignature.of("LAST SIGNATURE"));
            if (multiplePages) {
                fluent.pageBreak().body("FINAL BODY");
            }
            String main =
                    "<p>FIRST BODY</p>"
                            + (multiplePages ? "<page-break></page-break><p>FINAL BODY</p>" : "");
            var html =
                    Letter.fromHtml(
                                    "<letter><main>"
                                            + main
                                            + "</main>"
                                            + "<signature placement='bottom-last' height-mm='25'><p>LAST SIGNATURE</p></signature>"
                                            + "<footer><p>REPEATED FOOTER</p></footer></letter>")
                            .build();
            for (var document : List.of(fluent.build(), html)) {
                assertBottomLast(document, multiplePages ? 2 : 1);
            }
        }
    }

    @Test
    void flowRemainsTheDefaultAndPlacementCanBeChangedAfterContent() {
        var builder = Letter.builder().body("Body").signature(TextSignature.of("Signed"));
        var flow = builder.build();
        assertEquals(SignaturePlacement.FLOW, flow.signature().placement());
        assertEquals(0, flow.page().signatureZoneMm());
        assertNull(flow.signature().boundsMm());
        var bottom = builder.signaturePlacement(SignaturePlacement.BOTTOM_LAST).build();
        assertTrue(bottom.page().signatureZoneMm() > 0);
        builder.signaturePlacement(SignaturePlacement.FLOW);
        assertEquals(flow, builder.build());
    }

    @Test
    void selectingPlacementWithoutSignatureDoesNotReserveBlankSpace() {
        var letter =
                Letter.builder()
                        .body("Body")
                        .signaturePlacement(SignaturePlacement.BOTTOM_LAST)
                        .build();
        assertNull(letter.signature());
        assertEquals(0, letter.page().signatureZoneMm());
    }

    @Test
    void fluentOptionsRejectNullPlacementAndInvalidHeight() {
        assertThrows(
                IllegalArgumentException.class, () -> Letter.builder().signaturePlacement(null));
        for (double height : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Letter.builder().signatureHeightMm(height));
        }
    }

    @Test
    void htmlRejectsUnsupportedPlacementAndInvalidHeight() {
        for (String attributes :
                List.of(
                        "placement='middle'",
                        "height-mm='0'",
                        "height-mm='-1'",
                        "height-mm='NaN'")) {
            assertThrows(
                    InvalidLetterException.class,
                    () ->
                            Letter.fromHtml(
                                            "<letter><main><p>Body</p></main><signature "
                                                    + attributes
                                                    + "><p>Signed</p></signature></letter>")
                                    .build());
        }
    }

    private static void assertBottomLast(LetterDocument document, int expectedPages)
            throws Exception {
        byte[] bytes = new LetterPdfGenerator().generate(document);
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(expectedPages, pdf.getNumberOfPages());
            for (int page = 1; page <= expectedPages; page++) {
                var stripper =
                        new PDFTextStripper() {
                            @Override
                            protected void writeString(String value, List<TextPosition> positions)
                                    throws IOException {
                                if (value.contains("LAST SIGNATURE")) {
                                    for (var position : positions) {
                                        assertTrue(
                                                position.getYDirAdj()
                                                                >= SIGNATURE_TOP_MM * POINTS_PER_MM
                                                        && position.getYDirAdj()
                                                                < SIGNATURE_BOTTOM_MM
                                                                        * POINTS_PER_MM,
                                                "Signature baseline "
                                                        + position.getYDirAdj()
                                                        + " for text "
                                                        + value);
                                    }
                                }
                                super.writeString(value, positions);
                            }
                        };
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(pdf);
                assertEquals(page == expectedPages, text.contains("LAST SIGNATURE"));
                assertEquals(page == 1, text.contains("FIRST BODY"));
                assertTrue(text.contains("REPEATED FOOTER"));
                if (expectedPages > 1) {
                    assertEquals(page == expectedPages, text.contains("FINAL BODY"));
                }
            }
        }
    }
}
