package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.layout.PageGeometry;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.layout.Zone;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Coordinates validation of document content, geometry, and optional decorations. */
public final class InputValidator {
    private final FontRegistry fonts;
    private final StyleValidator styles;
    private final BlockValidator blocks;

    public InputValidator(FontRegistry fonts) {
        this.fonts = fonts;
        this.styles = new StyleValidator(fonts);
        this.blocks = new BlockValidator(fonts, styles);
    }

    public void validate(LetterDocument document) {
        ValidationChecks.required(document, "document");
        ValidationChecks.require(
                document.schemaVersion() == LetterDocument.SUPPORTED_SCHEMA_VERSION,
                "schemaVersion",
                "must be 1");
        PageGeometry geometry = GeometryValidator.page(document.page());
        styles.validate(document.defaultStyle(), "defaultStyle");
        sections(document, geometry.innerWidthMm());
        fixedElements(document, geometry.innerWidthMm());
        signature(document, geometry.innerWidthMm());
        watermark(document.watermark());
    }

    private void sections(LetterDocument document, double width) {
        ValidationChecks.required(document.sections(), "sections");
        boolean hasBodyBlock = false;
        for (int index = 0; index < document.sections().size(); index++) {
            String path = "sections[" + index + "]";
            Section section = document.sections().get(index);
            ValidationChecks.required(section, path);
            ValidationChecks.nonblankText(section.name(), path + ".name");
            styles.validate(section.style(), path + ".style");
            blocks.validate(
                    section.blocks(),
                    path + ".blocks",
                    document.defaultStyle(),
                    section.style(),
                    width,
                    true);
            hasBodyBlock |= !section.blocks().isEmpty();
        }
        ValidationChecks.require(hasBodyBlock, "sections", "at least one body block is required");
    }

    private void fixedElements(LetterDocument document, double width) {
        Set<String> identifiers = new HashSet<>();
        List<FixedElement> elements = document.fixedElements();
        for (int index = 0; index < elements.size(); index++) {
            String path = "fixedElements[" + index + "]";
            FixedElement element = elements.get(index);
            ValidationChecks.required(element, path);
            ValidationChecks.nonblankText(element.id(), path + ".id");
            ValidationChecks.require(identifiers.add(element.id()), path + ".id", "must be unique");
            ValidationChecks.required(element.zone(), path + ".zone");
            ValidationChecks.required(element.pages(), path + ".pages");
            double height =
                    element.zone() == Zone.TOP
                            ? document.page().topZoneMm()
                            : document.page().bottomZoneMm();
            GeometryValidator.bounds(element.boundsMm(), width, height, path + ".boundsMm");
            blocks.validate(
                    element.blocks(),
                    path + ".blocks",
                    document.defaultStyle(),
                    null,
                    element.boundsMm().width());
            ValidationChecks.require(
                    !element.blocks().isEmpty(), path + ".blocks", "must not be empty");
            rejectCollisions(element, elements.subList(0, index), path);
        }
    }

    private static void rejectCollisions(
            FixedElement element, List<FixedElement> preceding, String path) {
        for (int index = 0; index < preceding.size(); index++) {
            FixedElement other = preceding.get(index);
            if (element.zone() == other.zone() && element.pages().mayCoincideWith(other.pages())) {
                ValidationChecks.require(
                        !element.boundsMm().intersects(other.boundsMm()),
                        path + ".boundsMm",
                        "collides with fixedElements[" + index + "]");
            }
        }
    }

    private void signature(LetterDocument document, double width) {
        Signature signature = document.signature();
        if (signature == null) {
            return;
        }
        ValidationChecks.required(signature.placement(), "signature.placement");
        if (signature.placement() == SignaturePlacement.BOTTOM_LAST) {
            ValidationChecks.require(
                    document.page().signatureZoneMm() > 0,
                    "page.signatureZoneMm",
                    "required for BOTTOM_LAST");
            GeometryValidator.bounds(
                    signature.boundsMm(),
                    width,
                    document.page().signatureZoneMm(),
                    "signature.boundsMm");
            width = signature.boundsMm().width();
        } else {
            ValidationChecks.require(
                    signature.boundsMm() == null,
                    "signature.boundsMm",
                    "FLOW must not have bounds");
        }
        blocks.validate(
                signature.blocks(), "signature.blocks", document.defaultStyle(), null, width);
        ValidationChecks.require(
                !signature.blocks().isEmpty(), "signature.blocks", "must not be empty");
    }

    private void watermark(Watermark watermark) {
        if (watermark == null) {
            return;
        }
        ValidationChecks.required(watermark.pages(), "watermark.pages");
        ValidationChecks.required(watermark.opacity(), "watermark.opacity");
        ValidationChecks.finite(watermark.opacity(), "watermark.opacity");
        ValidationChecks.require(
                watermark.opacity() >= 0 && watermark.opacity() <= 1,
                "watermark.opacity",
                "must be between 0 and 1");
        ValidationChecks.required(watermark.rotationDegrees(), "watermark.rotationDegrees");
        ValidationChecks.finite(watermark.rotationDegrees(), "watermark.rotationDegrees");
        if (watermark instanceof TextWatermark text) {
            ValidationChecks.nonblankText(text.text(), "watermark.text");
            ValidationChecks.required(text.fontFamily(), "watermark.fontFamily");
            fonts.requireFamily(text.fontFamily(), "watermark.fontFamily");
            ValidationChecks.positive(text.fontSizePt(), "watermark.fontSizePt");
            StyleValidator.color(text.color(), "watermark.color");
        } else if (watermark instanceof ImageWatermark image) {
            ImageValidator.validate(
                    image.data(), image.mimeType(), image.widthMm(), image.heightMm(), "watermark");
        }
    }
}
