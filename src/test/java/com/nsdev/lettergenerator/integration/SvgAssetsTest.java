package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.content.ImageBlocks;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;
import com.nsdev.lettergenerator.signature.ImageSignature;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

class SvgAssetsTest {
    private static final String SVG =
            "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 100'>"
                    + "<defs><linearGradient id='paint'><stop stop-color='red'/>"
                    + "<stop offset='1' stop-color='blue'/></linearGradient></defs>"
                    + "<rect width='200' height='100' fill='url(#paint)'/></svg>";

    @Test
    void svgFitsFromViewBoxAndOwnsItsBytes() {
        byte[] bytes = bytes(SVG);
        var image = ImageBlocks.fit(bytes, 30, 30);
        bytes[0] = 0;
        assertEquals("image/svg+xml", image.mimeType());
        assertEquals(30, image.widthMm());
        assertEquals(15, image.heightMm());
        byte[] returned = image.data();
        returned[0] = 0;
        assertEquals('<', image.data()[0]);
    }

    @Test
    void typedJsonAndHtmlImagesRenderAsVectorContent() throws Exception {
        var generator = LetterPdfGenerator.builder().build();
        byte[] svg = bytes(SVG);
        var typed = document(List.of(new ImageBlock(svg, "image/svg+xml", 40d, 20d, null)));
        var html =
                Letter.fromHtml(
                                "<letter><main><img src='asset:logo'"
                                        + " width='40mm'/></main></letter>")
                        .asset("logo", svg)
                        .build();
        var fluent =
                Letter.builder()
                        .body("SVG assets")
                        .logo(svg)
                        .header(svg)
                        .footer(svg)
                        .signature(ImageSignature.of(svg))
                        .build();
        String json =
                "{\"schemaVersion\":1,\"sections\":[{\"name\":\"body\",\"blocks\":[{\"type\":\"image\","
                        + "\"data\":\""
                        + Base64.getEncoder().encodeToString(svg)
                        + "\",\"mimeType\":\"image/svg+xml\",\"widthMm\":40,\"heightMm\":20}]}]}";
        for (byte[] pdfBytes :
                List.of(
                        generator.generate(typed),
                        generator.generate(html),
                        generator.generate(fluent),
                        generator.generateJson(json))) {
            try (var pdf = Loader.loadPDF(pdfBytes)) {
                assertEquals(1, pdf.getNumberOfPages());
                assertFalse(hasBitmap(pdf.getPage(0).getResources()));
                var rendered = new PDFRenderer(pdf).renderImage(0);
                boolean colored = false;
                for (int y = 0; y < rendered.getHeight(); y++) {
                    for (int x = 0; x < rendered.getWidth(); x++) {
                        int rgb = rendered.getRGB(x, y);
                        if (((rgb >> 16) & 255) != (rgb & 255)) {
                            colored = true;
                        }
                    }
                }
                assertTrue(colored, "SVG must paint visible colored geometry");
            }
        }
    }

    @Test
    void svgWatermarkUsesBoundedBitmapFallback() throws Exception {
        var base = document(List.of(paragraph("SVG watermark")));
        var watermark = ImageWatermark.of(bytes(SVG));
        var withWatermark =
                new LetterDocument(
                        base.schemaVersion(),
                        base.page(),
                        base.defaultStyle(),
                        base.sections(),
                        base.fixedElements(),
                        base.signature(),
                        watermark,
                        base.theme(),
                        base.components());
        try (var pdf =
                Loader.loadPDF(LetterPdfGenerator.builder().build().generate(withWatermark))) {
            assertTrue(hasBitmap(pdf.getPage(0).getResources()));
        }
    }

    @Test
    void directModelValidationRejectsUnsafeAndMalformedSvgBeforeRendering() {
        var generator = LetterPdfGenerator.builder().build();
        for (String svg :
                List.of(
                        "<!DOCTYPE svg [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><svg"
                                + " xmlns='http://www.w3.org/2000/svg' width='10'"
                                + " height='10'>&x;</svg>",
                        "<svg xmlns='http://www.w3.org/2000/svg' width='10'"
                                + " height='10'><script>alert(1)</script></svg>",
                        "<svg xmlns='http://www.w3.org/2000/svg' width='10'"
                                + " height='10'><foreignObject/></svg>",
                        "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><image"
                                + " href='https://example.com/a.png'/></svg>",
                        "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect"
                                + " style='fill:url(file:///etc/passwd)'/></svg>",
                        "<svg xmlns='http://www.w3.org/2000/svg' width='10'"
                                + " height='10'><style>@import"
                                + " 'https://example.com/x.css';</style></svg>",
                        "<svg xmlns='http://www.w3.org/2000/svg' width='0' height='10'/>",
                        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 NaN 10'/>",
                        "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>")) {
            var error =
                    assertThrows(
                            InvalidLetterException.class,
                            () ->
                                    generator.generate(
                                            document(
                                                    List.of(
                                                            new ImageBlock(
                                                                    bytes(svg),
                                                                    "image/svg+xml",
                                                                    10d,
                                                                    10d,
                                                                    null)))));
            assertTrue(error.fieldPath().endsWith(".data"), error.fieldPath());
        }
    }

    private static byte[] bytes(String svg) {
        return svg.getBytes(StandardCharsets.UTF_8);
    }

    private static boolean hasBitmap(PDResources resources) throws Exception {
        for (var name : resources.getXObjectNames()) {
            var object = resources.getXObject(name);
            if (object instanceof PDImageXObject) {
                return true;
            }
            if (object instanceof PDFormXObject form && hasBitmap(form.getResources())) {
                return true;
            }
        }
        return false;
    }
}
