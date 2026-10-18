package com.nsdev.lettergenerator.model;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import java.util.List;

/**
 * Complete immutable input to PDF generation, shared by the Java builders and JSON input. A
 * document contains body {@link Section sections}, optional positioned content, a visual signature,
 * and a watermark. The schema version is checked during generation; constructing this record does
 * not validate content, fonts, geometry, or image formats.
 *
 * <p>Supplied lists are copied into unmodifiable lists, preserving null entries for later
 * validation. A null sections list remains null; only a null fixed-elements list becomes empty.
 *
 * @param schemaVersion input schema version; generation currently requires {@link
 *     #SUPPORTED_SCHEMA_VERSION}
 * @param page physical layout, or null for {@link PageSpec#defaults()}
 * @param defaultStyle nullable overrides applied before section, block, cell, and run styles
 * @param sections body sections in reading order; generation requires at least one body block
 * @param fixedElements positioned top/bottom content; null means no fixed elements
 * @param signature optional visual signature; null means no signature
 * @param watermark optional decoration behind page content; null means no watermark
 */
public record LetterDocument(
        int schemaVersion,
        PageSpec page,
        Style defaultStyle,
        List<Section> sections,
        List<FixedElement> fixedElements,
        Signature signature,
        Watermark watermark) {
    /** Schema version accepted by generation and by the JSON document model. */
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    /**
     * Substitutes the default page and snapshots both lists without validating their contents.
     *
     * @param schemaVersion schema identifier stored unchanged
     * @param page page specification, or null to use the default
     * @param defaultStyle optional document-wide style overrides
     * @param sections ordered body sections, copied if nonnull
     * @param fixedElements positioned elements, copied or replaced with an empty list
     * @param signature optional visual signature
     * @param watermark optional page decoration
     */
    public LetterDocument {
        if (page == null) {
            page = PageSpec.defaults();
        }
        sections = ImmutableLists.copyNullable(sections);
        fixedElements =
                fixedElements == null ? List.of() : ImmutableLists.copyNullable(fixedElements);
    }
}
