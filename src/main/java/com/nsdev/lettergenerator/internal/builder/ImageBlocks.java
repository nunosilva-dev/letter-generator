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
    private static final String PNG_FORMAT = "png";
    private static final String JPEG_FORMAT = "jpeg";

    private ImageBlocks() {}

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

    public static ImageBlock fit(ImageBlock image, double maxWidthMm, double maxHeightMm) {
        return fit(image.data(), maxWidthMm, maxHeightMm);
    }
}
