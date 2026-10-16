package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.onePageJson;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.json.JsonInput;
import com.nsdev.lettergenerator.internal.validation.InputValidator;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class InputValidationTest {

    static Stream<String[]> invalidInputs() {
        return Stream.of(
                new String[] {"{\"schemaVersion\":2}", "schemaVersion"},
                new String[] {"{\"schemaVersion\":1,\"sections\":[]}", "sections"},
                new String[] {"{\"schemaVersion\":1,\"surprise\":true}", "surprise"},
                new String[] {"{\"schemaVersion\":1,\"sections\":[null]}", "sections[0]"},
                new String[] {"{\"schemaVersion\":1,\"page\":{\"size\":\"A3\"}}", "page.size"},
                new String[] {
                    "{\"schemaVersion\":1,\"page\":{\"topZoneMm\":-1}}", "page.topZoneMm"
                },
                new String[] {
                    "{\"schemaVersion\":1,\"sections\":[{\"name\":\"body\",\"blocks\":[{\"type\":\"html\"}]}]}",
                    "sections[0].blocks[0]"
                },
                new String[] {
                    "{\"schemaVersion\":1,\"sections\":[{\"name\":\"body\",\"blocks\":[{\"type\":\"paragraph\",\"runs\":[null]}]}]}",
                    "sections[0].blocks[0].runs[0]"
                });
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void identifiesInvalidInputPath(String json, String path) {
        var e =
                assertThrows(
                        InvalidLetterException.class,
                        () -> LetterPdfGenerator.builder().build().generateJson(json));
        assertTrue(e.fieldPath().contains(path), e.getMessage());
    }

    @Test
    void validatesExampleAndPreservesBase64Bytes() throws Exception {
        var doc = JsonInput.read(onePageJson());
        new InputValidator(FontRegistry.empty()).validate(doc);
        assertEquals("body", doc.sections().get(0).name());
        assertEquals(25d, doc.page().topZoneMm());
    }

    @Test
    void rejectsGeometryCollisionsAndInvalidStylesLinksAndImages() throws Exception {
        var mapper = new ObjectMapper();
        for (String field :
                List.of("fontFamily", "fontSizePt", "color", "lineHeight", "spaceAfterMm")) {
            ObjectNode root = (ObjectNode) mapper.readTree(onePageJson());
            ((ObjectNode) root.get("defaultStyle"))
                    .set(
                            field,
                            switch (field) {
                                case "fontFamily" -> new TextNode("NotRegistered");
                                case "color" -> new TextNode("red");
                                default -> new DoubleNode(-1);
                            });
            var e =
                    assertThrows(
                            InvalidLetterException.class,
                            () ->
                                    LetterPdfGenerator.builder()
                                            .build()
                                            .generateJson(root.toString()));
            assertEquals("defaultStyle." + field, e.fieldPath());
        }
        ObjectNode root = (ObjectNode) mapper.readTree(onePageJson());
        ArrayNode fixed = (ArrayNode) root.get("fixedElements");
        fixed.add(fixed.get(0).deepCopy());
        ((ObjectNode) fixed.get(1)).put("id", "second");
        assertTrue(
                assertThrows(
                                InvalidLetterException.class,
                                () ->
                                        LetterPdfGenerator.builder()
                                                .build()
                                                .generateJson(root.toString()))
                        .fieldPath()
                        .startsWith("fixedElements[1]"));
        for (String block :
                List.of(
                        "{\"type\":\"paragraph\",\"runs\":[{\"text\":\"bad\",\"href\":\"file:///etc/passwd\"}]}",
                        "{\"type\":\"image\",\"data\":\"abc!\",\"mimeType\":\"image/png\",\"widthMm\":10,\"heightMm\":10}",
                        "{\"type\":\"image\",\"data\":\"aGVsbG8=\",\"mimeType\":\"image/png\",\"widthMm\":10,\"heightMm\":10}")) {
            ObjectNode r = (ObjectNode) mapper.readTree(onePageJson());
            ((ArrayNode) r.get("sections").get(0).get("blocks")).set(0, mapper.readTree(block));
            assertTrue(
                    assertThrows(
                                    InvalidLetterException.class,
                                    () ->
                                            LetterPdfGenerator.builder()
                                                    .build()
                                                    .generateJson(r.toString()))
                            .fieldPath()
                            .startsWith("sections[0].blocks[0]"));
        }
    }

    @Test
    void immutableModelCopiesListsAndArrays() {
        var runs = new ArrayList<>(List.of(new TextRun("original", null, null)));
        var paragraph = new Paragraph(runs, null);
        runs.clear();
        assertEquals(1, paragraph.runs().size());
        assertThrows(UnsupportedOperationException.class, () -> paragraph.runs().clear());
        byte[] bytes = {1, 2, 3};
        var image = new ImageBlock(bytes, "image/png", 10d, 10d, null);
        bytes[0] = 8;
        image.data()[0] = 9;
        assertEquals(1, image.data()[0]);
    }

    @Test
    void javaInputsHaveSameValidationAndFiniteNumbers() {
        var generator = LetterPdfGenerator.builder().build();
        assertEquals(
                "document",
                assertThrows(InvalidLetterException.class, () -> generator.generate(null))
                        .fieldPath());
        var page = new PageSpec(null, null, Double.NaN, null, null);
        var doc =
                new LetterDocument(
                        1,
                        page,
                        null,
                        List.of(
                                new Section(
                                        "body",
                                        null,
                                        List.of(
                                                new Paragraph(
                                                        List.of(new TextRun("hello", null, null)),
                                                        null)))),
                        null,
                        null,
                        null);
        assertEquals(
                "page.topZoneMm",
                assertThrows(InvalidLetterException.class, () -> generator.generate(doc))
                        .fieldPath());
    }

    @Test
    void invalidFontFailsAtBuildTime() {
        assertThrows(
                InvalidLetterException.class,
                () ->
                        LetterPdfGenerator.builder()
                                .registerFont("Bad", FontVariant.REGULAR, Path.of("pom.xml"))
                                .build());
        assertThrows(
                InvalidLetterException.class,
                () ->
                        LetterPdfGenerator.builder()
                                .registerClasspathFont("Bad", FontVariant.REGULAR, "missing.ttf")
                                .build());
    }
}
