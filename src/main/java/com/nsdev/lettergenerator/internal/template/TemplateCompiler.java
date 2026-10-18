package com.nsdev.lettergenerator.internal.template;

import com.nsdev.lettergenerator.internal.markup.HtmlParser;
import com.nsdev.lettergenerator.internal.markup.MarkupSupport;
import com.nsdev.lettergenerator.model.LetterDocument;
import java.util.Map;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

/** Coordinates parsing, safe binding and registered region handlers into the shared model. */
public final class TemplateCompiler {
    private static final TemplateTagRegistry TAGS = TemplateTagRegistry.standard();

    private TemplateCompiler() {}

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
