package com.nsdev.lettergenerator.internal.render.watermark;

import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.internal.content.ImageFormat;
import com.nsdev.lettergenerator.internal.content.SvgRasterizer;
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

/**
 * Optional PDFBox post-processing stage invoked only after FOP layout has succeeded.
 *
 * <p>Selected pages receive a prepended content stream, so existing opaque page content may cover
 * the watermark. Text/images are centered and rotated around the media-box center using supplied
 * font/image bytes. PDFBox objects and streams belong to this call; the shared generator retains
 * none.
 */
public final class WatermarkProcessor {
    /** Diagnostic name assigned to the document-local PDFBox watermark image object. */
    private static final String WATERMARK_IMAGE_NAME = "watermark";

    /** Prevents construction of the PDF watermark post-processing utility. */
    private WatermarkProcessor() {}

    /**
     * Adds a validated text or image watermark behind selected pages of a complete PDF.
     *
     * @param source accepted PDF bytes from the FOP stage
     * @param watermark non-null validated watermark appearance and page selector
     * @param fonts generator font snapshot used for regular text-watermark faces
     * @return newly serialized PDF bytes after all selected pages are processed
     * @throws PdfGenerationException if loading, painting or serializing the watermark fails
     */
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

    /**
     * Loads one document-local PDFBox font/image object and returns a painter reusable across this
     * PDF's selected pages.
     *
     * @param pdf document owning the loaded PDFBox font or image resource
     * @param watermark validated text or image appearance to paint
     * @param fonts immutable registered fonts available to text watermarks
     * @return painter retaining the document-local resource for reuse on selected pages
     * @throws IOException if PDFBox cannot load the registered font or image bytes
     */
    private static WatermarkPainter createPainter(
            PDDocument pdf, Watermark watermark, FontRegistry fonts) throws IOException {
        if (watermark instanceof TextWatermark text) {
            PDFont font = loadFont(pdf, text.fontFamily(), fonts);
            return stream -> paintText(stream, text, font);
        }
        ImageWatermark image = (ImageWatermark) watermark;
        byte[] data = image.data();
        if (ImageFormat.SVG.mimeType().equals(image.mimeType())) {
            data = SvgRasterizer.png(data, image.widthMm(), image.heightMm());
        }
        PDImageXObject bitmap = PDImageXObject.createFromByteArray(pdf, data, WATERMARK_IMAGE_NAME);
        return stream -> paintImage(stream, image, bitmap);
    }

    /**
     * Loads a registered regular font from its snapshotted bytes, or selects the matching built-in
     * PDF standard face; no font path or URL is resolved.
     *
     * @param pdf document owning a loaded custom font subset
     * @param family validated custom or built-in family identifier
     * @param fonts immutable registry searched for a regular custom face first
     * @return document-local regular face for watermark text
     * @throws IOException if a registered font stream cannot be read by PDFBox
     * @throws IllegalArgumentException if the family has neither a registered regular face nor a
     *     supported built-in identifier
     */
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

    /**
     * Creates matching stroking/nonstroking alpha values for both text and image painting.
     *
     * @param watermark validated watermark supplying an opacity from zero to one
     * @return graphics state with equal stroking and nonstroking alpha constants
     */
    private static PDExtendedGraphicsState opacity(Watermark watermark) {
        PDExtendedGraphicsState state = new PDExtendedGraphicsState();
        state.setNonStrokingAlphaConstant(watermark.opacity().floatValue());
        state.setStrokingAlphaConstant(watermark.opacity().floatValue());
        return state;
    }

    /**
     * Prepends an isolated graphics state, translates/rotates to the media-box center and invokes
     * the document-local painter.
     *
     * @param pdf document owning the page and its new prepended content stream
     * @param page selected page receiving watermark content behind existing content
     * @param watermark validated appearance supplying rotation in degrees
     * @param opacity shared document-local alpha settings for this watermark
     * @param painter callback painting relative to the rotated media-box center
     * @throws IOException if writing or closing the page content stream fails
     */
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

    /**
     * Centers a single native text line horizontally by measured width and vertically by cap
     * height; descenders/lowercase shapes are approximate.
     *
     * @param stream open page stream already translated and rotated to the watermark center
     * @param text validated single-line text, point size and RGB color
     * @param font regular face used for glyph metrics and PDF text painting
     * @throws IOException if font measurement or content-stream writing fails
     */
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

    /**
     * Fits the bitmap uniformly inside the model's millimeter box, converts to PDF points and
     * centers the fitted rectangle.
     *
     * @param stream open page stream already translated and rotated to the watermark center
     * @param image validated maximum fitting dimensions in millimeters
     * @param bitmap decoded document-local image with intrinsic pixel dimensions
     * @throws IOException if writing the image operation to the page stream fails
     */
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

    /**
     * Document-local callback invoked inside each selected page's centered/rotated graphics state.
     */
    @FunctionalInterface
    private interface WatermarkPainter {
        /**
         * Paints one watermark around the origin of an already centered and rotated page stream.
         * The caller owns and closes the stream and restores its graphics state.
         *
         * @param stream open stream carrying the watermark opacity and page transformation
         * @throws IOException if font/image painting or writing the stream fails
         */
        void paint(PDPageContentStream stream) throws IOException;
    }
}
