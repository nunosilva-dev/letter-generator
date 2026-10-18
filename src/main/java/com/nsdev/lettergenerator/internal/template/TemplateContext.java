package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.builder.LetterBuilder;
import java.util.Map;

/** Per-compilation state; reusable tag handlers never retain a builder or input assets. */
record TemplateContext(LetterBuilder builder, TemplatePage page, Map<String, byte[]> assets) {
    TemplateContext {
        assets = Map.copyOf(assets);
    }
}
