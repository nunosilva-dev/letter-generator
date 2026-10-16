package com.nsdev.lettergenerator.fixtures;

import java.io.File;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

/** Test-only PDF inspection and visual-output helpers. */
public final class PdfTestSupport {
    private PdfTestSupport() {}

    public static String text(byte[] bytes) throws Exception {
        try (var pdf = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(pdf);
        }
    }

    public static void renderFirst(byte[] bytes, String name) throws Exception {
        try (var pdf = Loader.loadPDF(bytes)) {
            ImageIO.write(
                    new PDFRenderer(pdf).renderImageWithDPI(0, 100),
                    "png",
                    new File("target/examples/" + name + ".png"));
        }
    }
}
