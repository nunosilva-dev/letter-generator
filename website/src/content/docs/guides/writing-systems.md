---
title: "Writing direction, fallback fonts and hyphenation"
description: "Letter Generator usage: writing direction, fallback fonts and hyphenation."
---

Language/formatting and writing direction are separate settings. `.locale(new LocaleSettings("he-IL"))` selects country formatting and document language; `.direction(TextDirection.AUTO)` derives paragraph base direction from that language. `.direction(TextDirection.RTL)` or `.direction(TextDirection.LTR)` overrides it. Omitting direction retains the existing left-to-right behavior, even with a different formatting locale. Alignment remains an independent style property. FOP performs bidirectional ordering and shaping within its supported script/font capabilities; changing direction does not add missing glyphs.

Register fonts with all variants needed by the content, then explicitly opt into fallback:

```java
var generator = LetterPdfGenerator.builder()
    .registerClasspathFont("Unicode", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
    .build();
var letter = Letter.builder().fallbackFonts("Unicode").body("Amount Ω").build();
var result = generator.render(letter);
```

The selected font is tried first, followed by configured families in order. Selection works on complete Unicode grapheme clusters, including combining marks, rather than splitting a mark from its base. Authored `TextRun` boundaries remain shaping boundaries; keep a combining sequence within one run. Exact bold/italic faces are required; no synthetic faces or system-font scans occur. Missing family names and uncovered clusters fail with model paths. Successful replacements produce `FONT_SUBSTITUTION` diagnostics. Hyperlinks, styles, dynamic page fields, rich cells and nested containers remain intact. SVG text is part of the image renderer rather than ordinary letter text-run fallback.

Dictionary hyphenation requires explicitly supplied trusted XML patterns, with no runtime downloads:

```java
var generator = LetterPdfGenerator.builder()
    .hyphenationPatterns("en-GB", trustedPatternXmlBytes)
    .build();
var letter = Letter.builder().locale(new LocaleSettings("en-GB"))
    .hyphenate(true).body("Long explanatory content...").build();
```

Patterns use the native FOP hyphenation XML format without DTDs/entities/external resources. The document language tag must exactly match a registration; a different country variant is not silently substituted. `DocumentSettings(locale, metadata, direction, fallbackFonts, hyphenate)` is the shared typed/JSON settings model. HTML callers can supply it through `.settings(...)` on their builder, keeping formatting locale overrides consistent with `.locale(...)`.
