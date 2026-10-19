package com.nsdev.lettergenerator.internal.content;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

/** Bounded raster fallback used exclusively for PDFBox image watermarks. */
public final class SvgRasterizer {
    /** Maximum bitmap extent on either axis, limiting the fallback to at most four megapixels. */
    private static final int MAX_PIXELS = 2048;

    /** Target sampling density for ordinary watermark sizes. */
    private static final double PIXELS_PER_INCH = 150;

    /** Physical millimeters per inch. */
    private static final double MILLIMETERS_PER_INCH = 25.4;

    /** Prevents construction of this stateless fallback utility. */
    private SvgRasterizer() {}

    /**
     * Revalidates static SVG before rasterizing it once per PDF watermark application. The bitmap
     * preserves the intrinsic viewport ratio at 150 dpi inside the requested millimeter box, with
     * its largest dimension capped at 2048 pixels. Batik script execution and external resources
     * are disabled in addition to the shared static-content checks.
     *
     * @param data encoded SVG bytes
     * @param widthMm validated positive maximum watermark width
     * @param heightMm validated positive maximum watermark height
     * @return new PNG bytes suitable for a document-local PDFBox image
     * @throws IllegalArgumentException if validation or transcoding fails
     */
    public static byte[] png(byte[] data, double widthMm, double heightMm) {
        SvgImages.Dimensions size = SvgImages.inspect(data);
        double scale =
                Math.min(widthMm / size.width(), heightMm / size.height())
                        * PIXELS_PER_INCH
                        / MILLIMETERS_PER_INCH;
        scale = Math.min(scale, MAX_PIXELS / Math.max(size.width(), size.height()));
        var transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(
                SVGAbstractTranscoder.KEY_WIDTH, (float) Math.max(1, size.width() * scale));
        transcoder.addTranscodingHint(
                SVGAbstractTranscoder.KEY_HEIGHT, (float) Math.max(1, size.height() * scale));
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_EXECUTE_ONLOAD, Boolean.FALSE);
        transcoder.addTranscodingHint(
                SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES, Boolean.FALSE);
        try {
            var output = new ByteArrayOutputStream();
            transcoder.transcode(
                    new TranscoderInput(new ByteArrayInputStream(SvgImages.renderingBytes(data))),
                    new TranscoderOutput(output));
            return output.toByteArray();
        } catch (TranscoderException exception) {
            throw new IllegalArgumentException("Unable to rasterize SVG watermark", exception);
        }
    }
}
