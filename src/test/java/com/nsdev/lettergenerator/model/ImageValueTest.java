package com.nsdev.lettergenerator.model;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.png;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ImageValueTest {
    @Test
    void equivalentImageBlocksHaveValueEqualityAndWorkAsSetKeys() throws Exception {
        byte[] image = png();
        ImageBlock first = new ImageBlock(image, "image/png", 20d, 10d, null);
        ImageBlock second = new ImageBlock(image.clone(), "image/png", 20d, 10d, null);
        Set<ImageBlock> images = new HashSet<>();
        images.add(first);
        assertEquals(first, second);
        assertTrue(images.contains(second));
    }

    @Test
    void equivalentImageWatermarksHaveValueEqualityAndWorkAsSetKeys() throws Exception {
        byte[] image = png();
        ImageWatermark first =
                new ImageWatermark(image, "image/png", 80d, 40d, PageSelector.ALL, 0.2, 0d);
        ImageWatermark second =
                new ImageWatermark(image.clone(), "image/png", 80d, 40d, PageSelector.ALL, 0.2, 0d);
        Set<ImageWatermark> watermarks = new HashSet<>();
        watermarks.add(first);
        assertEquals(first, second);
        assertTrue(watermarks.contains(second));
    }
}
