package com.nsdev.lettergenerator.signature;

import com.nsdev.lettergenerator.internal.content.ImageBlocks;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Immutable PNG/JPEG/static SVG signature part owning a defensive copy of its image bytes.
 * Factories decode the image immediately and preserve its aspect ratio within a 40 by 20 mm box.
 * {@link #sizeMm(double, double)} creates another fitted part without mutating this one. Placement
 * within the letter is configured on the letter builder.
 */
public final class ImageSignature implements SignatureContent {
    /** Maximum width in millimeters for the initial signature image fit. */
    private static final double DEFAULT_WIDTH_MM = 40.0;

    /** Maximum height in millimeters for the initial signature image fit. */
    private static final double DEFAULT_HEIGHT_MM = 20.0;

    /** Owned image snapshot containing original bytes and the current fitted dimensions. */
    private final ImageBlock image;

    /**
     * Retains an already-decoded immutable fitted image snapshot.
     *
     * @param image owned fitted image block
     */
    private ImageSignature(ImageBlock image) {
        this.image = image;
    }

    /**
     * Copies and decodes image bytes, fitting the aspect ratio inside a 40 by 20 mm box. The fit
     * can enlarge a small source image as well as reduce a large one.
     *
     * @param data non-null readable PNG, JPEG or static SVG image bytes
     * @return a new immutable image signature
     * @throws IllegalArgumentException if bytes are null, unreadable, or not PNG/JPEG/static SVG
     */
    public static ImageSignature of(byte[] data) {
        return new ImageSignature(ImageBlocks.fit(data, DEFAULT_WIDTH_MM, DEFAULT_HEIGHT_MM));
    }

    /**
     * Reads and decodes an image immediately, fitting it inside a 40 by 20 mm box. The resulting
     * signature no longer depends on the file.
     *
     * @param path non-null PNG, JPEG or static SVG file path
     * @return a new immutable image signature
     * @throws IOException if the file cannot be read
     * @throws NullPointerException if {@code path} is null
     * @throws IllegalArgumentException if file bytes are not a readable PNG, JPEG or static SVG
     *     image
     */
    public static ImageSignature of(Path path) throws IOException {
        return of(Files.readAllBytes(path));
    }

    /**
     * Creates a signature refitted from the original image bytes into the given box. The aspect
     * ratio is preserved, so at least one resulting dimension may be smaller than the box. This
     * operation may enlarge the image; it does not mutate this signature.
     *
     * @param maxWidth finite positive maximum width in millimeters
     * @param maxHeight finite positive maximum height in millimeters
     * @return a new fitted immutable image signature
     * @throws IllegalArgumentException if either dimension is nonpositive or nonfinite
     */
    public ImageSignature sizeMm(double maxWidth, double maxHeight) {
        return new ImageSignature(ImageBlocks.fit(image, maxWidth, maxHeight));
    }

    /** {@inheritDoc} */
    @Override
    public List<Block> blocks() {
        return List.of(image);
    }
}
