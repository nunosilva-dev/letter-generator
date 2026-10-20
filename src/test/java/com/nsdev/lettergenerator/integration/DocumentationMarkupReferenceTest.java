package com.nsdev.lettergenerator.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.component.LetterComponent;
import com.nsdev.lettergenerator.fixtures.TestResources;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class DocumentationMarkupReferenceTest {
    @TestFactory
    List<DynamicTest> everyDocumentedTagExampleCompilesAndRenders() throws Exception {
        var catalogue =
                new ObjectMapper()
                        .readTree(Path.of("website/src/data/markup-reference.json").toFile());
        assertTrue(catalogue.get("tags").size() >= 50);
        var tests = new ArrayList<DynamicTest>();
        var generator = new LetterPdfGenerator();
        byte[] image = TestResources.bytes(TestResources.ENERGY_LOGO);
        for (var tag : catalogue.get("tags")) {
            String name = tag.get("name").asText();
            String example = tag.get("example").asText();
            tests.add(
                    DynamicTest.dynamicTest(
                            "Reference example: " + name,
                            () -> {
                                var document =
                                        Letter.template(completeLetter(name, example))
                                                .values(
                                                        Map.of(
                                                                "name",
                                                                "Alex Example",
                                                                "total",
                                                                new BigDecimal("12.50"),
                                                                "showNotice",
                                                                true))
                                                .asset("logo", image)
                                                .asset("chart", image)
                                                .component(
                                                        "legal",
                                                        new LetterComponent(
                                                                List.of(
                                                                        new Paragraph(
                                                                                List.of(
                                                                                        new TextRun(
                                                                                                "Example legal text",
                                                                                                null,
                                                                                                null)),
                                                                                null))))
                                                .build();
                                assertTrue(generator.render(document).pageCount() > 0);
                            }));
        }
        return tests;
    }

    private static String completeLetter(String name, String example) {
        if (name.equals("letter")) {
            return example;
        }
        String content =
                switch (name) {
                    case "main",
                                    "header",
                                    "footer",
                                    "logo",
                                    "signature",
                                    "watermark",
                                    "sender",
                                    "recipient",
                                    "subject",
                                    "salutation",
                                    "closing",
                                    "date",
                                    "reference" ->
                            example;
                    case "thead" ->
                            "<main><table>"
                                    + example
                                    + "<tbody><tr><td>Item</td><td>12.50</td></tr></tbody></table></main>";
                    case "tbody", "tr" -> "<main><table>" + example + "</table></main>";
                    case "th" ->
                            "<main><table><thead><tr>"
                                    + example
                                    + "</tr></thead><tbody><tr><td>Item</td><td>12.50</td></tr></tbody></table></main>";
                    case "td" ->
                            "<main><table><tbody><tr>" + example + "</tr></tbody></table></main>";
                    case "li" -> "<main><ul>" + example + "</ul></main>";
                    case "column" -> "<main><columns>" + example + "</columns></main>";
                    case "page-break" -> example;
                    default -> "<main>" + example + "</main>";
                };
        if (!content.contains("<main>")) {
            content += "<main><p>Example body</p></main>";
        }
        return "<letter>" + content + "</letter>";
    }
}
