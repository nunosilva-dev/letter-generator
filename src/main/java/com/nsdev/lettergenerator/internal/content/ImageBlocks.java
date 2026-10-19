package com.nsdev.lettergenerator.internal.content;

import com.nsdev.lettergenerator.internal.util.ArgumentChecks;
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
        ArgumentChecks.positive(maxWidthMm, "image width");
        ArgumentChecks.positive(maxHeightMm, "image height");
        byte[] data = ArgumentChecks.required(supplied, "image bytes").clone();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            var readers = ImageIO.getImageReaders(input);
            ArgumentChecks.require(
                    readers.hasNext(), "Image must contain readable PNG or JPEG bytes");
            ImageReader reader = readers.next();
            try {
                ImageFormat format =
                        ImageFormat.fromReaderName(reader.getFormatName().toLowerCase(Locale.ROOT));
                ArgumentChecks.require(format != null, "Only PNG and JPEG images are supported");
                reader.setInput(input, true, true);
                var image = reader.read(0);
                double scale =
                        Math.min(maxWidthMm / image.getWidth(), maxHeightMm / image.getHeight());
                return new ImageBlock(
                        data,
                        format.mimeType(),
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
