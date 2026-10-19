package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.content.ImageFormat;
import com.nsdev.lettergenerator.internal.content.SvgImages;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Validates the declared format, byte signature, and actual decoder result. */
final class ImageValidator {
    /** Prevents instantiation of the stateless image checks. */
    private ImageValidator() {}

    /**
     * Requires positive millimeter dimensions, an allowed PNG/JPEG/static SVG/SVG MIME type,
     * matching file signature and successful decoding of nonempty supplied bytes. No paths or URLs
     * are read.
     *
     * @param data encoded image bytes, required and checked against the declared type
     * @param mimeType exact PNG, JPEG or SVG MIME label
     * @param width required positive finite fitting-box width in millimeters
     * @param height required positive finite fitting-box height in millimeters
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if dimensions, bytes, MIME
     *     type, signature, or decoding are invalid
     */
    static void validate(byte[] data, String mimeType, Double width, Double height, String path) {
        ValidationChecks.positive(width, path + ".widthMm");
        ValidationChecks.positive(height, path + ".heightMm");
        ValidationChecks.required(data, path + ".data");
        ImageFormat format = ImageFormat.fromMimeType(mimeType);
        ValidationChecks.require(
                format != null, path + ".mimeType", "only PNG, JPEG and static SVG are supported");
        if (format == ImageFormat.SVG) {
            try {
                SvgImages.inspect(data);
                return;
            } catch (IllegalArgumentException exception) {
                throw new InvalidLetterException(
                        path + ".data",
                        "invalid or unsafe SVG: " + exception.getMessage(),
                        exception);
            }
        }
        ValidationChecks.require(
                format.matchesSignature(data),
                path + ".data",
                "image bytes do not match MIME type");
        try (InputStream input = new ByteArrayInputStream(data)) {
            BufferedImage image = ImageIO.read(input);
            ValidationChecks.require(
                    image != null && image.getWidth() > 0 && image.getHeight() > 0,
                    path + ".data",
                    "invalid image");
        } catch (IOException exception) {
            throw new InvalidLetterException(path + ".data", "invalid image", exception);
        }
    }
}
