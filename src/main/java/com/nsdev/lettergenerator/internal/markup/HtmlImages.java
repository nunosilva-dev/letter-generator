package com.nsdev.lettergenerator.internal.markup;

import com.nsdev.lettergenerator.internal.builder.ImageBlocks;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.Map;
import org.jsoup.nodes.Element;

/** Resolves named application assets without loading document-controlled paths or URLs. */
public final class HtmlImages {
    private static final String ASSET_PREFIX = "asset:";
    private static final double DEFAULT_WIDTH_MM = 60.0;
    private static final double DEFAULT_HEIGHT_MM = 40.0;

    private HtmlImages() {}

    public static ImageBlock read(
            Element element,
            Map<String, byte[]> assets,
            double availableWidth,
            double availableHeight,
            boolean region,
            Style inherited) {
        MarkupSupport.attributes(element, "src", "width", "height", "alt");
        String source = element.attr("src");
        MarkupSupport.require(
                source.startsWith(ASSET_PREFIX) && source.length() > ASSET_PREFIX.length(),
                element,
                "Image src must reference a named asset: asset:name");
        byte[] bytes = assets.get(source.substring(ASSET_PREFIX.length()));
        MarkupSupport.require(bytes != null, element, "Missing image asset: " + source);
        var appearance = HtmlStyles.read(element);
        MarkupSupport.require(
                appearance.background() == null, element, "Image background-color is unsupported");
        String widthValue = appearance.width() == null ? element.attr("width") : appearance.width();
        String heightValue =
                appearance.height() == null ? element.attr("height") : appearance.height();
        double width =
                widthValue.isBlank()
                        ? Math.min(availableWidth, region ? availableWidth : DEFAULT_WIDTH_MM)
                        : HtmlStyles.lengthMm(widthValue, availableWidth, true, element);
        double height =
                heightValue.isBlank()
                        ? widthValue.isBlank()
                                ? Math.min(
                                        availableHeight,
                                        region ? availableHeight : DEFAULT_HEIGHT_MM)
                                : Double.MAX_VALUE
                        : HtmlStyles.lengthMm(heightValue, availableHeight, true, element);
        MarkupSupport.require(
                width > 0 && height > 0 && width <= availableWidth,
                element,
                "Image dimensions must be positive and fit the region width");
        try {
            ImageBlock image = ImageBlocks.fit(bytes, width, height);
            return new ImageBlock(
                    image.data(),
                    image.mimeType(),
                    image.widthMm(),
                    image.heightMm(),
                    HtmlStyles.inherited(inherited).overlay(appearance.style()));
        } catch (IllegalArgumentException exception) {
            throw MarkupSupport.error(
                    element, "Invalid image asset " + source + ": " + exception.getMessage());
        }
    }
}
