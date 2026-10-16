package com.nsdev.lettergenerator.model.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nsdev.lettergenerator.model.style.Style;

/** Closed set of supported body or fixed-region blocks. JSON uses the type discriminator. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Heading.class, name = "heading"),
    @JsonSubTypes.Type(value = Paragraph.class, name = "paragraph"),
    @JsonSubTypes.Type(value = ListBlock.class, name = "list"),
    @JsonSubTypes.Type(value = ImageBlock.class, name = "image"),
    @JsonSubTypes.Type(value = TableBlock.class, name = "table"),
    @JsonSubTypes.Type(value = PageBreakBlock.class, name = "pageBreak")
})
public sealed interface Block
        permits Heading, Paragraph, ListBlock, ImageBlock, TableBlock, PageBreakBlock {
    Style style();
}
