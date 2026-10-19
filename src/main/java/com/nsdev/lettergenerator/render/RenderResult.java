package com.nsdev.lettergenerator.render;

import com.nsdev.lettergenerator.diagnostics.Diagnostic;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Complete accepted PDF with actual page count and optional authoring geometry.
 *
 * @param pdfBytes complete PDF file; defensively copied on input and access
 * @param pageCount actual final page count, including any signature recovery page
 * @param diagnostics accepted processing diagnostics
 * @param debugBoxes declared geometry; empty for ordinary rendering
 * @param parts contiguous letter and attachment page ranges in output order
 */
public record RenderResult(
        byte[] pdfBytes,
        int pageCount,
        List<Diagnostic> diagnostics,
        List<DebugBox> debugBoxes,
        List<RenderPart> parts) {
    /**
     * Owns all mutable inputs.
     *
     * @param pdfBytes complete PDF bytes
     * @param pageCount final page count
     * @param diagnostics ordered diagnostic list
     * @param debugBoxes ordered declared geometry
     * @param parts ordered source ranges
     */
    public RenderResult {
        pdfBytes = pdfBytes.clone();
        diagnostics = List.copyOf(diagnostics);
        debugBoxes = List.copyOf(debugBoxes);
        parts = List.copyOf(parts);
    }

    /**
     * Preserves the original result constructor for one generated letter.
     *
     * @param pdfBytes accepted PDF bytes
     * @param pageCount complete letter page count
     * @param diagnostics accepted processing diagnostics
     * @param debugBoxes declared preview geometry
     */
    public RenderResult(
            byte[] pdfBytes,
            int pageCount,
            List<Diagnostic> diagnostics,
            List<DebugBox> debugBoxes) {
        this(
                pdfBytes,
                pageCount,
                diagnostics,
                debugBoxes,
                List.of(new RenderPart(RenderPartKind.LETTER, 1, pageCount)));
    }

    /**
     * @return an independent copy of the complete PDF bytes
     */
    @Override
    public byte[] pdfBytes() {
        return pdfBytes.clone();
    }

    /**
     * Compares PDF contents and result metadata.
     *
     * @param other object to compare
     * @return true for equal PDF bytes and metadata
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof RenderResult result
                && pageCount == result.pageCount
                && Arrays.equals(pdfBytes, result.pdfBytes)
                && diagnostics.equals(result.diagnostics)
                && debugBoxes.equals(result.debugBoxes)
                && parts.equals(result.parts);
    }

    /**
     * @return content-based hash consistent with equality
     */
    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(pdfBytes), pageCount, diagnostics, debugBoxes, parts);
    }
}
