package com.nsdev.lettergenerator.internal.template;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.internal.markup.HtmlParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class TemplateTagRegistryTest {
    @Test
    void registeringAnAdditionalHandlerRendersItsContentWithoutChangingTheCompiler()
            throws Exception {
        var handlers =
                new ArrayList<TemplateTagHandler>(
                        List.of(new MainTagHandler(), new NoticeTagHandler()));
        var registry = new TemplateTagRegistry(handlers);
        handlers.clear();
        Element root =
                HtmlParser.body(
                                "<letter><notice>Important notice</notice><main><p>Body</p></main></letter>")
                        .child(0);
        TemplatePage page = TemplatePage.read(root);
        var context = new TemplateContext(page.builder(), page, Map.of());
        registry.apply(root, context);
        String rendered = text(new LetterPdfGenerator().generate(context.builder().build()));
        assertTrue(rendered.contains("Body"));
        assertTrue(rendered.contains("Important notice"));
        assertTrue(rendered.indexOf("Body") < rendered.indexOf("Important notice"));
    }

    @Test
    void duplicateHandlerNamesCannotSilentlyOverrideAnExistingHandler() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TemplateTagRegistry(List.of(new MainTagHandler(), new MainTagHandler())));
    }

    private static final class NoticeTagHandler implements TemplateTagHandler {
        @Override
        public String tagName() {
            return "notice";
        }

        @Override
        public void handle(Element element, TemplateContext context) {
            context.builder().heading(element.text());
        }
    }
}
