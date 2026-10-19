package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.renderFirst;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Executable semantic, localized and data-driven letter with a first-page preview. */
class V1AuthoringExampleTest {
    @Test
    void preparedAuthoringExampleProducesIndependentLocalizedLetters() throws Exception {
        String source =
                """
                <letter language-tag="en-GB" currency="EUR" style="font-size:10pt;line-height:1.25">
                  <sender style="color:#123B4A">
                    <p><strong>NORTH COAST SERVICES</strong><br>18 Harbour Road<br>Example City EX1 2AB</p>
                  </sender>
                  <date value="${issuedIso}"></date>
                  <recipient><p><strong>${customer.name}</strong><br>${customer.street}<br>${customer.city}</p></recipient>
                  <reference label="Customer reference">${customer.reference}</reference>
                  <subject>Service renewal confirmation</subject>
                  <salutation>Dear ${customer.name},</salutation>
                  <main>
                    <p>Your renewed service starts on <strong><param key="start" format="date"></strong>.
                    The following monthly charges apply.</p>
                    <table column-weights="3,1,1">
                      <thead style="background-color:#123B4A;color:white">
                        <tr><th>Service</th><th>Quantity</th><th>Monthly charge</th></tr>
                      </thead>
                      <tbody>
                        <tr data-each="items" data-as="item">
                          <td>${item.description}</td><td><param key="item.quantity" format="number"></td>
                          <td><param key="item.amount" format="currency"></td>
                        </tr>
                      </tbody>
                    </table>
                    <p style="margin-top:4mm"><strong>Monthly total: <param key="total" format="currency"></strong></p>
                    <if flag="includeCredit">
                      <p style="color:#175943">A welcome credit of <param key="credit" format="currency"> will appear on your first statement.</p>
                      <else><p>Your account continues on its existing payment schedule.</p></else>
                    </if>
                    <p data-if="showContact">Questions? Email <a href="mailto:care@example.invalid">care@example.invalid</a>.</p>
                  </main>
                  <closing><p>Kind regards,<br><strong>The North Coast team</strong></p></closing>
                  <footer style="font-size:8pt;color:#556677"><p>Example correspondence | Page <page-number></page-number> of <page-count></page-count></p></footer>
                </letter>
                """;
        Map<String, Object> values =
                Map.of(
                        "issuedIso",
                        "2026-10-19",
                        "start",
                        LocalDate.of(2026, 11, 1),
                        "customer",
                        Map.of(
                                "name",
                                "Alex Example",
                                "street",
                                "24 Sample Lane",
                                "city",
                                "Example Town EX2 3CD",
                                "reference",
                                "NC-1001"),
                        "items",
                        List.of(
                                Map.of(
                                        "description",
                                        "Home service",
                                        "quantity",
                                        1,
                                        "amount",
                                        new BigDecimal("24.50")),
                                Map.of(
                                        "description",
                                        "Additional support",
                                        "quantity",
                                        1,
                                        "amount",
                                        new BigDecimal("5.25")),
                                Map.of(
                                        "description",
                                        "Paperless account",
                                        "quantity",
                                        1,
                                        "amount",
                                        BigDecimal.ZERO)),
                        "total",
                        new BigDecimal("29.75"),
                        "includeCredit",
                        true,
                        "credit",
                        new BigDecimal("10.00"),
                        "showContact",
                        true);
        var prepared = Letter.template(source).values(values).compile();
        var generator = new LetterPdfGenerator();
        assertTrue(prepared.validate(generator).isValid());
        assertEquals("items", prepared.requirements().collections().get(0).name());
        byte[] primary = generator.generate(prepared.build());
        String primaryText = text(primary);
        assertTrue(primaryText.contains("19 October 2026"), primaryText);
        assertTrue(primaryText.contains("1 November 2026"), primaryText);
        assertTrue(primaryText.contains("Additional support"), primaryText);
        assertTrue(primaryText.contains("29.75"), primaryText);
        assertTrue(primaryText.contains("welcome credit"), primaryText);
        assertTrue(primaryText.indexOf("Alex Example") < primaryText.indexOf("Service renewal"));
        assertTrue(primaryText.indexOf("Kind regards") > primaryText.indexOf("Monthly total"));
        var secondaryValues = new LinkedHashMap<String, Object>(values);
        secondaryValues.put(
                "customer",
                Map.of(
                        "name",
                        "Sam Example",
                        "street",
                        "36 Demo Street",
                        "city",
                        "Example Town EX3 4EF",
                        "reference",
                        "NC-1002"));
        secondaryValues.put("includeCredit", false);
        secondaryValues.put("showContact", false);
        secondaryValues.remove("credit");
        String secondaryText = text(generator.generate(prepared.bind(secondaryValues)));
        assertTrue(secondaryText.contains("Sam Example"), secondaryText);
        assertTrue(secondaryText.contains("existing payment schedule"), secondaryText);
        assertFalse(secondaryText.contains("welcome credit"), secondaryText);
        assertFalse(secondaryText.contains("Questions?"), secondaryText);
        assertTrue(text(generator.generate(prepared.build())).contains("Alex Example"));
        Path output = Path.of("target/examples");
        Files.createDirectories(output);
        Files.write(output.resolve("v1-authoring.pdf"), primary);
        renderFirst(primary, "v1-authoring");
    }
}
