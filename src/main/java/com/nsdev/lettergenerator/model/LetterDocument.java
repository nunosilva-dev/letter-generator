package com.nsdev.lettergenerator.model;

import com.nsdev.lettergenerator.component.LetterComponent;
import com.nsdev.lettergenerator.document.PdfAttachment;
import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete immutable input to PDF generation, shared by the Java builders and JSON input. A
 * document contains body {@link Section sections}, optional positioned content, a visual signature,
 * and a watermark. The schema version is checked during generation; constructing this record does
 * not validate content, fonts, geometry, or image formats.
 *
 * <p>Supplied lists and the component map are copied into unmodifiable collections, preserving null
 * entries for later validation. A null sections list remains null; only a null fixed-elements list
 * becomes empty. Themes and reusable components resolve before ordinary validation, without global
 * registrations.
 *
 * @param schemaVersion input schema version; generation currently requires {@link
 *     #SUPPORTED_SCHEMA_VERSION}
 * @param page physical layout, or null for {@link PageSpec#defaults()}
 * @param defaultStyle nullable overrides applied before section, block, cell, and run styles
 * @param sections body sections in reading order; generation requires at least one body block
 * @param fixedElements positioned top/bottom content; null means no fixed elements
 * @param signature optional visual signature; null means no signature
 * @param watermark optional decoration behind page content; null means no watermark
 * @param theme optional document-local role and named style defaults
 * @param components optional document-local reusable content; null becomes an empty map
 * @param settings optional formatting and output metadata; null selects stable defaults
 * @param attachments optional PDFs appended unchanged after the generated letter
 */
public record LetterDocument(
        int schemaVersion,
        PageSpec page,
        Style defaultStyle,
        List<Section> sections,
        List<FixedElement> fixedElements,
        Signature signature,
        Watermark watermark,
        LetterTheme theme,
        Map<String, LetterComponent> components,
        DocumentSettings settings,
        List<PdfAttachment> attachments) {
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
     * @param theme nullable role and named-style definitions
     * @param components nullable reusable-content registrations, defensively copied
     * @param settings optional document settings
     * @param attachments optional appended PDF snapshots
     */
    public LetterDocument {
        settings = settings == null ? DocumentSettings.defaults() : settings;
        attachments = attachments == null ? List.of() : ImmutableLists.copyNullable(attachments);
        if (page == null) {
            page = PageSpec.defaults();
        }
        sections = ImmutableLists.copyNullable(sections);
        fixedElements =
                fixedElements == null ? List.of() : ImmutableLists.copyNullable(fixedElements);
        components =
                components == null
                        ? Map.of()
                        : Collections.unmodifiableMap(new LinkedHashMap<>(components));
    }

    /**
     * Preserves composition-aware construction without additional settings or attachments.
     *
     * @param schemaVersion input schema
     * @param page physical layout
     * @param defaultStyle document style
     * @param sections body sections
     * @param fixedElements fixed content
     * @param signature optional signature
     * @param watermark optional decoration
     * @param theme optional theme
     * @param components optional reusable registrations
     */
    public LetterDocument(
            int schemaVersion,
            PageSpec page,
            Style defaultStyle,
            List<Section> sections,
            List<FixedElement> fixedElements,
            Signature signature,
            Watermark watermark,
            LetterTheme theme,
            Map<String, LetterComponent> components) {
        this(
                schemaVersion,
                page,
                defaultStyle,
                sections,
                fixedElements,
                signature,
                watermark,
                theme,
                components,
                null,
                null);
    }

    /**
     * Preserves the original constructor with no theme or reusable-content registrations.
     *
     * @param schemaVersion input schema version
     * @param page nullable page specification
     * @param defaultStyle nullable document overrides
     * @param sections ordered body sections
     * @param fixedElements nullable positioned content
     * @param signature nullable visual signature
     * @param watermark nullable decoration
     */
    public LetterDocument(
            int schemaVersion,
            PageSpec page,
            Style defaultStyle,
            List<Section> sections,
            List<FixedElement> fixedElements,
            Signature signature,
            Watermark watermark) {
        this(
                schemaVersion,
                page,
                defaultStyle,
                sections,
                fixedElements,
                signature,
                watermark,
                null,
                null);
    }

    /**
     * Replaces the theme while retaining content and component registrations.
     *
     * @param value nullable theme
     * @return immutable document copy
     */
    public LetterDocument withTheme(LetterTheme value) {
        return new LetterDocument(
                schemaVersion,
                page,
                defaultStyle,
                sections,
                fixedElements,
                signature,
                watermark,
                value,
                components,
                settings,
                attachments);
    }

    /**
     * Replaces component registrations with a defensive snapshot.
     *
     * @param value nullable document-local component registrations
     * @return immutable document copy
     */
    public LetterDocument withComponents(Map<String, LetterComponent> value) {
        return new LetterDocument(
                schemaVersion,
                page,
                defaultStyle,
                sections,
                fixedElements,
                signature,
                watermark,
                theme,
                value,
                settings,
                attachments);
    }

    /**
     * Replaces document settings while preserving all content and attachments.
     *
     * @param value formatting/output metadata
     * @return independent immutable model
     */
    public LetterDocument withSettings(DocumentSettings value) {
        return new LetterDocument(
                schemaVersion,
                page,
                defaultStyle,
                sections,
                fixedElements,
                signature,
                watermark,
                theme,
                components,
                value,
                attachments);
    }

    /**
     * Replaces appended PDF registrations while retaining the letter.
     *
     * @param value appended PDFs, snapshotted
     * @return immutable model
     */
    public LetterDocument withAttachments(List<PdfAttachment> value) {
        return new LetterDocument(
                schemaVersion,
                page,
                defaultStyle,
                sections,
                fixedElements,
                signature,
                watermark,
                theme,
                components,
                settings,
                value);
    }
}
