package com.nsdev.lettergenerator;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.font.FontSource;
import com.nsdev.lettergenerator.internal.json.JsonInput;
import com.nsdev.lettergenerator.internal.render.DocumentRenderer;
import com.nsdev.lettergenerator.internal.validation.InputValidator;
import com.nsdev.lettergenerator.model.LetterDocument;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates complete PDF letters from immutable Java models or JSON. Instances are safe to share
 * between concurrent callers; each rendering uses its own engine state.
 */
public final class LetterPdfGenerator {
    /** Per-generator validator sharing the same immutable font snapshot as the renderer. */
    private final InputValidator validator;

    /** Reusable renderer that creates fresh rendering engine state for each call. */
    private final DocumentRenderer renderer;

    /** Uses the PDF engine's built-in fonts; no font files or registration are required. */
    public LetterPdfGenerator() {
        this(FontRegistry.empty());
    }

    /**
     * Binds validation and rendering to one immutable loaded font registry.
     *
     * @param fonts immutable trusted font snapshot
     */
    private LetterPdfGenerator(FontRegistry fonts) {
        validator = new InputValidator(fonts);
        renderer = new DocumentRenderer(fonts);
    }

    /**
     * Starts mutable generator configuration for application-supplied TrueType fonts. Built-in
     * Helvetica, Times and Courier remain available without registration.
     *
     * @return a new configuration builder; fonts are loaded by its {@link Builder#build()} method
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses strict schema-version-1 JSON, validates the resulting model, and renders a PDF.
     * Unknown fields, duplicate keys, trailing tokens and incompatible scalar types are rejected.
     * No partial result is returned if any processing step fails.
     *
     * @param json non-null JSON document in the library's letter schema
     * @return complete PDF file bytes, including any requested watermark processing
     * @throws InvalidLetterException if the input is null, malformed, or fails document validation;
     *     the exception identifies the affected field path
     * @throws PdfGenerationException if composition, layout, or watermark processing fails
     */
    public byte[] generateJson(String json) {
        return generate(JsonInput.read(json));
    }

    /**
     * Validates an immutable document and renders it using this generator's font snapshot. All
     * requested processing must succeed before PDF bytes are returned. This method can be called
     * concurrently on the same generator.
     *
     * @param document non-null document to validate and render
     * @return complete PDF file bytes
     * @throws InvalidLetterException if the document is null or invalid, with its field path
     * @throws PdfGenerationException if composition, layout, or watermark processing fails
     */
    public byte[] generate(LetterDocument document) {
        validator.validate(document);
        return renderer.render(document);
    }

    /**
     * Mutable configuration that accumulates trusted Java font sources in registration order.
     * Registration defers font reads to {@link #build()}; each build loads a separate immutable
     * font snapshot. Reusing or changing this builder does not alter an existing generator. Each
     * custom family requires a regular face and any bold/italic faces used by its content.
     */
    public static final class Builder {
        /**
         * Ordered deferred font registrations; each build independently reads and validates them.
         */
        private final List<FontSource> fonts = new ArrayList<>();

        /** Creates an empty mutable set of deferred font registrations. */
        private Builder() {}

        /**
         * Appends a local TrueType font source without reading the file yet. Family names and
         * variants are checked at build time; duplicate family/variant pairs are errors rather than
         * replacements. Built-in family names cannot be overridden.
         *
         * @param family nonblank, case-sensitive family identifier used by document styles
         * @param variant non-null face represented by the file
         * @param path non-null trusted local font file, read when {@link #build()} is called
         * @return this builder
         * @throws InvalidLetterException if {@code path} is null
         */
        public Builder registerFont(String family, FontVariant variant, Path path) {
            fonts.add(FontSource.file(family, variant, path));
            return this;
        }

        /**
         * Appends a TrueType classpath source without resolving or reading it yet. At build time
         * the thread context class loader is used, falling back to the library loader if absent. A
         * leading slash is ignored. Duplicate family/variant pairs are rejected then.
         *
         * @param family nonblank, case-sensitive family identifier used by document styles
         * @param variant non-null face represented by the resource
         * @param resourcePath nonblank classpath resource name, resolved by {@link #build()}
         * @return this builder
         */
        public Builder registerClasspathFont(
                String family, FontVariant variant, String resourcePath) {
            fonts.add(FontSource.classpath(family, variant, resourcePath));
            return this;
        }

        /**
         * Reads and validates every registered font and creates an independent reusable generator.
         * Only TrueType files with TrueType outlines are accepted. Every custom family must include
         * {@link FontVariant#REGULAR}; other faces must be registered if document styles use them.
         * Later file changes or builder registrations do not alter the returned generator.
         *
         * @return a generator owning the loaded font snapshot
         * @throws InvalidLetterException if a source cannot be read, is not a supported font, has a
         *     blank family or null variant, duplicates a face, overrides a built-in family, or
         *     belongs to a family without a regular face
         */
        public LetterPdfGenerator build() {
            return new LetterPdfGenerator(FontRegistry.load(fonts));
        }
    }
}
