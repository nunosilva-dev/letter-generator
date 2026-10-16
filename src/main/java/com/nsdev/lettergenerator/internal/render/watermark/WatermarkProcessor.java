package com.nsdev.lettergenerator.internal.render.watermark;

import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.font.RegisteredFont;
import com.nsdev.lettergenerator.internal.font.StandardFontFamilies;
import com.nsdev.lettergenerator.internal.layout.Units;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

/** Optional PDFBox boundary; its state and font/image objects belong to one PDF document. */
public final class WatermarkProcessor {
    private static final String WATERMARK_IMAGE_NAME = "watermark";

    private WatermarkProcessor() {}

    public static byte[] apply(byte[] source, Watermark watermark, FontRegistry fonts) {
        try (PDDocument pdf = Loader.loadPDF(source);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            WatermarkPainter painter = createPainter(pdf, watermark, fonts);
            PDExtendedGraphicsState opacity = opacity(watermark);
            for (int index = 0; index < pdf.getNumberOfPages(); index++) {
                if (watermark.pages().matchesPageIndex(index)) {
                    paintPage(pdf, pdf.getPage(index), watermark, opacity, painter);
                }
            }
            pdf.save(output);
            return output.toByteArray();
        } catch (IOException | IllegalArgumentException exception) {
            throw new PdfGenerationException("Unable to apply requested watermark", exception);
        }
    }

    private static WatermarkPainter createPainter(
            PDDocument pdf, Watermark watermark, FontRegistry fonts) throws IOException {
        if (watermark instanceof TextWatermark text) {
            PDFont font = loadFont(pdf, text.fontFamily(), fonts);
            return stream -> paintText(stream, text, font);
        }
        ImageWatermark image = (ImageWatermark) watermark;
        PDImageXObject bitmap =
                PDImageXObject.createFromByteArray(pdf, image.data(), WATERMARK_IMAGE_NAME);
        return stream -> paintImage(stream, image, bitmap);
    }

    private static PDFont loadFont(PDDocument pdf, String family, FontRegistry fonts)
            throws IOException {
        Optional<RegisteredFont> registered = fonts.regular(family);
        if (registered.isPresent()) {
            try (InputStream input = registered.get().openStream()) {
                return PDType0Font.load(pdf, input, true);
            }
        }
        Standard14Fonts.FontName name =
                switch (family) {
                    case StandardFontFamilies.TIMES -> Standard14Fonts.FontName.TIMES_ROMAN;
                    case StandardFontFamilies.COURIER -> Standard14Fonts.FontName.COURIER;
                    case StandardFontFamilies.HELVETICA -> Standard14Fonts.FontName.HELVETICA;
                    default ->
                            throw new IllegalArgumentException(
                                    "Unregistered font family: " + family);
                };
        return new PDType1Font(name);
    }

    private static PDExtendedGraphicsState opacity(Watermark watermark) {
        PDExtendedGraphicsState state = new PDExtendedGraphicsState();
        state.setNonStrokingAlphaConstant(watermark.opacity().floatValue());
        state.setStrokingAlphaConstant(watermark.opacity().floatValue());
        return state;
    }

    private static void paintPage(
            PDDocument pdf,
            PDPage page,
            Watermark watermark,
            PDExtendedGraphicsState opacity,
            WatermarkPainter painter)
            throws IOException {
        PDRectangle media = page.getMediaBox();
        float centerX = media.getLowerLeftX() + media.getWidth() / 2;
        float centerY = media.getLowerLeftY() + media.getHeight() / 2;
        try (PDPageContentStream stream =
                new PDPageContentStream(
                        pdf, page, PDPageContentStream.AppendMode.PREPEND, true, false)) {
            stream.saveGraphicsState();
            stream.setGraphicsStateParameters(opacity);
            stream.transform(
                    Matrix.getRotateInstance(
                            Math.toRadians(watermark.rotationDegrees()), centerX, centerY));
            painter.paint(stream);
            stream.restoreGraphicsState();
        }
    }

    private static void paintText(PDPageContentStream stream, TextWatermark text, PDFont font)
            throws IOException {
        float size = text.fontSizePt().floatValue();
        float width = (float) Units.fontUnitsToPoints(font.getStringWidth(text.text()), size);
        float capHeight =
                (float) Units.fontUnitsToPoints(font.getFontDescriptor().getCapHeight(), size);
        stream.setNonStrokingColor(Color.decode(text.color()));
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(-width / 2, -capHeight / 2);
        stream.showText(text.text());
        stream.endText();
    }

    private static void paintImage(
            PDPageContentStream stream, ImageWatermark image, PDImageXObject bitmap)
            throws IOException {
        float width = (float) Units.millimetersToPoints(image.widthMm());
        float height = (float) Units.millimetersToPoints(image.heightMm());
        float scale = Math.min(width / bitmap.getWidth(), height / bitmap.getHeight());
        float actualWidth = bitmap.getWidth() * scale;
        float actualHeight = bitmap.getHeight() * scale;
        stream.drawImage(bitmap, -actualWidth / 2, -actualHeight / 2, actualWidth, actualHeight);
    }

    @FunctionalInterface
    private interface WatermarkPainter {
        void paint(PDPageContentStream stream) throws IOException;
    }
}
