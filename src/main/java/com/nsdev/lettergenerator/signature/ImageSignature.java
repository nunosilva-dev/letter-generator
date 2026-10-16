package com.nsdev.lettergenerator.signature;

import com.nsdev.lettergenerator.internal.builder.ImageBlocks;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** PNG/JPEG signature fitted into a 40 x 20 mm box, with immutable ownership of image bytes. */
public final class ImageSignature implements SignatureContent {
    private static final double DEFAULT_WIDTH_MM = 40.0;
    private static final double DEFAULT_HEIGHT_MM = 20.0;
    private final ImageBlock image;

    private ImageSignature(ImageBlock image) {
        this.image = image;
    }

    public static ImageSignature of(byte[] data) {
        return new ImageSignature(ImageBlocks.fit(data, DEFAULT_WIDTH_MM, DEFAULT_HEIGHT_MM));
    }

    public static ImageSignature of(Path path) throws IOException {
        return of(Files.readAllBytes(path));
    }

    /** Returns a new signature fitted inside the given box, retaining its aspect ratio. */
    public ImageSignature sizeMm(double maxWidth, double maxHeight) {
        return new ImageSignature(ImageBlocks.fit(image, maxWidth, maxHeight));
    }

    @Override
    public List<Block> blocks() {
        return List.of(image);
    }
}
