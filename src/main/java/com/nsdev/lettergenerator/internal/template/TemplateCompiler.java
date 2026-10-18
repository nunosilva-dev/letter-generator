package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.HtmlParser;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.LetterDocument;
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
     * @param values scalar strings used for map-only substitution; consulted only when enabled
     * @param assets non-null named image byte map without null keys/values; builder copies bytes
     * @param substitute whether to resolve placeholders in parsed text and attribute values
     * @return compiled immutable model, before the generator's shared semantic validation pass
     * @throws IllegalStateException if the required main region produces no body blocks
     * @throws IllegalArgumentException if source is null or builder geometry arguments are invalid
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if parsing, binding,
     *     region structure or supported markup rules fail
     */
    public static LetterDocument compile(
            String source,
            Map<String, String> values,
            Map<String, byte[]> assets,
            boolean substitute) {
        Element root = root(source);
        if (substitute) {
            new TemplateBindings(values).apply(root);
        }
        TemplatePage page = TemplatePage.read(root);
        var builder = page.builder();
        assets.forEach(builder::asset);
        TAGS.apply(root, new TemplateContext(builder, page, assets));
        return builder.build();
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
