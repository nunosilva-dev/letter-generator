package com.nsdev.lettergenerator.internal.render;

import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.render.fo.FoComposer;
import com.nsdev.lettergenerator.internal.render.fop.FopRenderResult;
import com.nsdev.lettergenerator.internal.render.fop.FopRenderer;
import com.nsdev.lettergenerator.internal.render.watermark.WatermarkProcessor;
import com.nsdev.lettergenerator.model.LetterDocument;

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

    /**
     * Binds rendering to one generator's immutable font snapshot.
     *
     * @param fonts trusted registered font bytes plus the built-in font families
     */
    public DocumentRenderer(FontRegistry fonts) {
        this.fonts = fonts;
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
        boolean reservedLastPage = document.page().signatureZoneMm() > 0;
        FopRenderResult result = compose(document, false);
        boolean missingLastMaster = result.isSuccessful() && !result.hasReservedLastMaster();
        if (reservedLastPage && (result.hasOnlyBodyOverflow() || missingLastMaster)) {
            result = compose(document, true);
        }
        result.requireSuccessful();
        if (reservedLastPage && !result.hasReservedLastMaster()) {
            throw new PdfGenerationException(
                    "FOP did not allocate the reserved last page",
                    new IllegalStateException(result.masters().toString()));
        }
        return document.watermark() == null
                ? result.bytes()
                : WatermarkProcessor.apply(result.bytes(), document.watermark(), fonts);
    }

    /**
     * Creates fresh FO and FOP state for one rendering attempt. The recovery flag adds an explicit
     * terminal page to obtain the reserved last-page layout.
     *
     * @param document validated model for this attempt
     * @param dedicatedSignaturePage whether to force a terminal last-page master
     * @return PDF attempt and typed diagnostics, before acceptance
     */
    private FopRenderResult compose(LetterDocument document, boolean dedicatedSignaturePage) {
        FoComposer.Composition composition =
                new FoComposer(document, dedicatedSignaturePage).compose();
        return FopRenderer.render(composition, fonts);
    }
}
