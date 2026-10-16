package com.nsdev.lettergenerator.model.watermark;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nsdev.lettergenerator.model.layout.PageSelector;

/** Optional decoration drawn behind selected pages. JSON uses the kind discriminator. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextWatermark.class, name = "text"),
    @JsonSubTypes.Type(value = ImageWatermark.class, name = "image")
})
public sealed interface Watermark permits TextWatermark, ImageWatermark {
    PageSelector pages();

    Double opacity();

    Double rotationDegrees();
}
