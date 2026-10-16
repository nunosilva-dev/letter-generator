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
    private static final String PNG_MIME_TYPE = "image/png";
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private ImageValidator() {}

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

    private static boolean startsWith(byte[] data, byte[] signature) {
        return data.length >= signature.length
                && Arrays.equals(data, 0, signature.length, signature, 0, signature.length);
    }
}
