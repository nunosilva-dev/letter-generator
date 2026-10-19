package com.nsdev.lettergenerator.internal.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class SvgDimensionsTest {
    @Test
    void absoluteUnitsAndMissingDimensionUseTheIntrinsicRatio() {
        for (String dimensions :
                List.of(
                        "width='2in' height='1in'",
                        "width='50.8mm' height='25.4mm'",
                        "width='144pt' height='72pt'",
                        "width='192px' height='96px'",
                        "width='192' viewBox='0 0 200 100'",
                        "height='96' viewBox='0 0 200 100'")) {
            var image = ImageBlocks.fit(svg(dimensions, "<path d='M0 0L1 1'/>"), 40, 40);
            assertEquals(40, image.widthMm());
            assertEquals(20, image.heightMm());
        }
    }

    @Test
    void unsafeReferencesAttributesAndUnboundedGeometryAreRejected() {
        for (String content :
                List.of(
                        "<rect onclick='alert(1)'/>",
                        "<a href='https://example.com'><rect/></a>",
                        "<rect fill='url(data:image/png;base64,AA)'/>",
                        "<rect style='fill:u\\72l(https://example.com)'/>",
                        "<rect xml:base='file:///tmp/'/>",
                        "<use href='#a' id='a'/>",
                        "<g id='a'><use href='#a'/></g>",
                        "<use href='#missing'/>",
                        "<?xml-stylesheet href='file:///tmp/a.css'?>",
                        "<animate attributeName='x'/>",
                        "<filter id='f'><feImage href='file:///tmp/a.png'/></filter>")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ImageBlocks.fit(svg("width='100' height='50'", content), 40, 40),
                    content);
        }
        for (String dimensions :
                List.of(
                        "width='100%' height='50%'",
                        "width='Infinity' height='1'",
                        "width='1e-320' height='1e-320'",
                        "viewBox='0 0 -1 10'",
                        "width='1' height='1' viewBox='0 0 10'")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ImageBlocks.fit(svg(dimensions, ""), 40, 40),
                    dimensions);
        }
    }

    @Test
    void recursiveUseExpansionIsBoundedBeforeRendering() {
        var content = new StringBuilder("<defs><g id='a0'><rect width='1' height='1'/></g>");
        for (int index = 1; index <= 20; index++) {
            content.append("<g id='a")
                    .append(index)
                    .append("'><use href='#a")
                    .append(index - 1)
                    .append("'/><use href='#a")
                    .append(index - 1)
                    .append("'/></g>");
        }
        content.append("</defs><use href='#a20'/>");
        assertThrows(
                IllegalArgumentException.class,
                () -> ImageBlocks.fit(svg("width='100' height='100'", content.toString()), 40, 40));
    }

    private static byte[] svg(String dimensions, String content) {
        return ("<svg xmlns='http://www.w3.org/2000/svg' " + dimensions + ">" + content + "</svg>")
                .getBytes(StandardCharsets.UTF_8);
    }
}
