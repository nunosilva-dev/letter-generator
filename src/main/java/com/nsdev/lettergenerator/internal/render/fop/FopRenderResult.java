package com.nsdev.lettergenerator.internal.render.fop;

import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.internal.render.fo.PageMaster;
import java.util.List;
import java.util.stream.Collectors;

/** Internal bytes are returned to the public caller only after every diagnostic is checked. */
public record FopRenderResult(
        byte[] bytes, List<PageMaster> masters, List<RenderProblem> problems) {
    public FopRenderResult {
        masters = List.copyOf(masters);
        problems = List.copyOf(problems);
    }

    public boolean hasReservedLastMaster() {
        return !masters.isEmpty() && masters.get(masters.size() - 1).isLastPage();
    }

    public boolean hasOnlyBodyOverflow() {
        return !problems.isEmpty() && problems.stream().allMatch(RenderProblem::bodyOverflow);
    }

    public boolean isSuccessful() {
        return problems.isEmpty();
    }

    public void requireSuccessful() {
        if (!isSuccessful()) {
            String message =
                    problems.stream().map(RenderProblem::message).collect(Collectors.joining("; "));
            throw new PdfGenerationException(
                    "FOP rejected the layout: " + message, new IllegalStateException(message));
        }
    }
}
