package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.HtmlParser;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.model.DocumentSettings;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.template.TemplateRequirements;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.util.Map;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

/**
 * Coordinator between {@link com.nsdev.lettergenerator.builder.HtmlLetterBuilder} and the shared
 * immutable document model. A single immutable registry reuses stateless internal region handlers;
 * every compile call owns a fresh parsed tree, builder and {@link TemplateContext}. This class
 * exposes no custom-tag registration SPI.
 */
public final class TemplateCompiler {
    /** Immutable standard registry shared by independent compilation calls. */
    private static final TemplateTagRegistry TAGS = TemplateTagRegistry.standard();

    /** Prevents construction of this static helper. */
    private TemplateCompiler() {}

    /**
     * Parses one letter root, optionally binds existing node values, reads page/style defaults,
     * registers supplied assets on a fresh builder, then runs the standard region handlers.
     * Handlers execute in registry order, independent of source region order; the final builder
     * resolves deferred header geometry and assembles the document.
     *
     * @param source non-null HTML fragment containing exactly one letter element
     * @param values typed values used for map-only binding and finite directives; consulted only
     *     when enabled
     * @param assets non-null named image byte map without null keys/values; builder copies bytes
     * @param substitute whether to resolve placeholders in parsed text and attribute values
     * @return compiled immutable model, before the generator's shared semantic validation pass
     * @throws IllegalStateException if the required main region produces no body blocks
     * @throws IllegalArgumentException if source is null or builder geometry arguments are invalid
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if parsing, binding,
     *     region structure or supported markup rules fail
     */
    public static LetterDocument compile(
            String source, Map<String, ?> values, Map<String, byte[]> assets, boolean substitute) {
        return compile(source, values, assets, substitute, null);
    }

    /**
     * Compiles with role defaults known before builder fallbacks are applied.
     *
     * @param source supported markup
     * @param values typed map-only bindings
     * @param assets named image bytes
     * @param substitute whether replacements are enabled
     * @param theme optional document theme
     * @return immutable unresolved composition model
     */
    public static LetterDocument compile(
            String source,
            Map<String, ?> values,
            Map<String, byte[]> assets,
            boolean substitute,
            LetterTheme theme) {
        return compile(prepare(source), values, assets, substitute, theme, null);
    }

    /**
     * Parses once into an opaque owned prototype for repeated safe binding.
     *
     * @param source supported source
     * @return immutable prototype handle
     */
    public static Prototype prepare(String source) {
        return new Prototype(root(source));
    }

    /**
     * Clones a prototype and compiles one independent binding.
     *
     * @param prototype owned parsed source
     * @param values owned typed values
     * @param assets named assets
     * @param substitute whether directives/substitution are enabled
     * @param theme optional theme
     * @param settings optional explicit document settings
     * @return fresh immutable model
     */
    public static LetterDocument compile(
            Prototype prototype,
            Map<String, ?> values,
            Map<String, byte[]> assets,
            boolean substitute,
            LetterTheme theme,
            DocumentSettings settings) {
        Element root = prototype.copy();
        LocaleSettings locale =
                TemplatePage.locale(root, settings == null ? null : settings.locale());
        if (substitute) {
            var wrapper = new Element("body").appendChild(root);
            new TemplateBindings(values, locale).apply(root);
            MarkupSupport.require(
                    root.parent() == wrapper,
                    root,
                    "Template conditions removed the required letter root");
        }
        TemplatePage page = TemplatePage.read(root);
        var builder = page.builder();
        if (settings != null) {
            builder.settings(settings);
        }
        if (settings != null || TemplatePage.hasLocale(root)) {
            builder.locale(locale);
        }
        if (theme != null) {
            builder.theme(theme);
        }
        assets.forEach(builder::asset);
        TAGS.apply(root, new TemplateContext(builder, page, assets, theme));
        return builder.build();
    }

    /** Opaque parsed prototype; callers can obtain only owned clones and immutable manifests. */
    public static final class Prototype {
        /** Never mutated after capture. */
        private final Element root;

        /** Owned immutable requirement manifest. */
        private final TemplateRequirements requirements;

        /**
         * Captures the already-owned root.
         *
         * @param root parsed source
         */
        private Prototype(Element root) {
            this.root = root;
            requirements = TemplateManifest.read(root);
        }

        /**
         * @return fresh mutable parsed clone owned by the caller
         */
        public Element copy() {
            return root.clone();
        }

        /**
         * @return immutable syntactic input contract
         */
        public TemplateRequirements requirements() {
            return requirements;
        }
    }

    /**
     * Requires exactly one letter element in the parsed body. Outside it, only comments and blank
     * text survive the contract; other content is rejected rather than silently discarded.
     *
     * @param source non-null source fragment containing the letter element
     * @return the single parsed letter element
     */
    private static Element root(String source) {
        Element body = HtmlParser.body(source);
        MarkupSupport.require(
                body.childrenSize() == 1 && body.child(0).normalName().equals("letter"),
                body,
                "Expected one letter root element");
        for (var node : body.childNodes()) {
            MarkupSupport.require(
                    node instanceof Element
                            || node instanceof Comment
                            || node instanceof TextNode text && text.isBlank(),
                    node,
                    "Unexpected content outside letter");
        }
        return body.child(0);
    }
}
