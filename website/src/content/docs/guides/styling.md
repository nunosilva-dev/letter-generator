---
title: "Style any text or content block"
description: "Letter Generator usage: style any text or content block."
---

Global defaults:

```java
var letter = Letter.builder()
    .style(Style.builder().fontFamily("Times").fontSizePt(12)
        .color("#222222").lineHeight(1.25).spaceAfterMm(4).build())
    .body("This inherits the document style.")
    .build();
```

Formatted words and paragraphs:

```java
var letter = Letter.builder()
    .bodyHtml("""
        <p style="text-align:justify;font-size:11pt;margin-bottom:4mm">
          A <strong>bold word</strong>, an <em>italic phrase</em>, and
          <span style="color:#008000;text-decoration:underline">colored underlined text</span>.
        </p>
        """)
    .build();
```

Or construct typed runs for programmatic decisions:

```java
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

var paragraph = new Paragraph(List.of(
    new TextRun("Amount due: ", null, null),
    new TextRun("EUR 25.00", Style.builder().bold().color("#008000").build(), null),
    new TextRun(" — pay online", null, "https://example.com/pay")
), Style.builder().spaceAfterMm(4).build());

var letter = Letter.builder().block(paragraph).build();
```

Style inheritance is property-by-property: rendering defaults → document → section → block → run. Missing properties inherit. `Style.plain()` has no overrides. Explicit boolean values such as `.bold(false)` disable inherited emphasis. Region convenience styles are applied to their blocks, with explicit block styles taking precedence. A style does not change supplied text. `Style.overlay(other)` returns an immutable cascade where the later style wins. When named references remain unresolved, it retains ordered parent layers until generator preparation resolves them.

| Java property | Meaning |
| --- | --- |
| `fontFamily(String)` | Exact built-in or registered family name |
| `fontSizePt(double)` | Positive point size |
| `color(String)` | `#RRGGBB` |
| `bold()`, `italic()`, `underline()` | Enable emphasis; boolean overloads can disable it |
| `textAlign(TextAlign)` | `LEFT`, `CENTER`, `RIGHT`, `JUSTIFY` |
| `lineHeight(double)` | Positive multiplier |
| `spaceBeforeMm(double)`, `spaceAfterMm(double)` | Nonnegative block spacing |

Alignment and spacing belong to the containing block. Inline margin declarations are rejected. A large run can increase the paragraph's line box: the renderer uses the largest resolved run font-size×line-height throughout that paragraph. Headings are bold and 14 pt by default, before explicit overrides; HTML `h1`–`h6` supply sizes 18, 16, 14, 12, 11 and 10 pt.
