package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.model.style.Style;
import java.util.Arrays;
import java.util.Objects;

/**
 * PNG/JPEG bytes rendered inside an explicit millimeter box. Bytes are copied defensively and
 * equality compares their contents.
 */
public record ImageBlock(byte[] data, String mimeType, Double widthMm, Double heightMm, Style style)
        implements Block {
    public ImageBlock {
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
        if (!(other instanceof ImageBlock image)) {
            return false;
        }
        return Arrays.equals(data, image.data)
                && Objects.equals(mimeType, image.mimeType)
                && Objects.equals(widthMm, image.widthMm)
                && Objects.equals(heightMm, image.heightMm)
                && Objects.equals(style, image.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(data), mimeType, widthMm, heightMm, style);
    }
}
