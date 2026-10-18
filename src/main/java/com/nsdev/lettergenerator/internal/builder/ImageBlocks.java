package com.nsdev.lettergenerator.internal.builder;

import com.nsdev.lettergenerator.model.block.ImageBlock;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * Reads supplied image bytes, detects their supported format and fits their intrinsic aspect ratio.
 */
public final class ImageBlocks {
    /** ImageIO format identifier allowed for PNG bytes. */
    private static final String PNG_FORMAT = "png";

    /** ImageIO format identifier allowed for JPEG bytes. */
    private static final String JPEG_FORMAT = "jpeg";

    /** Prevents construction of this static helper. */
    private ImageBlocks() {}

    /**
     * Decodes caller bytes and scales their intrinsic pixel aspect ratio into a millimeter box. The
     * first ImageIO reader must report PNG or JPEG. Scaling may enlarge small images; no raster
     * resampling occurs because the result retains the encoded bytes and stores display dimensions.
     * The caller's byte array is copied before decoding.
     *
     * @param supplied non-null readable PNG or JPEG bytes
     * @param maxWidthMm finite positive maximum display width
     * @param maxHeightMm finite positive maximum display height
     * @return an image with copied bytes, detected MIME type, fitted dimensions and null style
     * @throws IllegalArgumentException if dimensions, format or encoded data are invalid
     */
    public static ImageBlock fit(byte[] supplied, double maxWidthMm, double maxHeightMm) {
        BuilderChecks.positive(maxWidthMm, "image width");
        BuilderChecks.positive(maxHeightMm, "image height");
        byte[] data = BuilderChecks.required(supplied, "image bytes").clone();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            var readers = ImageIO.getImageReaders(input);
            BuilderChecks.require(
                    readers.hasNext(), "Image must contain readable PNG or JPEG bytes");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                BuilderChecks.require(
                        PNG_FORMAT.equals(format) || JPEG_FORMAT.equals(format),
                        "Only PNG and JPEG images are supported");
                reader.setInput(input, true, true);
                var image = reader.read(0);
                double scale =
                        Math.min(maxWidthMm / image.getWidth(), maxHeightMm / image.getHeight());
                return new ImageBlock(
                        data,
                        "image/" + format,
                        image.getWidth() * scale,
                        image.getHeight() * scale,
                        null);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to decode image", exception);
        }
    }

    /**
     * Refits from encoded bytes, discarding the input block's dimensions and style.
     *
     * @param image non-null image whose encoded data is decoded again
     * @param maxWidthMm finite positive maximum display width in millimeters
     * @param maxHeightMm finite positive maximum display height in millimeters
     * @return a newly fitted, unstyled block
     * @throws IllegalArgumentException if the box or encoded image is invalid
     * @throws NullPointerException if image is null
     */
    public static ImageBlock fit(ImageBlock image, double maxWidthMm, double maxHeightMm) {
        return fit(image.data(), maxWidthMm, maxHeightMm);
    }
}
