package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Acceptance checks for owned typed data and finite template directives. */
class TypedTemplateTest {
    @Test
    void countryDatesAndCurrencyUseExplicitLocale() throws Exception {
        var values = Map.of("date", LocalDate.of(2026, 10, 19), "amount", 1234.5);
        String source =
                "<letter language-tag='%s' currency='EUR'><main><p><param key='date' format='date'> <param key='amount' format='currency'></p></main></letter>";
        String british =
                text(
                        new LetterPdfGenerator()
                                .generate(
                                        Letter.template(source.formatted("en-GB"))
                                                .values(values)
                                                .build()));
        String american =
                text(
                        new LetterPdfGenerator()
                                .generate(
                                        Letter.template(source.formatted("en-US"))
                                                .values(values)
                                                .build()));
        assertTrue(british.contains("19 October 2026"), british);
        assertTrue(american.contains("October 19, 2026"), american);
        assertTrue(british.contains("1,234.50"), british);
    }

    @Test
    void rowsAndNestedAliasesRemainScopedAndBoundValuesNeverCreateMarkup() throws Exception {
        var child = new LinkedHashMap<String, Object>();
        child.put("label", "<strong>literal</strong> ${untouched}");
        var children = new ArrayList<>(List.of(child));
        var builder =
                Letter.template(
                                """
                <letter><main><p>START</p><div data-each='groups' data-as='group'>
                <p>${group.name} ${flat.key}</p><p data-each='group.items' data-as='item'>${item.label}</p>
                <p>${group.name}</p></div><p>${item}</p></main></letter>
                """)
                        .values(
                                Map.of(
                                        "groups",
                                        List.of(Map.of("name", "GROUP", "items", children)),
                                        "flat.key",
                                        "FLAT",
                                        "item",
                                        "OUTER"));
        child.put("label", "MUTATED");
        children.clear();
        String actual = text(new LetterPdfGenerator().generate(builder.build()));
        assertTrue(actual.contains("GROUP FLAT"), actual);
        assertTrue(actual.contains("<strong>literal</strong> ${untouched}"), actual);
        assertTrue(actual.contains("OUTER"), actual);
        assertFalse(actual.contains("MUTATED"), actual);
        var table =
                Letter.template(
                                "<letter><main><p>Rows</p><table><tr data-each='rows' data-as='row'><td>${row}</td></tr></table></main></letter>")
                        .values(Map.of("rows", List.of("One", "Two")))
                        .build();
        String rows = text(new LetterPdfGenerator().generate(table));
        assertTrue(rows.indexOf("One") < rows.indexOf("Two"), rows);
    }

    @Test
    void conditionsSkipUnselectedMissingBindingsAndEmptyLists() throws Exception {
        var document =
                Letter.template(
                                """
                <letter><main><p>Kept</p><if flag='enabled'><p>${missing}</p><else><p>Alternative</p></else></if>
                <p data-if='enabled'>${alsoMissing}</p><p data-each='empty' data-as='item'>${item}</p></main></letter>
                """)
                        .values(Map.of("enabled", false, "empty", List.of()))
                        .build();
        String actual = text(new LetterPdfGenerator().generate(document));
        assertTrue(actual.contains("Alternative"), actual);
        assertFalse(actual.contains("missing"), actual);
        assertEquals(2, document.sections().get(0).blocks().size());
    }

    @Test
    void dottedKeysResolveAtRootAndInsideScopedMaps() throws Exception {
        var document =
                Letter.template(
                                "<letter><main><p>${a.b}</p><p data-each='rows' data-as='row'>${row.a.b}</p></main></letter>")
                        .values(
                                Map.of(
                                        "a.b",
                                        "FLAT",
                                        "a",
                                        Map.of("b", "NESTED"),
                                        "rows",
                                        List.of(Map.of("a.b", "INNER"))))
                        .build();
        String actual = text(new LetterPdfGenerator().generate(document));
        assertTrue(actual.contains("FLAT"), actual);
        assertTrue(actual.contains("INNER"), actual);
        assertFalse(actual.contains("NESTED"), actual);
    }

    @Test
    void missingPathsAndInvalidDirectiveTypesFailAtTheirLocations() {
        for (String fragment :
                List.of(
                        "<p>${person.name}</p>",
                        "<p data-if='flag'>Wrong</p>",
                        "<p data-each='rows' data-as='bad.alias'>Wrong</p>",
                        "<else><p>Orphan</p></else>")) {
            var failure =
                    assertThrows(
                            InvalidLetterException.class,
                            () ->
                                    Letter.template(
                                                    "<letter><main>"
                                                            + fragment
                                                            + "</main></letter>")
                                            .values(
                                                    Map.of(
                                                            "person",
                                                            Map.of(),
                                                            "flag",
                                                            "true",
                                                            "rows",
                                                            List.of("one")))
                                            .build());
            assertTrue(failure.fieldPath().startsWith("template"), failure.getMessage());
        }
    }
}
