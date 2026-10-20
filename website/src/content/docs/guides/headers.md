---
title: "Headers: left, center, right and exact placement"
description: "Letter Generator usage: headers: left, center, right and exact placement."
---

### Place an ordinary header

```java
var letter = Letter.builder()
    .header("Example Company\nCustomer Services")
    .headerAlignment(TextAlign.RIGHT)
    .headerWidthMm(80)
    .headerHeightMm(30)
    .body("Dear customer,\n\nYour request has been received.")
    .build();
```

Use `TextAlign.LEFT`, `CENTER` or `RIGHT`. `JUSTIFY` is not a placement option; justified paragraph text can instead use a block's style.

Alignment does two jobs: it positions a narrower header box inside the available horizontal space, and supplies the default alignment of its text/images. With A4 and 20 mm margins, the inner width is 170 mm. An 80 mm header box starts at x=0, 45 or 90 mm for left, center or right alignment. These x values are relative to the inner page, so the paper-edge starts are 20, 65 and 110 mm.

Omit `headerWidthMm` to use the full available width. For text, choosing right alignment is enough to place the text against the right edge. Specifying a width is useful for a multiline address block, a table, or a narrower letterhead region.

When a logo is present, automatic placement uses the space to the right of the logo plus its 5 mm gap. Center means the center of that remaining space, rather than the center of the sheet. Exact bounds bypass this automatic placement.

### Control text alignment independently

```java
var letter = Letter.builder()
    .header("Example Company\nCustomer Services")
    .headerAlignment(TextAlign.RIGHT)
    .headerWidthMm(80)
    .headerStyle(Style.builder().textAlign(TextAlign.LEFT).color("#123456").build())
    .body("Body")
    .build();
```

This places the box on the right and left-aligns its contents. `headerStyle` alone does not move the box. Explicit styles on individual header blocks override header defaults, so a header may contain a centered image and left-aligned address lines.

Calling `headerStyle` with only a color/font change preserves the alignment selected by `headerAlignment`. If the style explicitly specifies `textAlign`, that property controls the content. A later `headerAlignment` call sets the default content alignment again.

### Position a header exactly

```java
var letter = Letter.builder()
    .margins(new Margins(15, 18, 17, 22))
    .header("CLIENTE EXEMPLO\nMORADA FICTÍCIA, 00\n0000-000 LOCALIDADE")
    .headerBoundsMm(88, 23, 78, 28)
    .body("Dear Ana,\n\nYour request has been received.")
    .build();
```

`headerBoundsMm(x, y, width, height)` uses the inner page's upper-left corner. Positive x moves right; positive y moves down. The example's address box starts **110 mm from the left paper edge** (22+88) and **38 mm from the top** (15+23), with width 78 and height 28 mm.

To translate a measurement from the paper edge:

```text
header x = requested paper-edge x - left margin
header y = requested paper-edge y - top margin
```

Choose margins that permit nonnegative x/y. Content cannot extend outside the inner page. The supplied coordinates identify the **box**, not the baseline or top of visible glyph ink; font metrics and paragraph margins influence ink placement inside it.

The body begins after the greater of the header box's bottom and logo's bottom, followed by a 5 mm gap. In this example, the body region starts 71 mm from the top paper edge: 15 + 23 + 28 + 5. There is no need to insert blank body paragraphs to make room for the address.

Exact bounds retain their x/y when `headerAlignment` changes. Subsequent `headerWidthMm` or `headerHeightMm` calls resize the exact box without moving its origin. Set bounds again to change the origin. Geometry is resolved during `build()`, so page/margin configuration can appear before or after content calls. Previously built documents remain unchanged.

A header box must fit horizontally and leave positive body height. Overlapping logo/fixed boxes are rejected during generation. Text/images that do not fit the box fail during generation instead of being clipped.

### Use images, tables or formatted header content

```java
var letter = Letter.builder()
    .headerHtml("""
        <p style="font-size:9pt;color:#345678">CUSTOMER SERVICES</p>
        <img src="asset:brand" width="45mm" height="12mm">
        """)
    .headerAlignment(TextAlign.RIGHT)
    .headerWidthMm(80)
    .headerHeightMm(30)
    .asset("brand", logoBytes)
    .body("Body")
    .build();
```

For an image-only header use `.header(imageBytes)` or `.header(imagePath)`. It fits the image to the final header box, retaining aspect ratio. Rich header image percentages also resolve against the final box.

### Template equivalent

```html
<header align="right" width-mm="80" height-mm="30">
  <p>Example Company<br>Customer Services</p>
</header>
```

For an exact box, supply all four dimensions:

```html
<header x-mm="88" y-mm="23" width-mm="78" height-mm="28" align="left">
  <p>${customerName}<br>${addressLine1}<br>${postalAddress}</p>
</header>
```

`align` is optional with exact bounds and affects content alignment there. Values use millimeters without a unit suffix. Each attribute can contain a `${key}` placeholder. Supplying only x or y, or an incomplete exact rectangle, is an error. Header-local `height-mm` overrides root `header-height-mm`. CSS `text-align` controls content only; it can override the default supplied by `align`.

The fluent header repeats on every page. For a first-page address block and a different following-page header, use [advanced fixed elements](/guides/fixed-regions/).

### First-page and continuation stationery

```java
import com.nsdev.lettergenerator.model.layout.PageSelector;

var letter = Letter.builder()
    .header("Example Company")                         // ALL fallback
    .header(PageSelector.FIRST, "Example Company\nFull postal address")
    .header(PageSelector.FOLLOWING, "Example Company | Continued")
    .footerHtml("<p>Page <page-number></page-number> of <page-count></page-count></p>")
    .body("First page", Letter.PAGE_BREAK, "Second page")
    .build();
```

All header/footer content methods accept a leading `PageSelector`: text, HTML, typed blocks and image bytes/paths. Specific content replaces the ALL fallback on selected pages. `headerBlocks(PageSelector.FOLLOWING)` supplies an explicit empty continuation header. Styles can vary with `headerStyle(selector, style)` and `footerStyle(selector, style)`.

Template equivalents:

```html
<header><p>Default company header</p></header>
<header pages="first"><p>Company and full address</p></header>
<header pages="following"><p>Continued</p></header>
<footer><p>Page <page-number></page-number> of <page-count></page-count></p></footer>
```

Duplicate headers/footers for the same selector are errors. FIRST and FOLLOWING are disjoint; ALL is fallback content. Geometry is shared: header position, width and height, and footer height apply to every variant. The final geometry configuration wins, while variant styles/content remain independent. The body reserves those shared heights on all pages, even where a variant is empty. Use advanced `FixedElement` values for independent rectangles within the same reserved zones. A single-page letter uses FIRST content.
