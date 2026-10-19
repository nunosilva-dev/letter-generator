package com.nsdev.lettergenerator.localization;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Currency;
import java.util.Locale;

/** Finite text/date/number/currency formatting without reflection or global lookups. */
public final class ValueFormats {
    /** Prevents construction. */
    private ValueFormats() {}

    /**
     * Formats one scalar using explicit locale configuration and optional exact pattern.
     *
     * @param value non-null scalar or supported java.time value
     * @param format text, date, number or currency; null means text
     * @param pattern optional date/DecimalFormat pattern
     * @param settings optional locale; null means stable defaults
     * @param currency optional ISO currency override
     * @return formatted literal text
     */
    public static String format(
            Object value, String format, String pattern, LocaleSettings settings, String currency) {
        if (value == null) {
            throw new IllegalArgumentException("Formatted value must not be null");
        }
        if (pattern != null && pattern.isBlank()) {
            throw new IllegalArgumentException("Formatting pattern must not be blank");
        }
        LocaleSettings effective = settings == null ? LocaleSettings.defaults() : settings;
        String kind = format == null ? "text" : format.toLowerCase(Locale.ROOT);
        return switch (kind) {
            case "text" -> text(value, pattern, currency);
            case "date" -> date(value, pattern, effective, currency);
            case "number", "currency" -> number(value, kind, pattern, effective, currency);
            default -> throw new IllegalArgumentException("Unsupported value format: " + format);
        };
    }

    /**
     * Formats scalar text without accepting containers or unused options.
     *
     * @param value scalar
     * @param pattern forbidden text pattern
     * @param currency forbidden text currency
     * @return literal scalar text
     */
    private static String text(Object value, String pattern, String currency) {
        if (pattern != null
                || currency != null
                || !(value instanceof CharSequence
                        || value instanceof Number
                        || value instanceof Boolean
                        || value instanceof Character
                        || value instanceof TemporalAccessor)) {
            throw new IllegalArgumentException(
                    "Text formatting requires a scalar and no pattern/currency");
        }
        return value.toString();
    }

    /**
     * Formats a temporal value, resolving instant time zones explicitly.
     *
     * @param value temporal value
     * @param pattern optional override
     * @param settings explicit locale
     * @param currency forbidden date currency
     * @return localized date
     */
    private static String date(
            Object value, String pattern, LocaleSettings settings, String currency) {
        if (currency != null || !(value instanceof TemporalAccessor temporal)) {
            throw new IllegalArgumentException(
                    "Date formatting requires a temporal value and no currency");
        }
        String selected = pattern == null ? settings.datePattern() : pattern;
        DateTimeFormatter formatter =
                selected == null
                        ? DateTimeFormatter.ofLocalizedDate(settings.dateStyle())
                                .withLocale(settings.locale())
                        : DateTimeFormatter.ofPattern(selected, settings.locale());
        formatter = formatter.withZone(ZoneId.of(settings.zoneId()));
        return formatter.format(
                value instanceof Instant instant
                        ? instant.atZone(ZoneId.of(settings.zoneId()))
                        : temporal);
    }

    /**
     * Formats finite numeric values with a call-owned formatter.
     *
     * @param value number
     * @param kind number or currency
     * @param pattern optional exact pattern
     * @param settings explicit locale
     * @param currency optional ISO override
     * @return localized number
     */
    private static String number(
            Object value, String kind, String pattern, LocaleSettings settings, String currency) {
        if (!(value instanceof Number number)
                || value instanceof Double && !Double.isFinite(number.doubleValue())
                || value instanceof Float && !Float.isFinite(number.floatValue())) {
            throw new IllegalArgumentException("Numeric formatting requires a finite number");
        }
        boolean monetary = kind.equals("currency");
        if (!monetary && currency != null) {
            throw new IllegalArgumentException("Currency override requires currency formatting");
        }
        NumberFormat formatter =
                pattern != null
                        ? new DecimalFormat(
                                pattern, DecimalFormatSymbols.getInstance(settings.locale()))
                        : monetary
                                ? NumberFormat.getCurrencyInstance(settings.locale())
                                : NumberFormat.getNumberInstance(settings.locale());
        if (monetary) {
            String code = currency == null ? settings.currencyCode() : currency;
            Currency selected =
                    code == null
                            ? Currency.getInstance(settings.locale())
                            : Currency.getInstance(code);
            formatter.setCurrency(selected);
            int digits = selected.getDefaultFractionDigits();
            if (pattern == null && digits >= 0) {
                formatter.setMinimumFractionDigits(digits);
                formatter.setMaximumFractionDigits(digits);
            }
        }
        return formatter.format(value);
    }
}
