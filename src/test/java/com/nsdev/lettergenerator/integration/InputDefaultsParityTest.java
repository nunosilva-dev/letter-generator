package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.png;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.signature.TextSignature;
import org.junit.jupiter.api.Test;

class InputDefaultsParityTest {
    @Test
    void javaAndHtmlShareDefaultLetterGeometryIncludingTheLastPageSignature() throws Exception {
        byte[] image = png();
        var javaLetter =
                Letter.builder()
                        .body("Body")
                        .logo(image)
                        .header("Header")
                        .footer("Footer")
                        .signature(TextSignature.of("Signed"))
                        .signaturePlacement(SignaturePlacement.BOTTOM_LAST)
                        .build();
        var htmlLetter =
                Letter.fromHtml(
                                """
                                <letter>
                                  <logo><img src="asset:logo"></logo>
                                  <header><p>Header</p></header>
                                  <main><p>Body</p></main>
                                  <footer><p>Footer</p></footer>
                                  <signature placement="bottom-last"><p>Signed</p></signature>
                                </letter>
                                """)
                        .asset("logo", image)
                        .build();

        assertEquals(new PageSpec(null, null, 30d, 20d, 20d), javaLetter.page());
        assertEquals(javaLetter.page(), htmlLetter.page());
        assertEquals(
                javaLetter.fixedElements().stream().map(FixedElement::boundsMm).toList(),
                htmlLetter.fixedElements().stream().map(FixedElement::boundsMm).toList());
        assertEquals(javaLetter.signature().boundsMm(), htmlLetter.signature().boundsMm());
        assertEquals(javaLetter.signature().placement(), htmlLetter.signature().placement());
    }

    @Test
    void cssPointPixelAndMillimeterDimensionsProduceEquivalentModelSizes() throws Exception {
        byte[] image = png();
        String[][] sizes = {{"12pt", "72pt"}, {"16px", "96px"}, {"4.233333333333333mm", "25.4mm"}};
        for (String[] size : sizes) {
            var document =
                    Letter.fromHtml(
                                    """
                                    <letter><main>
                                      <p style="font-size: %s">Text</p>
                                      <img src="asset:photo" width="%s">
                                    </main></letter>
                                    """
                                            .formatted(size[0], size[1]))
                            .asset("photo", image)
                            .build();
            var blocks = document.sections().get(0).blocks();
            assertEquals(12, ((Paragraph) blocks.get(0)).style().fontSizePt(), 0.000000001);
            assertEquals(25.4, ((ImageBlock) blocks.get(1)).widthMm(), 0.000000001);
            assertEquals(12.7, ((ImageBlock) blocks.get(1)).heightMm(), 0.000000001);
        }
    }
}
