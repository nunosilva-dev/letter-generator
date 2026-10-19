package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.builder.LetterBuilder;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.util.Map;

/**
 * Per-compilation state; reusable handlers never retain a builder or input assets.
 *
 * @param builder fresh mutable output builder owned by this compilation
 * @param page parsed immutable page/default-style options
 * @param assets named encoded image bytes, with a copied map but shared byte values
 * @param theme optional final document theme for rich cell image geometry
 */
record TemplateContext(
        LetterBuilder builder, TemplatePage page, Map<String, byte[]> assets, LetterTheme theme) {
    /**
     * Snapshots map membership; byte values are shared and read only during compilation.
     *
     * @param builder fresh mutable builder owned by the compilation
     * @param page immutable parsed page and root-style options
     * @param assets named encoded image bytes consulted during this traversal
     * @param theme optional final document theme
     */
    TemplateContext {
        assets = Map.copyOf(assets);
    }

    /**
     * Preserves compilation contexts without theme-aware table defaults.
     *
     * @param builder fresh output builder
     * @param page parsed page options
     * @param assets named encoded image bytes
     */
    TemplateContext(LetterBuilder builder, TemplatePage page, Map<String, byte[]> assets) {
        this(builder, page, assets, null);
    }
}
