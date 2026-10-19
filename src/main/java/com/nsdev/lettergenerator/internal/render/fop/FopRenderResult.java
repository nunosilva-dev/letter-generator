package com.nsdev.lettergenerator.internal.render.fop;

import com.nsdev.lettergenerator.diagnostics.Diagnostic;
import com.nsdev.lettergenerator.diagnostics.DiagnosticPhase;
import com.nsdev.lettergenerator.diagnostics.DiagnosticSeverity;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.internal.render.fo.PageMaster;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of one engine attempt, before the coordinator accepts it for public delivery.
 *
 * <p>Master/diagnostic lists are immutable snapshots. The PDF byte array is internal transport and
 * is not defensively copied here; callers must not expose it until diagnostics and final-page
 * master checks succeed.
 *
 * @param bytes bytes produced by this attempt, even when diagnostics reject the layout
 * @param masters actual page masters in page order
 * @param problems rejected-layout and engine diagnostics collected during rendering
 */
public record FopRenderResult(
        byte[] bytes, List<PageMaster> masters, List<RenderProblem> problems) {
    /**
     * Snapshots diagnostic/master collections while retaining the attempt's PDF bytes.
     *
     * @param bytes engine output for this attempt
     * @param masters actual selected page masters, copied into an immutable list
     * @param problems classified diagnostics, copied into an immutable list
     */
    public FopRenderResult {
        masters = List.copyOf(masters);
        problems = List.copyOf(problems);
    }

    /**
     * Checks whether the actual final viewport used an ONLY or LAST master.
     *
     * @return true when the last rendered page has final-page reservation semantics
     */
    public boolean hasReservedLastMaster() {
        return !masters.isEmpty() && masters.get(masters.size() - 1).isLastPage();
    }

    /**
     * Classifies the only diagnostic set eligible for body-layout recovery.
     *
     * @return true for a nonempty list consisting entirely of recoverable body overflows
     */
    public boolean hasOnlyBodyOverflow() {
        return !problems.isEmpty() && problems.stream().allMatch(RenderProblem::bodyOverflow);
    }

    /**
     * Checks diagnostic success without inspecting final-page reservation.
     *
     * @return true when no engine or painted-bound problems were collected
     */
    public boolean isSuccessful() {
        return problems.isEmpty();
    }

    /**
     * Rejects a result with diagnostics while retaining all human-readable problem messages.
     *
     * @throws PdfGenerationException if any diagnostic rejects this rendering attempt
     */
    public void requireSuccessful() {
        if (!isSuccessful()) {
            String message =
                    problems.stream().map(RenderProblem::message).collect(Collectors.joining("; "));
            throw new PdfGenerationException(
                    "FOP rejected the layout: " + message,
                    new IllegalStateException(message),
                    problems.stream()
                            .map(
                                    problem ->
                                            new Diagnostic(
                                                    problem.bodyOverflow()
                                                            ? "BODY_OVERFLOW"
                                                            : "LAYOUT_ERROR",
                                                    DiagnosticSeverity.ERROR,
                                                    DiagnosticPhase.LAYOUT,
                                                    "layout",
                                                    problem.message()))
                            .toList());
        }
    }
}
