package com.nsdev.lettergenerator.fixtures;

import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.layout.Zone;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Reusable input fixtures, independent of JUnit test classes. */
public final class LetterFixtures {
    private LetterFixtures() {}

    public static Paragraph paragraph(String text) {
        return new Paragraph(List.of(new TextRun(text, null, null)), null);
    }

    public static LetterDocument document(List<Block> blocks) {
        return new LetterDocument(
                1, null, null, List.of(new Section("body", null, blocks)), null, null, null);
    }

    public static byte[] png() throws Exception {
        var image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0, 0, 20, 10);
        g.dispose();
        var out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    public static LetterDocument layout(int lines, SignaturePlacement placement) {
        var blocks = new ArrayList<Block>();
        for (int i = 0; i < lines; i++) {
            blocks.add(paragraph("BODY_" + i + " Content to demonstrate reserved zones."));
        }
        return new LetterDocument(
                1,
                new PageSpec(null, null, 25d, 15d, 32d),
                null,
                List.of(new Section("body", null, blocks)),
                List.of(
                        new FixedElement(
                                "first",
                                Zone.TOP,
                                PageSelector.FIRST,
                                new Bounds(0, 0, 170, 20),
                                List.of(paragraph("FIRST HEADER"))),
                        new FixedElement(
                                "following",
                                Zone.TOP,
                                PageSelector.FOLLOWING,
                                new Bounds(0, 0, 170, 20),
                                List.of(paragraph("FOLLOWING HEADER"))),
                        new FixedElement(
                                "footer",
                                Zone.BOTTOM,
                                PageSelector.ALL,
                                new Bounds(0, 0, 170, 15),
                                List.of(paragraph("REPEATED FOOTER")))),
                new Signature(
                        placement,
                        List.of(paragraph("SIGNATURE START"), paragraph("SIGNATURE END")),
                        placement == SignaturePlacement.BOTTOM_LAST
                                ? new Bounds(0, 2, 100, 25)
                                : null),
                null);
    }

    public static String onePageJson() throws Exception {
        return TestResources.text("letters/one-page.json");
    }
}
