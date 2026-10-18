package com.nsdev.lettergenerator.model.watermark;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nsdev.lettergenerator.model.layout.PageSelector;

/**
 * Optional decoration centered on the physical page and drawn behind letter content. Watermarks do
 * not reserve space or alter pagination. JSON chooses the concrete record through {@code kind},
 * using {@code text} or {@code image}. The document may omit a watermark entirely.
 *
 * <p>Generation validates selection, opacity, rotation, and subtype-specific content. Nullable
 * numeric components in watermark records are not inherited style overrides and are not defaulted
 * by their canonical constructors; use subtype factories for defaults.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextWatermark.class, name = "text"),
    @JsonSubTypes.Type(value = ImageWatermark.class, name = "image")
})
public sealed interface Watermark permits TextWatermark, ImageWatermark {
    /**
     * Returns the pages on which to paint the decoration.
     *
     * @return selection required during generation
     */
    PageSelector pages();

    /**
     * Returns paint opacity, where zero is invisible and one is fully opaque.
     *
     * @return finite value from zero through one, required during generation
     */
    Double opacity();

    /**
     * Returns rotation about the physical page center. Positive values rotate counterclockwise in
     * PDF coordinates; zero leaves the decoration horizontal.
     *
     * @return finite rotation in degrees, required during generation
     */
    Double rotationDegrees();
}
