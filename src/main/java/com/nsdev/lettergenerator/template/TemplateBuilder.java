package com.nsdev.lettergenerator.template;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.builder.BuilderChecks;
import com.nsdev.lettergenerator.internal.template.TemplateCompiler;
import com.nsdev.lettergenerator.model.LetterDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable request builder for a stored letter template and application-supplied scalar values. */
public final class TemplateBuilder {
    public static final String FORMAT = "LETTER_HTML_V1";
    private final String source;
    private final boolean substitute;
    private final Map<String, byte[]> assets = new LinkedHashMap<>();
    private Map<String, String> values = Map.of();

    private TemplateBuilder(String source, boolean substitute) {
        this.source = BuilderChecks.required(source, "template markup");
        this.substitute = substitute;
    }

    public static TemplateBuilder template(String source) {
        return new TemplateBuilder(source, true);
    }

    public static TemplateBuilder resolvedHtml(String source) {
        return new TemplateBuilder(source, false);
    }

    public TemplateBuilder values(Map<String, ?> replacements) {
        BuilderChecks.require(
                substitute,
                "Use Letter.template(...) when supplying placeholder values; fromHtml expects already-resolved markup");
        BuilderChecks.required(replacements, "template values");
        var snapshot = new LinkedHashMap<String, String>();
        replacements.forEach(
                (key, value) -> {
                    BuilderChecks.require(
                            key != null && !key.isBlank(), "Variable key must not be blank");
                    BuilderChecks.required(value, "value for " + key);
                    BuilderChecks.require(
                            value instanceof CharSequence
                                    || value instanceof Number
                                    || value instanceof Boolean
                                    || value instanceof Character,
                            "Template values must be scalar: " + key);
                    snapshot.put(key, value.toString());
                });
        values = Map.copyOf(snapshot);
        return this;
    }

    public TemplateBuilder asset(String name, byte[] data) {
        BuilderChecks.require(name != null && !name.isBlank(), "Asset name must not be blank");
        assets.put(name, BuilderChecks.required(data, "asset bytes").clone());
        return this;
    }

    public TemplateBuilder asset(String name, Path path) throws IOException {
        return asset(name, Files.readAllBytes(path));
    }

    public LetterDocument build() {
        try {
            return TemplateCompiler.compile(source, values, assets, substitute);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new InvalidLetterException("template", exception.getMessage(), exception);
        }
    }
}
