---
title: "Country-aware dates, numbers and currencies"
description: "Letter Generator usage: country-aware dates, numbers and currencies."
---

`LocaleSettings` makes formatting explicit and deterministic. Defaults are `en-GB`, localized `LONG` dates and `UTC`; the JVM's default locale and time zone are not consulted. Language selects formatting conventions, without translating application text.

```java
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.localization.ValueFormats;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.FormatStyle;

var british = new LocaleSettings("en-GB");
var american = new LocaleSettings("en-US");
var portuguese = new LocaleSettings("pt-PT", "dd/MM/uuuu", FormatStyle.LONG, "EUR", "Europe/Lisbon");

String ukDate = ValueFormats.format(LocalDate.of(2026, 10, 19), "date", null, british, null);
// 19 October 2026
String usDate = ValueFormats.format(LocalDate.of(2026, 10, 19), "date", null, american, null);
// October 19, 2026
String amount = ValueFormats.format(new BigDecimal("1234.50"), "currency", null, american, "USD");
// $1,234.50
```

The full constructor is `LocaleSettings(languageTag, datePattern, dateStyle, currencyCode, zoneId)`. Tags use BCP 47; currencies use ISO codes; zones use Java `ZoneId` names. A configured `datePattern` takes precedence over `dateStyle`. Currency defaults to the locale's country; supply a code when the language tag has no country or a different currency is required. Invalid tags, patterns, currencies and zones fail during configuration or formatting.

`ValueFormats.format(value, format, pattern, locale, currencyOverride)` accepts only `text`, `date`, `number` and `currency` formats. A null format means literal text. Date patterns use Java `DateTimeFormatter`; numeric patterns use `DecimalFormat`, with symbols from the selected locale. A per-call pattern overrides the configured date pattern. Numeric formatting requires an actual finite number; it does not parse a numeric string. Date formatting requires an appropriate temporal value; it does not parse a date string. `Instant` values resolve in the selected zone. Patterns requesting fields absent from a supplied temporal value fail. Text formatting does not accept pattern or currency options.

Use `.locale(LocaleSettings)` on either builder, or `.locale(Locale)` on `LetterBuilder`. Typed/JSON documents carry `settings.locale`:

```json
{
  "schemaVersion": 1,
  "settings": {"locale": {"languageTag": "pt-PT", "datePattern": "dd/MM/uuuu", "currencyCode": "EUR", "zoneId": "Europe/Lisbon"}},
  "sections": [{"name": "Body", "blocks": [
    {"type": "paragraph", "runs": [{"text": "Correspondência de exemplo"}]}
  ]}]
}
```

HTML root attributes are `language-tag`, `date-pattern`, `date-style` (`FULL`, `LONG`, `MEDIUM`, `SHORT`), `currency` and `zone-id`. Explicit root formatting options override the corresponding builder locale fields. Supply those locale attributes as literal configuration. The document's language also comes from `settings.locale.languageTag`; font coverage and direction remain separate document settings.
