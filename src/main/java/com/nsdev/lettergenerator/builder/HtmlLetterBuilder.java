package com.nsdev.lettergenerator.builder;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.component.LetterComponent;
import com.nsdev.lettergenerator.diagnostics.ValidationReport;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.template.TemplateCompiler;
import com.nsdev.lettergenerator.internal.template.TemplateData;
import com.nsdev.lettergenerator.internal.template.TemplateInspector;
import com.nsdev.lettergenerator.internal.util.ArgumentChecks;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.model.DocumentSettings;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.template.PreparedLetterTemplate;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mutable compiler input for the supported HTML-like letter template language. Prefer {@link
 * com.nsdev.lettergenerator.Letter#template(String)} or {@link
 * com.nsdev.lettergenerator.Letter#fromHtml(String)} as entry points. A single {@code <letter>}
 * root defines regions and page settings; arbitrary browser HTML is not supported. Placeholder
 * values replace parsed text and attribute values without creating new markup nodes. Image
 * references use explicitly registered asset bytes.
 *
 * <p>Values replace the previous binding map; assets accumulate by name and replace an existing
 * name. Parsing, substitution, and asset resolution happen at {@link #build()}. Each build returns
 * an independent immutable document; full rendering validation happens in {@link
 * com.nsdev.lettergenerator.LetterPdfGenerator}.
 */
public final class HtmlLetterBuilder {
    /** Original template/resolved markup; parsed afresh for each document snapshot. */
    private final String source;

    /** Whether parsed text and attributes undergo scalar placeholder substitution. */
    private final boolean substitute;

    /** Owned image byte copies, indexed by the names used in markup asset references. */
    private final Map<String, byte[]> assets = new LinkedHashMap<>();

    /** Latest complete owned typed binding snapshot, empty until values are supplied. */
    private Map<String, Object> values = Map.of();

    /** Optional deterministic document settings. */
    private DocumentSettings settings;

    /** Optional role and named appearance definitions. */
    private LetterTheme theme;

    /** Document-local reusable fragments. */
    private final Map<String, LetterComponent> components = new LinkedHashMap<>();

    /**
     * Captures source markup and the selected substitution mode without parsing it yet.
     *
     * @param source non-null letter source
     * @param substitute whether build-time typed binding is enabled
     * @throws IllegalArgumentException if {@code source} is null
     */
    private HtmlLetterBuilder(String source, boolean substitute) {
        this.source = ArgumentChecks.required(source, "template markup");
        this.substitute = substitute;
    }

    /**
     * Creates a builder with {@code ${name}} substitution enabled. Missing bindings fail at build
     * time; replacement values are not recursively substituted.
     *
     * @param source non-null source containing one supported {@code <letter>} root
     * @return a fresh template builder
     * @throws IllegalArgumentException if {@code source} is null
     */
    public static HtmlLetterBuilder template(String source) {
        return new HtmlLetterBuilder(source, true);
    }

    /**
     * Creates a builder for already-resolved letter markup with substitution disabled. Calling
     * {@link #values(Map)} on this builder is an error.
     *
     * @param source non-null source containing one supported {@code <letter>} root
     * @return a fresh builder for resolved markup
     * @throws IllegalArgumentException if {@code source} is null
     */
    public static HtmlLetterBuilder resolvedHtml(String source) {
        return new HtmlLetterBuilder(source, false);
    }

    /**
     * Replaces all bindings with an owned typed snapshot. Maps with nonblank string keys, lists,
     * finite numbers, booleans, text, characters and supported immutable java.time values are
     * accepted. Null values, cycles and arbitrary objects fail with the input path.
     *
     * @param replacements non-null map of nonblank keys to non-null supported values
     * @return this builder
     * @throws IllegalArgumentException if substitution is disabled, the map is null, a key is null
     *     or blank, or a value is null or unsupported
     */
    public HtmlLetterBuilder values(Map<String, ?> replacements) {
        ArgumentChecks.require(
                substitute,
                "Use Letter.template(...) when supplying placeholder values; fromHtml expects already-resolved markup");
        values = TemplateData.snapshot(replacements);
        return this;
    }

    /**
     * Sets deterministic document locale while preserving other explicit settings.
     *
     * @param value non-null formatting settings
     * @return this builder
     */
    public HtmlLetterBuilder locale(LocaleSettings value) {
        ArgumentChecks.required(value, "locale");
        settings =
                settings == null
                        ? new DocumentSettings(value, null, null, null, null)
                        : new DocumentSettings(
                                value,
                                settings.metadata(),
                                settings.direction(),
                                settings.fallbackFonts(),
                                settings.hyphenate());
        return this;
    }

    /**
     * Sets immutable language, metadata and text configuration.
     *
     * @param value non-null document settings
     * @return this builder
     */
    public HtmlLetterBuilder settings(DocumentSettings value) {
        settings = ArgumentChecks.required(value, "settings");
        return this;
    }

    /**
     * Captures parsed source and owned configuration for concurrent repeated binding.
     *
     * @return immutable prepared template
     */
    public PreparedLetterTemplate compile() {
        return new PreparedLetterTemplate(
                source, values, assets, substitute, theme, components, settings);
    }

    /**
     * Registers a defensive copy of named image bytes for later HTML asset resolution. Reusing a
     * name replaces its previous bytes. Image decoding and format checks occur when markup
     * referencing the asset is built, so registration can follow content calls.
     *
     * @param name nonblank asset identifier used by markup image references
     * @param data non-null image bytes; supported image formats are PNG, JPEG and static SVG
     * @return this builder
     * @throws IllegalArgumentException if the name is null or blank, or the bytes are null
     */
    public HtmlLetterBuilder asset(String name, byte[] data) {
        ArgumentChecks.require(name != null && !name.isBlank(), "Asset name must not be blank");
        assets.put(name, ArgumentChecks.required(data, "asset bytes").clone());
        return this;
    }

    /**
     * Reads a local file immediately and registers its bytes as a named markup image asset. Reusing
     * a name replaces the existing asset; image decoding is deferred until referenced markup is
     * built.
     *
     * @param name nonblank asset identifier
     * @param path non-null image file to read
     * @return this builder
     * @throws IOException if the file cannot be read
     * @throws NullPointerException if {@code path} is null
     * @throws IllegalArgumentException if {@code name} is null or blank
     */
    public HtmlLetterBuilder asset(String name, Path path) throws IOException {
        return asset(name, Files.readAllBytes(path));
    }

    /**
     * Supplies per-document role defaults and named styles.
     *
     * @param value non-null immutable theme
     * @return this builder
     */
    public HtmlLetterBuilder theme(LetterTheme value) {
        theme = ArgumentChecks.required(value, "theme");
        return this;
    }

    /**
     * Registers a fragment referenced by an empty component tag with a ref attribute.
     *
     * @param name nonblank component name
     * @param value immutable content
     * @return this builder
     */
    public HtmlLetterBuilder component(String name, LetterComponent value) {
        ArgumentChecks.require(name != null && !name.isBlank(), "Component name must not be blank");
        components.put(name, ArgumentChecks.required(value, "component"));
        return this;
    }

    /**
     * Inspects bindings, assets and declared model constraints using built-in fonts, without PDF
     * layout.
     *
     * @return immutable report aggregating missing variable occurrences and invalid assets
     */
    public ValidationReport validate() {
        return validate(new LetterPdfGenerator());
    }

    /**
     * Inspects with the selected generator's registered fonts. Success does not guarantee content
     * fit.
     *
     * @param generator non-null font-aware validator
     * @return immutable authoring report
     */
    public ValidationReport validate(LetterPdfGenerator generator) {
        ArgumentChecks.required(generator, "generator");
        return TemplateInspector.inspect(
                source, values, assets, substitute, settings, generator, this::build);
    }

    /**
     * Compiles the current source, bindings and assets into an immutable document snapshot.
     * Template parsing, supported-tag and style checks, placeholder binding, image decoding and
     * region arrangement occur here. Rendering validation, including font availability and layout
     * constraints, is performed later by {@link com.nsdev.lettergenerator.LetterPdfGenerator}. The
     * builder remains reusable after a build.
     *
     * @return a new immutable letter document
     * @throws InvalidLetterException if markup, placeholders, referenced assets or constructed
     *     letter regions are invalid, or no body content is produced
     */
    public LetterDocument build() {
        try {
            return TemplateCompiler.compile(
                            TemplateCompiler.prepare(source),
                            values,
                            assets,
                            substitute,
                            theme,
                            settings)
                    .withTheme(theme)
                    .withComponents(components);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new InvalidLetterException("template", exception.getMessage(), exception);
        }
    }
}
