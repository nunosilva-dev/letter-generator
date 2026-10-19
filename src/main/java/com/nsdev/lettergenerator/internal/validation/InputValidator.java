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

/**
 * Pre-render validation gateway for every input API.
 *
 * <p>{@link com.nsdev.lettergenerator.LetterPdfGenerator} calls this before any FO is composed. The
 * validator combines page geometry, style/font availability, body blocks, fixed-box collisions,
 * signatures and watermark checks. Errors retain a model field path for both Java and JSON inputs.
 * Painted glyph bounds and actual pagination are checked later by the rendering boundary.
 */
public final class InputValidator {
    /** Generator font snapshot used for watermark family checks and shared content validators. */
    private final FontRegistry fonts;

    /** Shared checker for unresolved nullable style properties. */
    private final StyleValidator styles;

    /** Content walker for body, fixed-element, and signature block lists. */
    private final BlockValidator blocks;

    /**
     * Creates reusable validators bound to one generator's immutable font registry.
     *
     * @param fonts built-in families plus the generator's explicitly registered font variants
     */
    public InputValidator(FontRegistry fonts) {
        this.fonts = fonts;
        this.styles = new StyleValidator(fonts);
        this.blocks = new BlockValidator(fonts, styles);
    }

    /**
     * Checks model structure and declared dimensions before rendering allocates engine state.
     *
     * @param document candidate model, including defaults already supplied by its records
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if a field, resource or
     *     geometry constraint is invalid; the exception identifies its document field path
     */
    public void validate(LetterDocument document) {
        ValidationChecks.required(document, "document");
        ValidationChecks.require(
                document.schemaVersion() == LetterDocument.SUPPORTED_SCHEMA_VERSION,
                "schemaVersion",
                "must be 1");
        for (int index = 0; index < document.attachments().size(); index++) {
            ValidationChecks.required(
                    document.attachments().get(index), "attachments[" + index + "]");
        }
        PageGeometry geometry = GeometryValidator.page(document.page());
        styles.validate(document.defaultStyle(), "defaultStyle");
        sections(document, geometry.innerWidthMm());
        fixedElements(document, geometry.innerWidthMm());
        signature(document, geometry.innerWidthMm());
        watermark(document.watermark());
        CanvasHeights.validate(document, geometry);
    }

    /**
     * Validates section names, inherited styles and ordered body blocks, requiring at least one
     * body block. Body regions alone permit explicit page breaks.
     *
     * @param document model whose page geometry has already passed validation
     * @param width usable page width between the side margins, in millimeters
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if section names, body
     *     content, or styles are invalid, or no body block exists
     */
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

    /**
     * Checks each fixed rectangle against its reserved zone and rejects overlapping boxes whose
     * page selectors can coincide.
     *
     * @param document model whose page geometry has already passed validation
     * @param width usable page width between the side margins, in millimeters
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if identifiers, placement,
     *     content, or collisions are invalid
     */
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

    /**
     * Compares a fixed element with earlier validated elements in the same zone, so collision
     * errors identify the later conflicting box.
     *
     * @param element current element, with validated zone, selection, and bounds
     * @param preceding earlier validated elements from this document
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if overlapping boxes share
     *     a zone and can occur on the same page
     */
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

    /**
     * Validates FLOW content at body width or BOTTOM_LAST content within its positive reserved
     * signature zone; page breaks are forbidden in either signature.
     *
     * @param document model whose page geometry has already passed validation
     * @param width usable page width between the side margins, in millimeters
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if signature placement,
     *     zone, bounds, or content is invalid
     */
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

    /**
     * Checks optional watermark selectors, opacity, rotation and declared text/image resources
     * before PDF post-processing.
     *
     * @param watermark decoration to check, or null for no decoration
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if selector, paint
     *     settings, text/font/color, or image data is invalid
     */
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
