package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.letter.Address;
import com.nsdev.lettergenerator.letter.Party;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Conventional semantic ordering, final locale and input validation. */
class SemanticLetterTest {
    @Test
    void fluentPartsUseConventionalOrderRegardlessOfCallOrderAndFinalLocale() throws Exception {
        var document =
                Letter.builder()
                        .body("BODY")
                        .closing("CLOSING")
                        .subject("SUBJECT")
                        .recipient(Party.of("RECIPIENT", Address.of("Recipient street")))
                        .salutation("SALUTATION")
                        .reference("Account", "EX-1001")
                        .date(LocalDate.of(2026, 10, 19))
                        .sender(Party.company("SENDER", Address.of("Sender street")))
                        .locale(new LocaleSettings("en-US"))
                        .build();
        String actual = text(new LetterPdfGenerator().generate(document));
        List<String> order =
                List.of(
                        "SENDER",
                        "October 19, 2026",
                        "RECIPIENT",
                        "Account: EX-1001",
                        "SUBJECT",
                        "SALUTATION",
                        "BODY",
                        "CLOSING");
        for (int index = 1; index < order.size(); index++) {
            assertTrue(
                    actual.indexOf(order.get(index - 1)) < actual.indexOf(order.get(index)),
                    actual);
        }
    }

    @Test
    void htmlDatesAndRichReferencesRespectLocaleAndLiteralLabels() throws Exception {
        var document =
                Letter.fromHtml(
                                "<letter language-tag='en-GB'><date value='2026-10-19'></date><reference label='Account &amp; code'><p><em>EX-1001</em></p></reference><main><p>Body</p></main></letter>")
                        .build();
        String actual = text(new LetterPdfGenerator().generate(document));
        assertTrue(actual.contains("19 October 2026"), actual);
        assertTrue(actual.contains("Account & code: EX-1001"), actual);
    }

    @Test
    void malformedSemanticPartiesAndBlankContentFailDuringAuthoring() {
        for (Party party :
                List.of(
                        new Party(null, null, null),
                        Party.of(" ", null),
                        Party.of("Name", new Address(List.of(""))),
                        Party.of("Name", new Address(null)))) {
            assertThrows(IllegalArgumentException.class, () -> Letter.builder().sender(party));
        }
        assertThrows(IllegalArgumentException.class, () -> Letter.builder().subject(" "));
    }
}
