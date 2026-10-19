package com.nsdev.lettergenerator.internal.render;

import com.nsdev.lettergenerator.diagnostics.Diagnostic;
import com.nsdev.lettergenerator.diagnostics.DiagnosticPhase;
import com.nsdev.lettergenerator.diagnostics.DiagnosticSeverity;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.hyphenation.HyphenationPatterns;
import com.nsdev.lettergenerator.internal.output.OutputProfiles;
import com.nsdev.lettergenerator.internal.output.PdfPacks;
import com.nsdev.lettergenerator.internal.render.fo.FoComposer;
import com.nsdev.lettergenerator.internal.render.fop.FopRenderResult;
import com.nsdev.lettergenerator.internal.render.fop.FopRenderer;
import com.nsdev.lettergenerator.internal.render.watermark.WatermarkProcessor;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.render.OutputProfile;
import com.nsdev.lettergenerator.render.RenderOptions;
import com.nsdev.lettergenerator.render.RenderResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates the engine stages after model validation has succeeded.
 *
 * <p>Each call composes XSL-FO, renders it with FOP, checks layout diagnostics and optionally
 * applies PDFBox watermarking. A positive signature zone permits one targeted retry with a
 * dedicated final page when only body overflow occurred or the reserved last-page master was not
 * selected. Other failures never trigger a retry, and no partially accepted PDF is returned.
 */
public final class DocumentRenderer {
    /** Immutable generator font registry reused by fresh composition/engine attempts. */
    private final FontRegistry fonts;

    /** Owned explicit language patterns, shared without mutable native parser state. */
    private final HyphenationPatterns patterns;

    /**
     * Binds rendering to one generator's immutable font snapshot.
     *
     * @param fonts trusted registered font bytes plus the built-in font families
     */
    public DocumentRenderer(FontRegistry fonts) {
        this(fonts, HyphenationPatterns.empty());
    }

    /**
     * Binds native rendering to owned explicit font and hyphenation resources.
     *
     * @param fonts immutable registered fonts
     * @param patterns immutable explicit dictionaries
     */
    public DocumentRenderer(FontRegistry fonts, HyphenationPatterns patterns) {
        this.fonts = fonts;
        this.patterns = patterns;
    }

    /**
     * Renders an already validated model and applies its optional watermark only after layout
     * succeeds.
     *
     * @param document immutable input accepted by the shared input validator
     * @return complete PDF bytes after all requested stages succeed
     * @throws PdfGenerationException if composition, layout checks, recovery or watermarking fails
     */
    public byte[] render(LetterDocument document) {
        return render(document, RenderOptions.defaults()).pdfBytes();
    }

    /**
     * Renders an accepted model with optional declared-geometry overlays.
     *
     * @param document validated input
     * @param options requested preview behavior
     * @return accepted PDF result
     */
    public RenderResult render(LetterDocument document, RenderOptions options) {
        OutputProfiles.validate(document, options);
        patterns.validate(document);
        boolean recovered = false;
        boolean reservedLastPage = document.page().signatureZoneMm() > 0;
        FopRenderResult result = compose(document, false, options);
        boolean missingLastMaster = result.isSuccessful() && !result.hasReservedLastMaster();
        if (reservedLastPage && (result.hasOnlyBodyOverflow() || missingLastMaster)) {
            result = compose(document, true, options);
            recovered = true;
        }
        result.requireSuccessful();
        if (reservedLastPage && !result.hasReservedLastMaster()) {
            throw new PdfGenerationException(
                    "FOP did not allocate the reserved last page",
                    new IllegalStateException(result.masters().toString()));
        }
        byte[] bytes =
                document.watermark() == null
                        ? result.bytes()
                        : WatermarkProcessor.apply(result.bytes(), document.watermark(), fonts);
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (recovered) {
            diagnostics.add(
                    new Diagnostic(
                            "SIGNATURE_PAGE_RECOVERY",
                            DiagnosticSeverity.INFO,
                            DiagnosticPhase.LAYOUT,
                            "signature",
                            "Allocated a dedicated final page for the reserved signature zone"));
        }
        if (options.profile() == OutputProfile.TAGGED) {
            diagnostics.add(
                    new Diagnostic(
                            "TAGGED_READING_ORDER",
                            DiagnosticSeverity.INFO,
                            DiagnosticPhase.LAYOUT,
                            "render.profile",
                            "Native tags follow model order; positioned content requires reading-order review. "
                                    + "Tagged output does not guarantee PDF/UA conformance."));
        }
        RenderResult letter =
                options.debug()
                        ? DebugOverlay.apply(bytes, document, result.masters(), diagnostics)
                        : new RenderResult(bytes, result.masters().size(), diagnostics, List.of());
        return PdfPacks.append(letter, document.attachments());
    }

    /**
     * Creates fresh FO and FOP state for one rendering attempt. The recovery flag adds an explicit
     * terminal page to obtain the reserved last-page layout.
     *
     * @param document validated model for this attempt
     * @param dedicatedSignaturePage whether to force a terminal last-page master
     * @param options native output profile
     * @return PDF attempt and typed diagnostics, before acceptance
     */
    private FopRenderResult compose(
            LetterDocument document, boolean dedicatedSignaturePage, RenderOptions options) {
        FoComposer.Composition composition =
                new FoComposer(document, dedicatedSignaturePage).compose();
        if (options.profile() == OutputProfile.PDF_A_2B) {
            OutputProfiles.requireEmbeddedFonts(composition.xml(), fonts);
        }
        return FopRenderer.render(composition, fonts, document.settings(), options, patterns);
    }
}
