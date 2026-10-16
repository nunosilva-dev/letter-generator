package com.nsdev.lettergenerator.internal.render;

import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.render.fo.FoComposer;
import com.nsdev.lettergenerator.internal.render.fop.FopRenderResult;
import com.nsdev.lettergenerator.internal.render.fop.FopRenderer;
import com.nsdev.lettergenerator.internal.render.watermark.WatermarkProcessor;
import com.nsdev.lettergenerator.model.LetterDocument;

/** Orchestrates composition, the narrow last-page recovery, and optional watermarking. */
public final class DocumentRenderer {
    private final FontRegistry fonts;

    public DocumentRenderer(FontRegistry fonts) {
        this.fonts = fonts;
    }

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

    private FopRenderResult compose(LetterDocument document, boolean dedicatedSignaturePage) {
        FoComposer.Composition composition =
                new FoComposer(document, dedicatedSignaturePage).compose();
        return FopRenderer.render(composition, fonts);
    }
}
