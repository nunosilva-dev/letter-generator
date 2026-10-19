package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.model.block.Paragraph;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

/** Prepared reuse, contracts and deterministic owned configuration checks. */
class PreparedTemplateTest {
    @Test
    void concurrentBindingsOwnTheirTreesAndPreserveCapturedConfiguration() throws Exception {
        var builder =
                Letter.template(
                                "<letter><main><p>${name} <param key='date' format='date'></p></main></letter>")
                        .locale(new LocaleSettings("en-US"))
                        .values(Map.of("name", "Original", "date", LocalDate.of(2026, 10, 19)));
        var prepared = builder.compile();
        builder.locale(new LocaleSettings("en-GB")).values(Map.of("name", "Changed"));
        var executor = Executors.newFixedThreadPool(4);
        try {
            var calls = new ArrayList<Callable<String>>();
            for (int index = 0; index < 12; index++) {
                final String name = "Bound" + index;
                calls.add(
                        () -> {
                            var document =
                                    prepared.bind(
                                            Map.of(
                                                    "name",
                                                    name,
                                                    "date",
                                                    LocalDate.of(2026, 10, 19)));
                            Paragraph paragraph =
                                    (Paragraph) document.sections().get(0).blocks().get(0);
                            return paragraph.runs().stream()
                                    .map(run -> run.text())
                                    .reduce("", String::concat);
                        });
            }
            var results = executor.invokeAll(calls);
            for (int index = 0; index < results.size(); index++) {
                assertEquals("Bound" + index + " October 19, 2026", results.get(index).get());
            }
        } finally {
            executor.shutdownNow();
        }
        assertTrue(
                text(new LetterPdfGenerator().generate(prepared.build()))
                        .contains("Original October 19, 2026"));
        assertTrue(prepared.validate().isValid());
    }

    @Test
    void requirementsKeepSourceOccurrencesAndConditionalContracts() {
        var contract =
                Letter.template(
                                "<letter><main><p style-name='notice'>${person.name} ${person.name}</p><if flag='show'><p data-each='rows' data-as='row'><param key='row.amount' format='number'></p></if><img src='asset:logo'><component ref='tail'></component></main></letter>")
                        .compile()
                        .requirements();
        assertEquals(
                List.of("person.name", "person.name", "row.amount"),
                contract.variables().stream().map(item -> item.name()).toList());
        assertEquals("show", contract.flags().get(0).name());
        assertEquals("rows", contract.collections().get(0).name());
        assertEquals("logo", contract.assets().get(0).name());
        assertEquals("notice", contract.styles().get(0).name());
        assertEquals("tail", contract.components().get(0).name());
        assertTrue(
                contract.variables().stream()
                        .allMatch(item -> item.path().startsWith("template.")));
        assertThrows(UnsupportedOperationException.class, () -> contract.flags().clear());
    }

    @Test
    void activeRepeatedMissingValuesAggregateAndSkippedBranchesDoNotDemandValues() {
        var prepared =
                Letter.template(
                                "<letter><main><p>Body</p><p data-each='rows' data-as='row'>${row.missing} ${row.missing}</p><p data-if='hidden'>${notNeeded}</p></main></letter>")
                        .values(Map.of("rows", List.of(Map.of(), Map.of()), "hidden", false))
                        .compile();
        var diagnostics = prepared.validate().diagnostics();
        assertEquals(
                4,
                diagnostics.stream()
                        .filter(item -> item.code().equals("MISSING_VARIABLE"))
                        .count());
        assertFalse(diagnostics.stream().anyMatch(item -> item.message().contains("notNeeded")));
    }

    @Test
    void dataSnapshotsRejectCyclesUnsupportedNumbersAndUnsupportedObjectsWithPaths() {
        var cyclic = new HashMap<String, Object>();
        cyclic.put("self", cyclic);
        var error =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                Letter.template("<letter><main><p>Body</p></main></letter>")
                                        .values(cyclic));
        assertTrue(error.getMessage().contains("values.self"));
        for (Object value : List.of(Double.NaN, Double.POSITIVE_INFINITY, new Object())) {
            var failure =
                    assertThrows(
                            IllegalArgumentException.class,
                            () ->
                                    Letter.template("<letter><main><p>Body</p></main></letter>")
                                            .values(Map.of("nested", Map.of("bad", value))));
            assertTrue(failure.getMessage().contains("values.nested.bad"));
        }
    }

    @Test
    void invalidLocaleAndFormatAndNonemptyParametersFailClearly() {
        assertThrows(IllegalArgumentException.class, () -> new LocaleSettings("not a tag"));
        for (String content :
                List.of(
                        "<param key='value' format='evaluate'>",
                        "<param key='value' format='date'>ignored</param>",
                        "<if flag='yes'><p>Selected</p><else><p>No</p></else><p>Lost</p></if>")) {
            assertThrows(
                    com.nsdev.lettergenerator.exception.InvalidLetterException.class,
                    () ->
                            Letter.template("<letter><main>" + content + "</main></letter>")
                                    .values(Map.of("value", "string", "yes", true))
                                    .build());
        }
    }
}
