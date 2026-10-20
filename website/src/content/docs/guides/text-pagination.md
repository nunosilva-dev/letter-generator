---
title: "Paragraphs, line changes and page breaks"
description: "Letter Generator usage: paragraphs, line changes and page breaks."
---

```java
var letter = Letter.builder()
    .body("Dear Ana,\n\nFirst paragraph.\nSecond line of the same paragraph.")
    .body("A new paragraph from a new body argument.", "Another paragraph.")
    .pageBreak()
    .body("Page two")
    .build();
```

Plain text rules:

| Input | Result |
| --- | --- |
| Single `\n` | New line in the same paragraph |
| Blank line (`\n\n`) | New paragraph |
| A separate string argument | Separate paragraph |
| CRLF or CR | Normalized to newline |
| `Letter.PAGE_BREAK` or `\f` inside body text | New page |
| `.pageBreak()` | New page between body blocks |

Consecutive, leading and trailing explicit page breaks are preserved. That can intentionally create blank pages. Page breaks are valid only in body content, not headers, footers or signatures.

In HTML, ordinary whitespace/newlines collapse. Use `<p>` for paragraphs, `<br>` for a line change, and paired `<page-break></page-break>` for a page change. Do not use `<page-break/>`; it is a custom HTML element, not a void tag. A newline in a replacement value follows these HTML whitespace rules. For a multiline address, put `<br>` between separate placeholders, or use the plain Java header method.

Content appends in call order: `body(...).table(...).bodyHtml(...).body(...)`. Header, footer and signature content calls replace the prior content of that region. To combine several items in a region, use one rich fragment, one block array, or one signature call with multiple parts.

### Deliberate pagination controls

```java
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.block.GroupBlock;

var policy = Style.builder()
    .keepTogether(Keep.PREFER)
    .keepWithNext(Keep.AUTO)
    .widows(3).orphans(3)
    .build();

var together = new GroupBlock(List.of(firstBlock, secondBlock),
    Style.builder().keepTogether(Keep.ALWAYS).build());
var letter = Letter.builder().block(together).group("Address line 1\nAddress line 2").build();
```

`AUTO` allows splitting, `PREFER` requests a breakable keep, and `ALWAYS` requests a strong keep. The no-argument style methods `keepTogether()` and `keepWithNext()` select ALWAYS. Headings ordinarily keep with their successor; an explicit `keepWithNext(Keep.AUTO)` disables that default. `keepWithNext` links a block's end to the next block's beginning; use a kept group, or keep the successor together, when its whole content must move with it. Oversized strong keeps can cause layout rejection; the library does not shrink them.

Groups inherit typography into their children, apply margins around the group, and keep their pagination controls on the group itself. Their ordinary policy is PREFER. `group(Block...)` and `group(String...)` are fluent shortcuts. Explicit AUTO groups and ordinary grouping divs in body flow may contain page breaks; kept groups, signatures, fixed content and table cells cannot.

```html
<group style="keep-together:always">
  <h2>Account details</h2><p>These details should travel with the heading.</p>
</group>
<p style="keep-with-next:always">Please review the following:</p>
<p style="keep-together:prefer;widows:3;orphans:3">A naturally wrapped paragraph...</p>
```

Widows are the minimum trailing lines and orphans the minimum opening lines of a naturally wrapped paragraph. Counts must be positive integers. FOP treats forced newlines and `br` as separate layout paragraphs, so widow/orphan counts do not span those forced boundaries. Use a kept group or `keep-together` for content with explicit line breaks. These are native layout controls, subject to available space and FOP's supported behavior.

For typed page fields, use `TextRun.currentPage()` and `TextRun.pageCount()` among ordinary runs. In JSON, set `"text":""` and `"pageNumber":"CURRENT"` or `"TOTAL"`. Totals include an automatically recovered final signature page. Page fields work in any text run, including headers, footers and cells.
