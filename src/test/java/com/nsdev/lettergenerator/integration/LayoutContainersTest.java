package com.nsdev.lettergenerator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import java.util.HashMap;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class LayoutContainersTest {
    @Test
    void columnsLayOutIndependentRichBlocksWithWeightedWidthsAndGap() throws Exception {
        var document =
                Letter.fromHtml(
                                """
<letter><main><columns gap-mm="10">
  <column weight="1"><p>LEFT_COLUMN</p></column>
  <column weight="2"><p>RIGHT_COLUMN</p><p>SECOND_RIGHT</p></column>
</columns><p>AFTER_COLUMNS</p></main></letter>
""")
                        .build();
        var positions = positions(new LetterPdfGenerator().generate(document));
        assertEquals(
                10 / 25.4 * 72 + (170d - 10) / 3 / 25.4 * 72,
                positions.get("RIGHT_COLUMN")[0] - positions.get("LEFT_COLUMN")[0],
                0.2);
        assertEquals(positions.get("RIGHT_COLUMN")[1], positions.get("LEFT_COLUMN")[1], 0.2);
        assertTrue(positions.get("AFTER_COLUMNS")[1] > positions.get("SECOND_RIGHT")[1]);
    }

    @Test
    void canvasConsumesDeclaredHeightAndMaintainsRelativeCoordinates() throws Exception {
        var document =
                Letter.fromHtml(
                                """
<letter><main><canvas width-mm="140" height-mm="70">
  <box id="a" x-mm="0" y-mm="20" width-mm="60" height-mm="20"><p>LOWER_LEFT</p></box>
  <box id="b" x-mm="85" y-mm="0" width-mm="50" height-mm="20"><p>UPPER_RIGHT</p></box>
</canvas><p>AFTER_CANVAS</p></main></letter>
""")
                        .build();
        var positions = positions(new LetterPdfGenerator().generate(document));
        assertEquals(
                85 / 25.4 * 72,
                positions.get("UPPER_RIGHT")[0] - positions.get("LOWER_LEFT")[0],
                0.2);
        assertEquals(
                20 / 25.4 * 72,
                positions.get("LOWER_LEFT")[1] - positions.get("UPPER_RIGHT")[1],
                0.2);
        assertTrue(
                positions.get("AFTER_CANVAS")[1] - positions.get("UPPER_RIGHT")[1]
                        >= 70 / 25.4 * 72 - 1);
    }

    @Test
    void boxPaddingOffsetsNativeContentAndPreservesFlow() throws Exception {
        var document =
                Letter.fromHtml(
                                """
<letter><main><p>BEFORE_BOX</p>
  <box style="padding: 4mm; border-left: 2pt solid #FF0000; background-color: #EEEEEE">
    <p>INSIDE_BOX</p>
  </box><p>AFTER_BOX</p>
</main></letter>
""")
                        .build();
        var positions = positions(new LetterPdfGenerator().generate(document));
        assertEquals(
                4 / 25.4 * 72 + 2,
                positions.get("INSIDE_BOX")[0] - positions.get("BEFORE_BOX")[0],
                0.2);
        assertTrue(positions.get("AFTER_BOX")[1] > positions.get("INSIDE_BOX")[1]);
    }

    @Test
    void malformedContainerGeometryFailsBeforeRendering() {
        for (String markup :
                new String[] {
                    "<columns gap-mm='-1'><column>A</column></columns>",
                    "<columns><column weight='0'>A</column></columns>",
                    "<canvas width-mm='100' height-mm='40'><box id='x' x-mm='-1' y-mm='0'"
                            + " width-mm='20' height-mm='20'>A</box></canvas>",
                    "<canvas width-mm='100' height-mm='40'><box id='x' x-mm='0' y-mm='0'"
                            + " width-mm='20'"
                            + " height-mm='20'><group><page-break/></group></box></canvas>",
                    "<box style='padding: -1mm'>A</box>"
                }) {
            assertThrows(
                    InvalidLetterException.class,
                    () ->
                            new LetterPdfGenerator()
                                    .generate(
                                            Letter.fromHtml(
                                                            "<letter><main>"
                                                                    + markup
                                                                    + "</main></letter>")
                                                    .build()));
        }
    }

    private static Map<String, float[]> positions(byte[] bytes) throws Exception {
        var result = new HashMap<String, float[]>();
        try (var pdf = Loader.loadPDF(bytes)) {
            var stripper =
                    new PDFTextStripper() {
                        @Override
                        protected void writeString(
                                String text, java.util.List<TextPosition> positions) {
                            if (!positions.isEmpty()) {
                                result.put(
                                        text,
                                        new float[] {
                                            positions.get(0).getXDirAdj(),
                                            positions.get(0).getYDirAdj()
                                        });
                            }
                        }
                    };
            stripper.getText(pdf);
        }
        return result;
    }
}
