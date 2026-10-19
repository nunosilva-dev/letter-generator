package com.nsdev.lettergenerator.internal.font;

import com.nsdev.lettergenerator.diagnostics.Diagnostic;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Signature;
import java.util.ArrayList;
import java.util.List;

/** Resolves explicitly configured fallback runs before semantic validation and native rendering. */
public final class FontFallbackPreparation {
    /**
     * Prepared immutable content and observable font substitutions.
     *
     * @param document nullable input or resolved document retaining all other settings/resources
     * @param diagnostics immutable ordered substitution diagnostics
     */
    public record Preparation(LetterDocument document, List<Diagnostic> diagnostics) {
        /**
         * Owns the diagnostic sequence without retaining mutable per-call state.
         *
         * @param document nullable immutable content
         * @param diagnostics ordered substitutions
         */
        public Preparation {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    /** Prevents construction of this stateless entry point. */
    private FontFallbackPreparation() {}

    /**
     * Uses configured exact font faces to cover complete Unicode grapheme clusters. Documents
     * without explicit fallback families retain the existing engine validation behavior unchanged.
     *
     * @param document composition-prepared nullable document
     * @param fonts owned registered-font snapshot
     * @return replacement immutable content and per-call substitution diagnostics
     */
    public static Preparation prepare(LetterDocument document, FontRegistry fonts) {
        if (document == null || document.settings().fallbackFonts().isEmpty()) {
            return new Preparation(document, List.of());
        }
        List<Diagnostic> diagnostics = new ArrayList<>();
        FallbackBlocks blocks =
                new FallbackBlocks(
                        document.defaultStyle(),
                        new FallbackRuns(fonts, document.settings().fallbackFonts(), diagnostics));
        List<Section> sections =
                FallbackBlocks.map(
                        document.sections(),
                        "sections",
                        (section, path) ->
                                section == null
                                        ? null
                                        : new Section(
                                                section.name(),
                                                section.style(),
                                                blocks.prepare(
                                                        section.blocks(),
                                                        section.style(),
                                                        null,
                                                        path + ".blocks")));
        List<FixedElement> fixed =
                FallbackBlocks.map(
                        document.fixedElements(),
                        "fixedElements",
                        (element, path) ->
                                element == null
                                        ? null
                                        : new FixedElement(
                                                element.id(),
                                                element.zone(),
                                                element.pages(),
                                                element.boundsMm(),
                                                blocks.prepare(
                                                        element.blocks(),
                                                        null,
                                                        null,
                                                        path + ".blocks")));
        Signature signature = document.signature();
        if (signature != null) {
            signature =
                    new Signature(
                            signature.placement(),
                            blocks.prepare(signature.blocks(), null, null, "signature.blocks"),
                            signature.boundsMm());
        }
        LetterDocument resolved =
                new LetterDocument(
                        document.schemaVersion(),
                        document.page(),
                        document.defaultStyle(),
                        sections,
                        fixed,
                        signature,
                        document.watermark(),
                        document.theme(),
                        document.components(),
                        document.settings(),
                        document.attachments());
        return new Preparation(resolved, diagnostics);
    }
}
