package com.nsdev.lettergenerator.template;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.component.LetterComponent;
import com.nsdev.lettergenerator.diagnostics.ValidationReport;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.template.TemplateCompiler;
import com.nsdev.lettergenerator.internal.template.TemplateData;
import com.nsdev.lettergenerator.internal.template.TemplateInspector;
import com.nsdev.lettergenerator.model.DocumentSettings;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable parsed template and configuration, safe for concurrent independent binding. */
public final class PreparedLetterTemplate {
    /** Parsed immutable source prototype. */
    private final TemplateCompiler.Prototype prototype;

    /** Owned default typed values. */
    private final Map<String, Object> values;

    /** Owned encoded image bytes, never exposed. */
    private final Map<String, byte[]> assets;

    /** Whether directives and placeholder binding are enabled. */
    private final boolean substitute;

    /** Optional immutable document theme. */
    private final LetterTheme theme;

    /** Owned component registrations. */
    private final Map<String, LetterComponent> components;

    /** Optional explicit document settings. */
    private final DocumentSettings settings;

    /**
     * Captures source and configuration from a builder, parsing once.
     *
     * @param source supported markup
     * @param values typed default bindings
     * @param assets explicitly named encoded images
     * @param substitute binding mode
     * @param theme optional theme
     * @param components reusable model components
     * @param settings optional document settings
     */
    public PreparedLetterTemplate(
            String source,
            Map<String, ?> values,
            Map<String, byte[]> assets,
            boolean substitute,
            LetterTheme theme,
            Map<String, LetterComponent> components,
            DocumentSettings settings) {
        prototype = TemplateCompiler.prepare(source);
        this.values = TemplateData.snapshot(values);
        var ownedAssets = new LinkedHashMap<String, byte[]>();
        assets.forEach((name, data) -> ownedAssets.put(name, data.clone()));
        this.assets = Collections.unmodifiableMap(ownedAssets);
        this.substitute = substitute;
        this.theme = theme;
        this.components = Map.copyOf(components);
        this.settings = settings;
    }

    /**
     * @return immutable syntactic requirements with source selectors
     */
    public TemplateRequirements requirements() {
        return prototype.requirements();
    }

    /**
     * @return a fresh document using the bindings captured at compile time
     */
    public LetterDocument build() {
        return compile(values);
    }

    /**
     * Replaces all captured bindings for this call without changing the prepared template.
     *
     * @param replacements non-null owned typed data
     * @return independent immutable letter document
     */
    public LetterDocument bind(Map<String, ?> replacements) {
        if (!substitute) {
            throw new IllegalArgumentException("Resolved markup does not accept bindings");
        }
        return compile(TemplateData.snapshot(replacements));
    }

    /**
     * @return authoring report using built-in fonts and captured bindings
     */
    public ValidationReport validate() {
        return validate(new LetterPdfGenerator());
    }

    /**
     * Validates captured bindings and configuration without native layout.
     *
     * @param generator explicit font-aware validator
     * @return aggregated authoring report
     */
    public ValidationReport validate(LetterPdfGenerator generator) {
        return TemplateInspector.inspect(
                prototype, values, assets, substitute, settings, generator, this::build);
    }

    /**
     * Compiles a fresh clone and preserves configured theme/components/settings.
     *
     * @param binding immutable owned typed values
     * @return immutable model
     */
    private LetterDocument compile(Map<String, ?> binding) {
        try {
            return TemplateCompiler.compile(prototype, binding, assets, substitute, theme, settings)
                    .withTheme(theme)
                    .withComponents(components);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new InvalidLetterException("template", exception.getMessage(), exception);
        }
    }
}
