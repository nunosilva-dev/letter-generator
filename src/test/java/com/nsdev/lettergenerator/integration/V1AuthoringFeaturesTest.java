package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;

/** Real-PDF acceptance for semantic letters and freely positioned content. */
class V1AuthoringFeaturesTest {
    @Test
    void semanticLetterTagsProduceConventionalLetterParts() throws Exception {
        var model =
                Letter.fromHtml(
                                """
                <letter>
                  <sender><p>Example Company<br>100 Example Avenue</p></sender>
                  <recipient><p>Alex Example<br>200 Sample Street</p></recipient>
                  <subject>Account confirmation</subject>
                  <reference label="Account">EX-1001</reference>
                  <salutation>Dear Alex,</salutation>
                  <main><p>Your request has been accepted.</p></main>
                  <closing>Kind regards,</closing>
                </letter>
                """)
                        .build();
        String content = text(new LetterPdfGenerator().generate(model));
        for (String expected :
                new String[] {
                    "Example Company",
                    "Alex Example",
                    "Account confirmation",
                    "Account: EX-1001",
                    "Dear Alex,",
                    "Your request",
                    "Kind regards,"
                }) {
            assertTrue(content.contains(expected), content);
        }
        assertTrue(content.indexOf("Dear Alex,") < content.indexOf("Your request"));
        assertTrue(content.indexOf("Kind regards,") > content.indexOf("Your request"));
    }

    @Test
    void canvasMarkupRendersArbitraryContentPositionsOnCustomPaper() throws Exception {
        var model =
                Letter.fromHtml(
                                """
                <letter page-width-mm="180" page-height-mm="240">
                  <main><canvas width-mm="140" height-mm="70">
                    <box id="recipient" x-mm="0" y-mm="20" width-mm="70" height-mm="25"><p>LEFT ADDRESS</p></box>
                    <box id="reference" x-mm="85" y-mm="0" width-mm="50" height-mm="20"><p>RIGHT REFERENCE</p></box>
                  </canvas><p>AFTER CANVAS</p></main>
                </letter>
                """)
                        .build();
        try (var pdf = Loader.loadPDF(new LetterPdfGenerator().generate(model))) {
            assertEquals(180 / 25.4 * 72, pdf.getPage(0).getMediaBox().getWidth(), 0.01);
            assertEquals(240 / 25.4 * 72, pdf.getPage(0).getMediaBox().getHeight(), 0.01);
            String content = new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
            assertTrue(content.contains("LEFT ADDRESS"));
            assertTrue(content.contains("RIGHT REFERENCE"));
            assertTrue(content.contains("AFTER CANVAS"));
        }
    }

    @Test
    void themeTableAppearanceAppliesToTypedJsonWithoutLocalConfiguration() throws Exception {
        byte[] pdf =
                new LetterPdfGenerator()
                        .generateJson(
                                """
                {"schemaVersion":1,"theme":{"tableAppearance":{
                  "cells":{"padding":{"topMm":2,"rightMm":2,"bottomMm":2,"leftMm":2},"backgroundColor":"#EEEEEE"},
                  "header":{"backgroundColor":"#10384A"},"rowKeepTogether":"PREFER"}},
                 "sections":[{"name":"Body","blocks":[{"type":"table","columnWeights":[1],
                   "header":[{"runs":[{"text":"Header"}]}],"rows":[[{"runs":[{"text":"Value"}]}]]}]}]}
                """);
        assertTrue(text(pdf).contains("Value"));
    }

    @Test
    void canvasRejectsDuplicateElementIdsAndOutOfBoundsBoxes() {
        for (String boxes :
                new String[] {
                    "<box id='same' x-mm='0' y-mm='0' width-mm='20' height-mm='10'>A</box><box id='same' x-mm='30' y-mm='0' width-mm='20' height-mm='10'>B</box>",
                    "<box id='outside' x-mm='95' y-mm='0' width-mm='20' height-mm='10'>A</box>"
                }) {
            assertThrows(
                    InvalidLetterException.class,
                    () ->
                            new LetterPdfGenerator()
                                    .generate(
                                            Letter.fromHtml(
                                                            "<letter><main><canvas width-mm='100' height-mm='40'>"
                                                                    + boxes
                                                                    + "</canvas></main></letter>")
                                                    .build()));
        }
    }
}
