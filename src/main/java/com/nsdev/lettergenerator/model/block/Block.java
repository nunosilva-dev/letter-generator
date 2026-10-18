package com.nsdev.lettergenerator.model.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nsdev.lettergenerator.model.style.Style;

/**
 * Closed set of letter content blocks usable in body sections and positioned regions. JSON selects
 * the concrete record through the {@code type} property: {@code heading}, {@code paragraph}, {@code
 * list}, {@code image}, {@code table}, or {@code pageBreak}. A {@link PageBreakBlock} is valid only
 * in body sections; other block kinds can also appear in fixed elements and visual signatures.
 *
 * <p>Block records snapshot supplied collections or image bytes but generally defer content, style,
 * and geometry validation until PDF generation.
 */
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
    /**
     * Returns overrides layered on the inherited document and optional section style. Individual
     * text runs and table cells can override the resulting style further.
     *
     * @return nullable block style; null means inherit all properties
     */
    Style style();
}
