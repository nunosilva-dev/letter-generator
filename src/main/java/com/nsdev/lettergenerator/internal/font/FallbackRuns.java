package com.nsdev.lettergenerator.internal.font;

import com.nsdev.lettergenerator.diagnostics.Diagnostic;
import com.nsdev.lettergenerator.diagnostics.DiagnosticPhase;
import com.nsdev.lettergenerator.diagnostics.DiagnosticSeverity;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.style.ResolvedStyle;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Resolves complete grapheme clusters while preserving native links and dynamic page fields. */
final class FallbackRuns {
    /** Java's extended Unicode grapheme matcher, including combining sequences and joiners. */
    private static final Pattern GRAPHEME = Pattern.compile("\\X");

    /** Registered exact faces used for validation. */
    private final FontRegistry fonts;

    /** Per-call coverage independent of installed system fonts. */
    private final FontCoverage coverage;

    /** Ordered explicit fallback families. */
    private final List<String> fallbacks;

    /** Caller-owned per-preparation diagnostic accumulator. */
    private final List<Diagnostic> diagnostics;

    /**
     * Binds one preparation's owned font coverage and reporting state.
     *
     * @param fonts registered faces
     * @param fallbacks configured ordered families
     * @param diagnostics per-call diagnostic accumulator
     */
    FallbackRuns(FontRegistry fonts, List<String> fallbacks, List<Diagnostic> diagnostics) {
        this.fonts = fonts;
        this.fallbacks = fallbacks;
        this.diagnostics = diagnostics;
        coverage = new FontCoverage(fonts);
        for (int index = 0; index < fallbacks.size(); index++) {
            fonts.requireFamily(fallbacks.get(index), "settings.fallbackFonts[" + index + "]");
        }
    }

    /**
     * Materializes only necessary font changes; invalid null entries remain for ordinary
     * validation.
     *
     * @param runs ordered nullable inline content
     * @param inherited effective paragraph or cell typography
     * @param path source model path
     * @return replacement inline content preserving text order and controls
     */
    List<TextRun> prepare(List<TextRun> runs, ResolvedStyle inherited, String path) {
        if (runs == null) {
            return null;
        }
        List<TextRun> result = new ArrayList<>();
        for (int index = 0; index < runs.size(); index++) {
            TextRun run = runs.get(index);
            String runPath = path + "[" + index + "]";
            if (run == null
                    || run.text() == null
                    || run.pageNumber() != null
                    || run.text().isEmpty()) {
                result.add(run);
                continue;
            }
            ResolvedStyle style = inherited.overlay(run.style());
            fonts.requireVariant(style.fontFamily(), style.variant(), runPath + ".style");
            var matcher = GRAPHEME.matcher(run.text());
            String family = null;
            StringBuilder text = new StringBuilder();
            while (matcher.find()) {
                String cluster = matcher.group();
                String selected = choose(cluster, style, runPath);
                if (family != null && !family.equals(selected)) {
                    result.add(segment(run, text.toString(), family, style, runPath));
                    text.setLength(0);
                }
                family = selected;
                text.append(cluster);
            }
            if (family != null) {
                result.add(segment(run, text.toString(), family, style, runPath));
            }
        }
        return result;
    }

    /**
     * Selects the first face covering an entire cluster; absent variants cannot be synthesized.
     *
     * @param cluster complete Unicode grapheme
     * @param style resolved primary face
     * @param path source run path
     * @return selected configured family
     */
    private String choose(String cluster, ResolvedStyle style, String path) {
        if (coverage.supports(style.fontFamily(), style.variant(), cluster)) {
            return style.fontFamily();
        }
        for (String family : fallbacks) {
            if (coverage.supports(family, style.variant(), cluster)) {
                return family;
            }
        }
        throw new InvalidLetterException(
                path,
                "No configured font with variant "
                        + style.variant()
                        + " covers grapheme "
                        + cluster.codePoints()
                                .mapToObj(
                                        value ->
                                                "U+"
                                                        + Integer.toHexString(value)
                                                                .toUpperCase(java.util.Locale.ROOT))
                                .toList());
    }

    /**
     * Writes one same-font segment, retaining all run-level overrides and hyperlink ownership.
     *
     * @param original original run
     * @param text segment text
     * @param family selected configured family
     * @param style effective primary typography
     * @param path source run path
     * @return native replacement run
     */
    private TextRun segment(
            TextRun original, String text, String family, ResolvedStyle style, String path) {
        if (family.equals(style.fontFamily())) {
            return new TextRun(text, original.style(), original.href(), original.pageNumber());
        }
        Style override = new Style(family, null, null, null, null, null, null, null, null, null);
        Style local = original.style() == null ? override : original.style().overlay(override);
        diagnostics.add(
                new Diagnostic(
                        "FONT_SUBSTITUTION",
                        DiagnosticSeverity.INFO,
                        DiagnosticPhase.INPUT,
                        path,
                        "Used configured font "
                                + family
                                + " / "
                                + style.variant()
                                + " in place of "
                                + style.fontFamily()
                                + " for a complete text cluster"));
        return new TextRun(text, local, original.href(), original.pageNumber());
    }
}
