package com.nsdev.lettergenerator.builder;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.builder.BuilderChecks;
import com.nsdev.lettergenerator.internal.template.TemplateCompiler;
import com.nsdev.lettergenerator.model.LetterDocument;
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

    /** Latest complete scalar binding snapshot, empty until values are supplied. */
    private Map<String, String> values = Map.of();

    /**
     * Captures source markup and the selected substitution mode without parsing it yet.
     *
     * @param source non-null letter source
     * @param substitute whether build-time scalar binding is enabled
     * @throws IllegalArgumentException if {@code source} is null
     */
    private HtmlLetterBuilder(String source, boolean substitute) {
        this.source = BuilderChecks.required(source, "template markup");
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
     * Replaces all placeholder bindings with an immediate string snapshot of the supplied map.
     * Accepted values are character sequences, numbers, booleans and characters; each is converted
     * with {@code toString()}. Mutating the original map or values later has no effect.
     *
     * @param replacements non-null map of nonblank keys to non-null scalar values
     * @return this builder
     * @throws IllegalArgumentException if substitution is disabled, the map is null, a key is null
     *     or blank, or a value is null or not scalar
     */
    public HtmlLetterBuilder values(Map<String, ?> replacements) {
        BuilderChecks.require(
                substitute,
                "Use Letter.template(...) when supplying placeholder values; fromHtml expects already-resolved markup");
        BuilderChecks.required(replacements, "template values");
        var snapshot = new LinkedHashMap<String, String>();
        replacements.forEach(
                (key, value) -> {
                    BuilderChecks.require(
                            key != null && !key.isBlank(), "Variable key must not be blank");
                    BuilderChecks.required(value, "value for " + key);
                    BuilderChecks.require(
                            value instanceof CharSequence
                                    || value instanceof Number
                                    || value instanceof Boolean
                                    || value instanceof Character,
                            "Template values must be scalar: " + key);
                    snapshot.put(key, value.toString());
                });
        values = Map.copyOf(snapshot);
        return this;
    }

    /**
     * Registers a defensive copy of named image bytes for later HTML asset resolution. Reusing a
     * name replaces its previous bytes. Image decoding and format checks occur when markup
     * referencing the asset is built, so registration can follow content calls.
     *
     * @param name nonblank asset identifier used by markup image references
     * @param data non-null image bytes; supported image formats are PNG and JPEG
     * @return this builder
     * @throws IllegalArgumentException if the name is null or blank, or the bytes are null
     */
    public HtmlLetterBuilder asset(String name, byte[] data) {
        BuilderChecks.require(name != null && !name.isBlank(), "Asset name must not be blank");
        assets.put(name, BuilderChecks.required(data, "asset bytes").clone());
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
            return TemplateCompiler.compile(source, values, assets, substitute);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new InvalidLetterException("template", exception.getMessage(), exception);
        }
    }
}
