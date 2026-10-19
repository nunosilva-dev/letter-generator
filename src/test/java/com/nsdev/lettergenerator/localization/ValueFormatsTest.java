package com.nsdev.lettergenerator.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import org.junit.jupiter.api.Test;

/** Country and instant formatting independent of process defaults. */
class ValueFormatsTest {
    @Test
    void explicitCountriesAndPatternsControlFormatting() {
        assertEquals(
                "19 October 2026",
                ValueFormats.format(
                        LocalDate.of(2026, 10, 19),
                        "date",
                        null,
                        new LocaleSettings("en-GB"),
                        null));
        assertEquals(
                "October 19, 2026",
                ValueFormats.format(
                        LocalDate.of(2026, 10, 19),
                        "date",
                        null,
                        new LocaleSettings("en-US"),
                        null));
        assertEquals(
                "19/10/2026",
                ValueFormats.format(
                        LocalDate.of(2026, 10, 19),
                        "date",
                        "dd/MM/uuuu",
                        new LocaleSettings("pt-PT"),
                        null));
        assertEquals(
                "1.234,50",
                ValueFormats.format(
                        new BigDecimal("1234.5"),
                        "number",
                        "#,##0.00",
                        new LocaleSettings("de-DE"),
                        null));
        assertEquals(
                "$1,234.50",
                ValueFormats.format(
                        new BigDecimal("1234.5"),
                        "currency",
                        null,
                        new LocaleSettings("en-US"),
                        "USD"));
    }

    @Test
    void instantsUseExplicitZoneAndDefaultLocaleRemainsStable() {
        assertEquals(
                "18 October 2026",
                ValueFormats.format(
                        Instant.parse("2026-10-19T00:30:00Z"),
                        "date",
                        null,
                        new LocaleSettings(
                                "en-GB", null, FormatStyle.LONG, null, "America/New_York"),
                        null));
        assertEquals(
                "19 October 2026",
                ValueFormats.format(
                        Instant.parse("2026-10-19T00:30:00Z"), "date", null, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> ValueFormats.format(Double.NaN, "number", null, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> ValueFormats.format(123, "text", "00", null, null));
    }
}
