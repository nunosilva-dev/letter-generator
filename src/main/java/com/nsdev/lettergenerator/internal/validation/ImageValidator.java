package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Validates the declared format, byte signature, and actual decoder result. */
final class ImageValidator {
    /** Exact MIME label accepted for PNG data. */
    private static final String PNG_MIME_TYPE = "image/png";

    /** Exact MIME label accepted for JPEG data. */
    private static final String JPEG_MIME_TYPE = "image/jpeg";

    /** Complete PNG magic bytes checked before attempting a decode. */
    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    /** JPEG start-of-image prefix checked before attempting a decode. */
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /** Prevents instantiation of the stateless image checks. */
    private ImageValidator() {}

    /**
     * Requires positive millimeter dimensions, an allowed PNG/JPEG MIME type, matching file
     * signature and successful decoding of nonempty supplied bytes. No paths or URLs are read.
     *
     * @param data encoded image bytes, required and checked against the declared type
     * @param mimeType exact PNG or JPEG MIME label
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
        ValidationChecks.require(
                PNG_MIME_TYPE.equals(mimeType) || JPEG_MIME_TYPE.equals(mimeType),
                path + ".mimeType",
                "only PNG and JPEG are supported");
        byte[] signature = PNG_MIME_TYPE.equals(mimeType) ? PNG_SIGNATURE : JPEG_SIGNATURE;
        ValidationChecks.require(
                startsWith(data, signature), path + ".data", "image bytes do not match MIME type");
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

    /**
     * Compares a complete encoded-image prefix without reading beyond either byte array.
     *
     * @param data nonnull encoded data
     * @param signature nonnull expected leading bytes
     * @return true if data has at least the prefix length and begins with all signature bytes
     */
    private static boolean startsWith(byte[] data, byte[] signature) {
        return data.length >= signature.length
                && Arrays.equals(data, 0, signature.length, signature, 0, signature.length);
    }
}
