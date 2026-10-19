package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.internal.content.SvgRasterizer;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

class SvgPaintBoundsTest {
    @Test
    void incompleteSvgViewportPaintsTheDeclaredPhysicalRectangle() throws Exception {
        for (String dimensions :
                List.of("", " width='120'", " height='60'", " width='120' height='60'")) {
            byte[] data = svg(dimensions);
            var image = new ImageBlock(data, "image/svg+xml", 40d, 20d, null);
            byte[] pdfBytes = new LetterPdfGenerator().generate(document(List.of(image)));
            assertArrayEquals(
                    data, image.data(), "Rendering must preserve original supplied bytes");
            try (var pdf = Loader.loadPDF(pdfBytes)) {
                var page = pdf.getPage(0);
                assertFalse(
                        page.getResources().getXObjectNames().iterator().hasNext(),
                        "A solid SVG rectangle must remain vector content");
                var rendered = new PDFRenderer(pdf).renderImageWithDPI(0, 144);
                int[] bounds = redBounds(rendered);
                assertEquals(20, bounds[0] * 25.4 / 144, 0.4, dimensions + " left");
                assertEquals(20, bounds[1] * 25.4 / 144, 0.4, dimensions + " top");
                assertEquals(40, bounds[2] * 25.4 / 144, 0.4, dimensions + " width");
                assertEquals(20, bounds[3] * 25.4 / 144, 0.4, dimensions + " height");
            }
        }
    }

    @Test
    void incompleteSvgViewportFillsTheBoundedWatermarkBitmap() throws Exception {
        for (String dimensions :
                List.of("", " width='120'", " height='60'", " width='120' height='60'")) {
            var bitmap =
                    ImageIO.read(
                            new ByteArrayInputStream(SvgRasterizer.png(svg(dimensions), 40, 20)));
            int[] bounds = redBounds(bitmap);
            assertEquals(0, bounds[0]);
            assertEquals(0, bounds[1]);
            assertEquals(bitmap.getWidth(), bounds[2], 1);
            assertEquals(bitmap.getHeight(), bounds[3], 1);
            assertEquals(2, bitmap.getWidth() / (double) bitmap.getHeight(), 0.02);
            assertTrue(bitmap.getWidth() <= 2048 && bitmap.getHeight() <= 2048);
        }
    }

    private static byte[] svg(String dimensions) {
        return ("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 60'"
                        + dimensions
                        + ">"
                        + "<rect width='120' height='60' fill='#FF0000'/></svg>")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static int[] redBounds(BufferedImage image) {
        int left = image.getWidth();
        int top = image.getHeight();
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if ((pixel >>> 24) > 200
                        && ((pixel >>> 16) & 255) > 200
                        && ((pixel >>> 8) & 255) < 60
                        && (pixel & 255) < 60) {
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        assertTrue(right >= left && bottom >= top, "Expected visible red SVG geometry");
        return new int[] {left, top, right - left + 1, bottom - top + 1};
    }
}
