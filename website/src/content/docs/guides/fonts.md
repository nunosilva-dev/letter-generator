---
title: "Fonts: defaults, embedding and Unicode"
description: "Letter Generator usage: fonts: defaults, embedding and unicode."
---

### Defaults require no files

`new LetterPdfGenerator()` uses the engine's standard Helvetica, Times and Courier families, including regular, bold, italic and bold italic. Helvetica is the default document font. These standard fonts require no operating-system font lookup or application registration. They are **not embedded TrueType resources bundled in this library's JAR**; PDF viewers provide standard-font display behavior.

The DejaVu files under this repository's `src/test/resources/fonts` are licensed test/example fixtures. Their presence does not make DejaVu a production default. The energy examples explicitly register their `ReferenceSans` alias to those fixtures.

### Register fonts for embedding or wider character coverage

Package fonts in your application's resources:

```text
src/main/resources/fonts/CompanySans-Regular.ttf
src/main/resources/fonts/CompanySans-Bold.ttf
src/main/resources/fonts/CompanySans-Italic.ttf
src/main/resources/fonts/CompanySans-BoldItalic.ttf
```

```java
import com.nsdev.lettergenerator.font.FontVariant;

var generator = LetterPdfGenerator.builder()
    .registerClasspathFont("CompanySans", FontVariant.REGULAR, "fonts/CompanySans-Regular.ttf")
    .registerClasspathFont("CompanySans", FontVariant.BOLD, "fonts/CompanySans-Bold.ttf")
    .registerClasspathFont("CompanySans", FontVariant.ITALIC, "fonts/CompanySans-Italic.ttf")
    .registerClasspathFont("CompanySans", FontVariant.BOLD_ITALIC, "fonts/CompanySans-BoldItalic.ttf")
    .build();

var letter = Letter.builder()
    .style(Style.builder().fontFamily("CompanySans").build())
    .body("Text in the registered font")
    .build();
```

Use `registerFont(family, variant, Path)` for trusted filesystem fonts. Font bytes load and validate when the generator is built and remain available if the original file is later removed. Configure the generator once, then reuse it.

Supply a regular face for every family and each style variant you use. Headings/table headers commonly request bold; italic signatures need italic; combined emphasis needs bold italic. Missing variants fail rather than being synthesized. Unknown families and missing glyphs fail. A custom font is not a guarantee of coverage for every script or shaping requirement; verify actual target-language output.

Fonts must be `.ttf` with TrueType outlines. Built-in family names cannot be replaced. Family aliases are exact application identifiers, such as `CompanySans`. The application is responsible for font licensing and packaging. Standard fonts have limited character coverage; use an appropriate registered font when your target text falls outside it.
