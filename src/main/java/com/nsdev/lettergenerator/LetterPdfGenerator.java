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
    private final InputValidator validator;
    private final DocumentRenderer renderer;

    /** Uses the PDF engine's built-in fonts; no font files or registration are required. */
    public LetterPdfGenerator() {
        this(FontRegistry.empty());
    }

    private LetterPdfGenerator(FontRegistry fonts) {
        validator = new InputValidator(fonts);
        renderer = new DocumentRenderer(fonts);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses strict schema-version-1 JSON and generates a complete PDF.
     *
     * @throws InvalidLetterException if the JSON or document is invalid, with its field path
     * @throws PdfGenerationException if composition, layout, or watermark processing fails
     */
    public byte[] generateJson(String json) {
        return generate(JsonInput.read(json));
    }

    /**
     * Validates the document and returns PDF bytes only after all requested processing succeeds.
     *
     * @throws InvalidLetterException if the document is invalid, with its field path
     * @throws PdfGenerationException if composition, layout, or watermark processing fails
     */
    public byte[] generate(LetterDocument document) {
        validator.validate(document);
        return renderer.render(document);
    }

    /** Mutable Java configuration. Build once; reuse the immutable generator. */
    public static final class Builder {
        private final List<FontSource> fonts = new ArrayList<>();

        private Builder() {}

        /** Registers a trusted file; the font is loaded and validated by {@link #build()}. */
        public Builder registerFont(String family, FontVariant variant, Path path) {
            fonts.add(FontSource.file(family, variant, path));
            return this;
        }

        /** Registers a trusted classpath resource, resolved by {@link #build()}. */
        public Builder registerClasspathFont(
                String family, FontVariant variant, String resourcePath) {
            fonts.add(FontSource.classpath(family, variant, resourcePath));
            return this;
        }

        /** Loads all font resources and creates an immutable, reusable generator. */
        public LetterPdfGenerator build() {
            return new LetterPdfGenerator(FontRegistry.load(fonts));
        }
    }
}
