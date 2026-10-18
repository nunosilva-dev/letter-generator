package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import java.util.Locale;
import org.jsoup.nodes.Element;

/** Signature placement, reserved height and styled text/image content. */
final class SignatureTagHandler implements TemplateTagHandler {
    private static final double DEFAULT_HEIGHT_MM = 20.0;

    @Override
    public String tagName() {
        return "signature";
    }

    @Override
    public void handle(Element element, TemplateContext context) {
        SignaturePlacement placement;
        try {
            placement =
                    element.hasAttr("placement")
                            ? SignaturePlacement.valueOf(
                                    element.attr("placement")
                                            .toUpperCase(Locale.ROOT)
                                            .replace('-', '_'))
                            : SignaturePlacement.FLOW;
        } catch (IllegalArgumentException exception) {
            throw MarkupSupport.error(element, "Signature placement must be flow or bottom-last");
        }
        double height = TemplateAttributes.number(element, "height-mm", DEFAULT_HEIGHT_MM);
        MarkupSupport.require(
                Double.isFinite(height) && height > 0,
                element,
                "Signature height-mm must be finite and positive");
        context.builder()
                .signaturePlacement(placement)
                .signatureHeightMm(height)
                .signatureBlocks(
                        TemplateBlocks.read(
                                        element, context, height, false, "placement", "height-mm")
                                .toArray(Block[]::new));
    }
}
