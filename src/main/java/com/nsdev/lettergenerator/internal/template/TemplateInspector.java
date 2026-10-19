package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.diagnostics.Diagnostic;
import com.nsdev.lettergenerator.diagnostics.DiagnosticPhase;
import com.nsdev.lettergenerator.diagnostics.DiagnosticSeverity;
import com.nsdev.lettergenerator.diagnostics.ValidationReport;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.content.ImageBlocks;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.model.DocumentSettings;
import com.nsdev.lettergenerator.model.LetterDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jsoup.nodes.Element;

/** Inspects parsed values and assets without creating FO or a final PDF. */
public final class TemplateInspector {
    /** Only accepted template image source scheme. */
    private static final String ASSET_PREFIX = "asset:";

    /** Positive decode-only fitting bound; inspection does not calculate display geometry. */
    private static final double DECODE_BOX_MM = 1.0;

    /** Prevents instances of the inspection entry point. */
    private TemplateInspector() {}

    /**
     * Aggregates missing variable occurrences and invalid referenced assets before semantic checks.
     *
     * @param source supported letter markup
     * @param values literal map-only bindings
     * @param assets named encoded image bytes
     * @param substitute whether binding is enabled
     * @param generator font-aware semantic validator
     * @return immutable authoring report
     */
    public static ValidationReport inspect(
            String source,
            Map<String, ?> values,
            Map<String, byte[]> assets,
            boolean substitute,
            LetterPdfGenerator generator) {
        return inspect(
                source,
                values,
                assets,
                substitute,
                generator,
                () -> TemplateCompiler.compile(source, values, assets, substitute));
    }

    /**
     * Inspects with document-local theme/component configuration applied before semantic
     * validation. Compilation is attempted only after all binding and asset checks pass; semantic
     * validation reports the first remaining error. No layout engine runs, so fit is not
     * guaranteed.
     *
     * @param source supported letter markup
     * @param values literal map-only bindings
     * @param assets named encoded image bytes
     * @param substitute whether binding is enabled
     * @param generator font-aware validator
     * @param compile deferred model compilation after binding/asset preflight
     * @return immutable authoring report
     */
    public static ValidationReport inspect(
            String source,
            Map<String, ?> values,
            Map<String, byte[]> assets,
            boolean substitute,
            LetterPdfGenerator generator,
            Supplier<LetterDocument> compile) {
        return inspect(source, values, assets, substitute, null, generator, compile);
    }

    /**
     * Inspects source using the same explicit settings as its builder.
     *
     * @param source supported markup
     * @param values owned typed values
     * @param assets named bytes
     * @param substitute binding mode
     * @param settings explicit document settings
     * @param generator font-aware validator
     * @param compile configuration-aware compiler
     * @return immutable authoring report
     */
    public static ValidationReport inspect(
            String source,
            Map<String, ?> values,
            Map<String, byte[]> assets,
            boolean substitute,
            DocumentSettings settings,
            LetterPdfGenerator generator,
            Supplier<LetterDocument> compile) {
        try {
            return inspect(
                    TemplateCompiler.prepare(source),
                    values,
                    assets,
                    substitute,
                    settings,
                    generator,
                    compile);
        } catch (InvalidLetterException exception) {
            return new ValidationReport(
                    List.of(
                            error(
                                    "INVALID_TEMPLATE",
                                    exception.fieldPath(),
                                    exception.getMessage())));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return new ValidationReport(
                    List.of(error("INVALID_TEMPLATE", "template", exception.getMessage())));
        }
    }

    /**
     * Inspects a prepared clone with the same typed scopes and locale as normal binding.
     *
     * @param prototype parsed source
     * @param values owned typed bindings
     * @param assets owned named bytes
     * @param substitute whether directives are enabled
     * @param settings optional explicit formatting settings
     * @param generator font-aware validator
     * @param compile complete configuration-aware compilation supplier
     * @return aggregate authoring report; semantic errors retain first-error behavior
     */
    public static ValidationReport inspect(
            TemplateCompiler.Prototype prototype,
            Map<String, ?> values,
            Map<String, byte[]> assets,
            boolean substitute,
            DocumentSettings settings,
            LetterPdfGenerator generator,
            Supplier<LetterDocument> compile) {
        var diagnostics = new ArrayList<Diagnostic>();
        try {
            Element parsed = prototype.copy();
            if (substitute) {
                LocaleSettings locale =
                        TemplatePage.locale(parsed, settings == null ? null : settings.locale());
                new TemplateBindings(
                                values,
                                locale,
                                (key, path) ->
                                        diagnostics.add(
                                                error(
                                                        "MISSING_VARIABLE",
                                                        path,
                                                        "Missing variable: " + key)))
                        .apply(parsed);
            }
            for (Element image : parsed.select("img")) {
                asset(image, assets, diagnostics);
            }
            if (diagnostics.isEmpty()) {
                return generator.validate(compile.get());
            }
        } catch (InvalidLetterException exception) {
            diagnostics.add(
                    error("INVALID_TEMPLATE", exception.fieldPath(), exception.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            diagnostics.add(error("INVALID_TEMPLATE", "template", exception.getMessage()));
        }
        return new ValidationReport(diagnostics);
    }

    /**
     * Checks each referenced asset independently without opening paths or network resources.
     *
     * @param image parsed image element after binding
     * @param assets supplied named bytes
     * @param diagnostics output errors
     */
    private static void asset(
            Element image, Map<String, byte[]> assets, List<Diagnostic> diagnostics) {
        String source = image.attr("src");
        String path = MarkupSupport.path(image) + "@src";
        if (diagnostics.stream()
                .anyMatch(
                        item ->
                                item.code().equals("MISSING_VARIABLE")
                                        && item.path().equals(path))) {
            return;
        }
        if (!source.startsWith(ASSET_PREFIX) || source.length() == ASSET_PREFIX.length()) {
            diagnostics.add(
                    error("INVALID_ASSET_REFERENCE", path, "Image src must use asset:name"));
            return;
        }
        byte[] bytes = assets.get(source.substring(ASSET_PREFIX.length()));
        if (bytes == null) {
            diagnostics.add(error("MISSING_ASSET", path, "Missing image asset: " + source));
            return;
        }
        try {
            ImageBlocks.fit(bytes, DECODE_BOX_MM, DECODE_BOX_MM);
        } catch (IllegalArgumentException exception) {
            diagnostics.add(
                    error(
                            "INVALID_ASSET",
                            path,
                            "Invalid image asset " + source + ": " + exception.getMessage()));
        }
    }

    /**
     * Creates a structured input error.
     *
     * @param code stable classification
     * @param path input location
     * @param message explanation
     * @return error diagnostic
     */
    private static Diagnostic error(String code, String path, String message) {
        return new Diagnostic(code, DiagnosticSeverity.ERROR, DiagnosticPhase.INPUT, path, message);
    }
}
