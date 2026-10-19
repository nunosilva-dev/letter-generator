package com.nsdev.lettergenerator;

import com.nsdev.lettergenerator.diagnostics.Diagnostic;
import com.nsdev.lettergenerator.diagnostics.DiagnosticPhase;
import com.nsdev.lettergenerator.diagnostics.DiagnosticSeverity;
import com.nsdev.lettergenerator.diagnostics.ValidationReport;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.internal.composition.DocumentPreparation;
import com.nsdev.lettergenerator.internal.font.FontFallbackPreparation;
import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.font.FontSource;
import com.nsdev.lettergenerator.internal.hyphenation.HyphenationPatterns;
import com.nsdev.lettergenerator.internal.json.JsonInput;
import com.nsdev.lettergenerator.internal.render.DocumentRenderer;
import com.nsdev.lettergenerator.internal.validation.InputValidator;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.render.RenderOptions;
import com.nsdev.lettergenerator.render.RenderResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates complete PDF letters from immutable Java models or JSON. Instances are safe to share
 * between concurrent callers; each rendering uses its own engine state.
 */
public final class LetterPdfGenerator {
    /** Per-generator validator sharing the same immutable font snapshot as the renderer. */
    private final InputValidator validator;

    /** Reusable renderer that creates fresh rendering engine state for each call. */
    private final DocumentRenderer renderer;

    /** Font snapshot used when resolving explicitly configured fallback runs. */
    private final FontRegistry fonts;

    /** Owned application-supplied hyphenation dictionaries. */
    private final HyphenationPatterns patterns;

    /** Uses the PDF engine's built-in fonts; no font files or registration are required. */
    public LetterPdfGenerator() {
        this(FontRegistry.empty(), HyphenationPatterns.empty());
    }

    /**
     * Binds validation and rendering to one immutable loaded font registry.
     *
     * @param fonts immutable trusted font snapshot
     * @param patterns immutable trusted pattern snapshot
     */
    private LetterPdfGenerator(FontRegistry fonts, HyphenationPatterns patterns) {
        this.fonts = fonts;
        this.patterns = patterns;
        validator = new InputValidator(fonts);
        renderer = new DocumentRenderer(fonts, patterns);
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
     * Renders strict JSON and returns actual pagination and diagnostic information.
     *
     * @param json nonnull shared-model JSON
     * @return accepted PDF and report using the standard output profile
     */
    public RenderResult renderJson(String json) {
        return renderJson(json, RenderOptions.defaults());
    }

    /**
     * Renders strict JSON with the same output choices available to Java and HTML models.
     *
     * @param json nonnull shared-model JSON
     * @param options per-call output choices
     * @return complete accepted PDF and rendering report
     * @throws InvalidLetterException for invalid input or incompatible output choices
     * @throws PdfGenerationException for rejected native layout or PDF processing
     */
    public RenderResult renderJson(String json, RenderOptions options) {
        return render(JsonInput.read(json), options);
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
        return render(document).pdfBytes();
    }

    /**
     * Validates composition references, declared geometry, fonts and content without running PDF
     * layout. Reports the first semantic model error; template inspection separately aggregates
     * missing variables and asset errors. Success does not guarantee that content fits.
     *
     * @param document nullable model to inspect
     * @return immutable input report
     */
    public ValidationReport validate(LetterDocument document) {
        try {
            LetterDocument prepared =
                    FontFallbackPreparation.prepare(DocumentPreparation.prepare(document), fonts)
                            .document();
            patterns.validate(prepared);
            validator.validate(prepared);
            return new ValidationReport(List.of());
        } catch (InvalidLetterException exception) {
            return new ValidationReport(
                    List.of(
                            new Diagnostic(
                                    "INVALID_INPUT",
                                    DiagnosticSeverity.ERROR,
                                    DiagnosticPhase.INPUT,
                                    exception.fieldPath(),
                                    exception.getMessage())));
        }
    }

    /**
     * Renders a validated model and returns its actual pagination and processing diagnostics.
     *
     * @param document non-null input
     * @return accepted PDF and rendering report
     * @throws InvalidLetterException for invalid input
     * @throws PdfGenerationException for rejected layout or PDF processing
     */
    public RenderResult render(LetterDocument document) {
        return render(document, RenderOptions.defaults());
    }

    /**
     * Renders with optional authoring overlays. Invalid input and rejected layouts still throw; no
     * partial PDF is returned. Debug geometry describes allocated regions, not glyph bounds.
     *
     * @param document non-null input
     * @param options non-null rendering options
     * @return accepted PDF with actual page count and optional declared geometry
     * @throws InvalidLetterException for invalid input
     * @throws IllegalArgumentException if options is null
     * @throws PdfGenerationException for rejected layout or PDF processing
     */
    public RenderResult render(LetterDocument document, RenderOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Render options are required");
        }
        LetterDocument prepared = DocumentPreparation.prepare(document);
        var resolved = FontFallbackPreparation.prepare(prepared, fonts);
        patterns.validate(resolved.document());
        validator.validate(resolved.document());
        RenderResult result = renderer.render(resolved.document(), options);
        if (resolved.diagnostics().isEmpty()) {
            return result;
        }
        List<Diagnostic> diagnostics = new ArrayList<>(resolved.diagnostics());
        diagnostics.addAll(result.diagnostics());
        return new RenderResult(
                result.pdfBytes(),
                result.pageCount(),
                diagnostics,
                result.debugBoxes(),
                result.parts());
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

        /** Ordered trusted pattern registrations, snapshotted independently by every build. */
        private final Map<String, byte[]> patterns = new LinkedHashMap<>();

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
         * Registers trusted TeX-style FOP hyphenation XML for one language or language-country. XML
         * declarations of external entities or DTDs are rejected. No dictionary lookup or download
         * is performed; document hyphenation requires its exact registered tag.
         *
         * @param languageTag BCP 47 language or language-country tag
         * @param trustedXml pattern XML, copied immediately and validated by build
         * @return this builder
         * @throws InvalidLetterException for duplicate tags or absent bytes
         */
        public Builder hyphenationPatterns(String languageTag, byte[] trustedXml) {
            if (trustedXml == null || patterns.containsKey(languageTag)) {
                throw new InvalidLetterException(
                        "hyphenation", "Pattern bytes and unique tag required");
            }
            patterns.put(languageTag, trustedXml.clone());
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
            return new LetterPdfGenerator(
                    FontRegistry.load(fonts), new HyphenationPatterns(patterns));
        }
    }
}
