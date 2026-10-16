package com.nsdev.lettergenerator.model.watermark;

import com.nsdev.lettergenerator.internal.builder.ImageBlocks;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * PNG/JPEG watermark with an explicit millimeter box. Byte contents participate in value equality.
 */
public record ImageWatermark(
        byte[] data,
        String mimeType,
        Double widthMm,
        Double heightMm,
        PageSelector pages,
        Double opacity,
        Double rotationDegrees)
        implements Watermark {
    private static final double DEFAULT_WIDTH_MM = 80.0;
    private static final double DEFAULT_HEIGHT_MM = 40.0;
    private static final double DEFAULT_OPACITY = 0.12;
    private static final double DEFAULT_ROTATION_DEGREES = 0.0;

    public static ImageWatermark of(byte[] data) {
        var image = ImageBlocks.fit(data, DEFAULT_WIDTH_MM, DEFAULT_HEIGHT_MM);
        return new ImageWatermark(
                image.data(),
                image.mimeType(),
                image.widthMm(),
                image.heightMm(),
                PageSelector.ALL,
                DEFAULT_OPACITY,
                DEFAULT_ROTATION_DEGREES);
    }

    public static ImageWatermark of(Path path) throws IOException {
        return of(Files.readAllBytes(path));
    }

    public ImageWatermark {
        data = data == null ? null : data.clone();
    }

    @Override
    public byte[] data() {
        return data == null ? null : data.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ImageWatermark image)) {
            return false;
        }
        return Arrays.equals(data, image.data)
                && Objects.equals(mimeType, image.mimeType)
                && Objects.equals(widthMm, image.widthMm)
                && Objects.equals(heightMm, image.heightMm)
                && Objects.equals(pages, image.pages)
                && Objects.equals(opacity, image.opacity)
                && Objects.equals(rotationDegrees, image.rotationDegrees);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Arrays.hashCode(data),
                mimeType,
                widthMm,
                heightMm,
                pages,
                opacity,
                rotationDegrees);
    }
}
