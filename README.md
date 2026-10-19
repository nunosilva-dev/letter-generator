# Letter Generator

A framework-free Java 17 library that turns Java objects, HTML templates or JSON into PDF bytes. Apache FOP composes and paginates the letter; PDFBox applies optional watermarks and debug overlays after successful layout. Supply content, styles and assets to generate complete PDF bytes.

This README is the complete usage guide for `com.nsdev:letter-generator:1.0.0-SNAPSHOT`.

Follow the guide for a first integration, or jump to a task:

- [1. Install and build](#1-install-and-build)
- [2. Generate your first PDF without font configuration](#2-generate-your-first-pdf-without-font-configuration)
- [3. Choose an input API](#3-choose-an-input-api)
- [4. Paper, margins, units and layout](#4-paper-margins-units-and-layout)
- [5. Headers: left, center, right and exact placement](#5-headers-left-center-right-and-exact-placement)
- [6. Footers and logos](#6-footers-and-logos)
- [7. Paragraphs, line changes and page breaks](#7-paragraphs-line-changes-and-page-breaks)
- [8. Style any text or content block](#8-style-any-text-or-content-block)
- [9. Tables and lists](#9-tables-and-lists)
- [10. Images and named assets](#10-images-and-named-assets)
- [11. Signatures](#11-signatures)
- [12. HTML templates and placeholders](#12-html-templates-and-placeholders)
- [13. Fonts: defaults, embedding and Unicode](#13-fonts-defaults-embedding-and-unicode)
- [14. Country and stationery variations](#14-country-and-stationery-variations)
- [15. Watermarks](#15-watermarks)
- [16. Advanced fixed elements and last-page signatures](#16-advanced-fixed-elements-and-last-page-signatures)
- [17. JSON input](#17-json-input)
- [18. Application integration and concurrency](#18-application-integration-and-concurrency)
- [19. Errors and troubleshooting](#19-errors-and-troubleshooting)
- [20. Run the examples and inspect output](#20-run-the-examples-and-inspect-output)
- [21. Public API reference](#21-public-api-reference)
- [22. Supported scope and limits](#22-supported-scope-and-limits)
- [23. Development and code quality](#23-development-and-code-quality)
- [24. Reusable themes and components](#24-reusable-themes-and-components)
- [25. Validate, diagnose and preview layout](#25-validate-diagnose-and-preview-layout)
- [26. Semantic letter parts and address layouts](#26-semantic-letter-parts-and-address-layouts)
- [27. Country-aware dates, numbers and currencies](#27-country-aware-dates-numbers-and-currencies)
- [28. Typed data, conditions and repeated rows](#28-typed-data-conditions-and-repeated-rows)
- [29. Prepare templates once and inspect their contracts](#29-prepare-templates-once-and-inspect-their-contracts)
- [30. Company stationery and table appearance](#30-company-stationery-and-table-appearance)
- [31. Canvases, columns, boxes and custom paper](#31-canvases-columns-boxes-and-custom-paper)
- [32. Writing direction, fallback fonts and hyphenation](#32-writing-direction-fallback-fonts-and-hyphenation)
- [33. Metadata, tagged output and PDF/A](#33-metadata-tagged-output-and-pdfa)
- [34. Append existing PDFs](#34-append-existing-pdfs)

## 1. Install and build

Use JDK 17 or a compatible newer JDK. The artifact targets Java 17; project acceptance runs use JDK 17. The checked-in Maven Wrapper manages Maven, so a global Maven installation is optional.

The runtime needs Java's `java.desktop` module for image and font support, including on a headless server. Keep that module if you assemble a reduced runtime with `jlink`.

From this repository:

```sh
./mvnw clean install
```

This runs formatting checks, compilation with warnings treated as errors, integration tests and Checkstyle, builds the binary/sources JARs, and installs the artifact into your local Maven repository. An initial build needs access to Maven repositories to download dependencies.

In the consuming application's `pom.xml`:

```xml
<dependency>
  <groupId>com.nsdev</groupId>
  <artifactId>letter-generator</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The build produces `target/letter-generator-1.0.0-SNAPSHOT.jar` and its sources JAR. Maven Wrapper downloads Maven 3.9.11 on first use.

Dependencies are transitive: Apache FOP Core **2.11**, Jackson Databind **2.22.3**, Apache PDFBox **3.0.8**, jsoup **1.23.2**, and Apache Commons Text **1.15.0**.

The library is a normal JAR with transitive dependencies. Copying only its JAR to a classpath is insufficient; Maven/Gradle or your application packaging must also include its runtime dependencies. There is no Spring, servlet, database or web-service requirement. `SNAPSHOT` is the current development version; publish it to your organization's artifact repository when another machine must consume it.

If this development machine selects a different Java installation, select the installed Java 17 explicitly:

```sh
export JAVA_HOME="$HOME/.local/share/letter-pdf-tools/jdk-17.0.20.1+1"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw clean install
```

## 2. Generate your first PDF without font configuration

Save this as `FirstLetter.java` in your application:

```java
import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FirstLetter {
    public static void main(String[] args) throws Exception {
        var generator = new LetterPdfGenerator();
        var letter = Letter.builder()
            .header("Example Company\nCustomer Services")
            .heading("Confirmation")
            .body("Dear Ana,\n\nYour request has been received.",
                  "Please keep this letter for your records.")
            .footer("Example Company | example.com")
            .build();

        byte[] pdf = generator.generate(letter);
        Files.write(Path.of("letter.pdf"), pdf);
    }
}
```

The output is an A4 PDF with 20 mm margins and built-in Helvetica at 11 pt. Body paragraphs have a 3 mm gap and a 1.2 line-height multiplier. Header and footer regions reserve space automatically.

Both of these generator constructions require **no font files**:

```java
var generator = new LetterPdfGenerator();
var equivalent = LetterPdfGenerator.builder().build();
```

The constructor using an internal `FontRegistry` is private because applications do not configure internal registry objects. Use the public no-argument constructor for defaults, or the public builder for optional custom font registration. Built-in PDF fonts are distinct from embedded TrueType files; [section 13](#13-fonts-defaults-embedding-and-unicode) explains the difference.

## 3. Choose an input API

All input paths produce the same immutable document model and use the same validation/rendering engine.

| Situation | Entry point | Why |
| --- | --- | --- |
| Application assembles ordinary letter content | `Letter.builder()` | Content-first methods and layout defaults |
| A letter template contains `${key}` placeholders | `Letter.template(markup).values(map)` | Integrated literal-value substitution and named assets |
| Application already produced resolved markup | `Letter.fromHtml(markup)` | Explicitly performs no substitution |
| Most content is plain Java, some is formatted markup | `bodyHtml`, `headerHtml`, `footerHtml`, `signatureHtml` | Mix formatted fragments into fluent construction |
| Exact page selectors or multiple fixed boxes | Immutable model records | Full geometry contract |
| An application boundary exchanges structured documents | `generator.generateJson(json)` | Strict versioned JSON model |

`Letter.template(...)` and `Letter.fromHtml(...)` both return `HtmlLetterBuilder` from `com.nsdev.lettergenerator.builder`, alongside `LetterBuilder` and `TableBuilder`.

Plain `.body("<strong>Hello</strong>")` prints those characters literally. To interpret supported markup, use `.bodyHtml(...)` or a template API.

## 4. Paper, margins, units and layout

Useful imports for geometry/style examples:

```java
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
```

| Setting | Default | Customization |
| --- | --- | --- |
| Paper | `PageSize.A4`, 210 × 297 mm | `page(PageSize.LETTER)`, 215.9 × 279.4 mm |
| Margins | 20 mm on each side | `marginsMm(15)` or `margins(new Margins(top, right, bottom, left))` |
| Header content height | 25 mm when present | `headerHeightMm(...)` or exact header bounds |
| Footer content height | 15 mm when present | `footerHeightMm(...)` |
| Body/header and body/footer separation | 5 mm | Advanced `PageSpec` for complete zone control |
| Body paragraph spacing | 3 mm after paragraphs | `style(Style.builder().spaceAfterMm(...).build())` |
| Text size | 11 pt | `fontSizePt(...)` |

`Margins` constructor order follows top, right, bottom, left:

```java
var letter = Letter.builder()
    .page(PageSize.LETTER)
    .margins(new Margins(15, 18, 17, 22))
    .body("Custom stationery")
    .build();
```

Page/box dimensions and spacing use **millimeters**. Font sizes use **points**. A line-height value is a **multiplier**, so 11 pt with 1.2 gives a 13.2 pt line box. Rotation uses degrees and opacity uses a value from 0 to 1.

The margins form the inner page. Header/logo content occupies a reserved top zone, footer content occupies a bottom zone, and the body flows between them. The library reserves each zone's full width; text does not wrap beside fixed elements. Empty fluent header/footer regions reserve no space unless advanced fixed elements use those zones.

`PageSpec.A4` and `PageSpec.LETTER` are immutable advanced-model presets with default margins and zero reserved zones. `Letter.builder()` creates the zones needed by its header/footer calls. This difference matters when constructing `LetterDocument` directly.

## 5. Headers: left, center, right and exact placement

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

The fluent header repeats on every page. For a first-page address block and a different following-page header, use [advanced fixed elements](#16-advanced-fixed-elements-and-last-page-signatures).

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

## 6. Footers and logos

Plain footers:

```java
var letter = Letter.builder()
    .body("Body")
    .footer("Example Company | example.com\nRegistered office: Lisboa")
    .footerStyle(Style.builder().fontSizePt(8).color("#666666")
        .textAlign(TextAlign.CENTER).build())
    .footerHeightMm(18)
    .build();
```

Default footer text uses 9 pt and `#666666`. Header/footer paragraphs default to zero paragraph spacing. Footer content repeats on every page. Increasing footer height reduces the available body area and preserves the 5 mm gap above the footer.

An image is valid footer content:

```java
var letter = Letter.builder().body("Body").footer(footerBytes).build();
```

Use a rich fragment to combine images and text:

```java
var letter = Letter.builder()
    .body("Body")
    .footerHtml("""
        <img src="asset:banner" width="100%" height="6mm">
        <p style="font-size:7pt;text-align:center;margin-top:1mm">${literal}</p>
        """)
    .asset("banner", footerBytes)
    .footerHeightMm(15)
    .build();
```

This fragment does not substitute `${literal}`; it prints that text. Use the full template API when replacements are needed. A footer may also contain a table or explicit immutable blocks through `footerBlocks(...)`.

`.logo(Path/byte[])` fits a PNG/JPEG/static SVG within 45×20 mm and places it at the top-left of the inner page, beside the automatic header. `.logo(ImageBlock)` supplies explicit image dimensions/style. The separate logo region repeats on every page. To place a brand image within an exact header rather than beside it, include that image in header content and omit the separate logo call.

## 7. Paragraphs, line changes and page breaks

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

## 8. Style any text or content block

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

## 9. Tables and lists

### Fluent table

```java
import com.nsdev.lettergenerator.Table;

var prices = Table.builder("Component", "Old price", "New price")
    .columnWeights(2, 1, 1)
    .headerBackground("#10384A")
    .headerStyle(Style.builder().color("#FFFFFF").bold().build())
    .row("Energy (EUR/kWh)", "0.2008", "0.1632")
    .row("Power (EUR/day)", "0.0645", "0.0815")
    .build();

var letter = Letter.builder().body("New prices:").table(prices).build();
```

Column weights are positive proportions, not millimeters. Every physical row must cover the column grid exactly, including cells continued by row spans. At least one body row is required. The table fills its available region width and keeps native selectable text. Its optional header repeats when the table spans pages.

Cells default to 1 mm padding, white 0.25 pt borders and middle vertical alignment. Plain cell paragraph spacing defaults to zero; rich blocks can override spacing. Long tables paginate, repeating their header. Rows prefer staying together when they fit. See the rich-cell and row-policy options below.

For per-cell styling, instantiate `TableCell(runs, style, backgroundColor)` and `TableBlock(columnWeights, header, rows, style)`. An empty header list produces a headerless advanced table. The convenience `Table.builder(...)` always defines a header.

### Template table

```html
<table column-weights="2,1">
  <thead style="background-color:#10384A;color:white">
    <tr><th>Service</th><th>Status</th></tr>
  </thead>
  <tbody>
    <tr style="background-color:#eee"><td>${service}</td><td>${status}</td></tr>
  </tbody>
</table>
```

Text/background styling inherits from table to section to row to cell. One header row is supported. Place all table text inside `td` or `th`; structural text between rows/cells is an error.

### Rich cells, spans and appearance

```java
import com.nsdev.lettergenerator.model.table.Border;
import com.nsdev.lettergenerator.model.table.BorderStyle;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.Padding;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.VerticalAlignment;
import com.nsdev.lettergenerator.model.layout.Keep;

var table = Table.builder("Item", "Description", "Amount")
    .cellAppearance(new CellAppearance(
        new Border(0.5, BorderStyle.SOLID, "#B8C9CF"),
        Padding.uniform(2), VerticalAlignment.TOP, null))
    .headerCells(TableCell.text("Details").span(2, 1), TableCell.text("Amount"))
    .rowCells(TableCell.content(List.of(descriptionParagraph, imageBlock)).span(2, 1),
              TableCell.text("EUR 25.00"))
    .rowKeepTogether(Keep.PREFER)
    .build();
```

Use `TableCell.text(...)` for inline content or `TableCell.content(List<Block>)` for paragraphs, headings, nested lists, images and layout containers. These forms are exclusive. `content(List.of())` is an empty visual cell. `span(columns, rows)`, `withStyle(style)` and `withAppearance(appearance)` return immutable copies. Cell images use the merged width after padding and borders; oversized explicit images are rejected. Nested tables and page breaks are rejected, including inside components/groups.

Every row must cover the physical column grid exactly. A cell starts at the next unoccupied column. Continuing row spans supply their occupied columns; omit those cells from later rows. An empty row is valid when previous spans cover all columns. Column weights still describe physical columns. Spans cannot overlap or extend beyond a section; the single repeating header row cannot span into body rows. Fully covered continuation rows have no independent height and are collapsed for native FO rendering.

Appearance order is legacy defaults, table `cellAppearance`, cell `appearance`, then legacy cell `backgroundColor`. Borders are uniform on four edges and measured in points; padding is top/right/bottom/left in millimeters. Alignment is TOP, MIDDLE or BOTTOM. Null properties inherit independently; `Border.NONE` and `Padding.uniform(0)` are explicit overrides. Background null inherits; transparency overrides and per-edge border styles are not currently exposed.

```html
<table column-weights="2,1,1" row-keep="prefer"
       style="border:0.5pt solid #B8C9CF;padding:2mm;vertical-align:top">
  <thead><tr><th colspan="2">Details</th><th>Amount</th></tr></thead>
  <tbody keep-together="always">
    <tr row-keep="always"><td rowspan="2"><p>Two-row description</p></td>
      <td>First value</td><td>EUR 10.00</td></tr>
    <tr><td>Second value</td><td>EUR 15.00</td></tr>
  </tbody>
</table>
```

Table, section, row and cell appearance inherits toward cells. `padding` accepts one, two (vertical/horizontal), or four lengths; `border` accepts `none` or `width style color`, where style is solid, dashed, dotted or none. `vertical-align` accepts top/middle/bottom. `colspan` and `rowspan` are positive integers.

### Row splitting and row groups

`rowKeepTogether(Keep.AUTO)` allows native row splitting; PREFER is the default and ALWAYS strongly keeps each row. `rowPolicy(new TableRowPolicy(rowIndex, keepTogether, keepWithNext))` overrides a zero-based **body** row. `rowGroup(new TableRowGroup(startRow, rowCount, keepTogether))` requests that a contiguous set of body rows travel together. Groups must be disjoint and in range; their stronger keep takes precedence over weaker row policies. Headers use the table's default row keep and repeat after page breaks.

The HTML equivalents are table/tr `row-keep`, tr `keep-with-next`, and tbody `keep-together` attributes, each taking auto/prefer/always. Cell or rich-block keeps can additionally constrain splitting. FOP may reject an oversized row/group with a strong keep; row splitting with merged cells remains subject to native engine limits.

### Lists

```java
var letter = Letter.builder()
    .bodyHtml("<ol><li>First item</li><li><strong>Second</strong> item</li></ol>")
    .build();
```

Use `ul` for bullets or `ol` for numbering. An `li` contains inline text followed by optional nested `ul`/`ol` children. Each nested list indents relative to its parent and numbers independently from one. Put item text before child lists; arbitrary paragraph/table/image blocks within items remain unsupported. Item margins are unsupported; style the surrounding list.

Typed flat lists retain `ListBlock(items, ordered, style)`, where each item is a list of `TextRun`. Hierarchical lists use `ListBlock.nested(List<ListItem>, ordered, style)` and `new ListItem(runs, children)`. `children` is a list of child `ListBlock` values. JSON uses `contentItems` for hierarchical items and `items` for flat items; exactly one form must be present.

## 10. Images and named assets

The supported image formats are PNG, JPEG and **static SVG** (`image/svg+xml`). Existing image entry points accept all three: body, table cells, positioned content, header, footer, logo, signature and image watermark. Image bytes are copied, format/MIME is checked, and aspect ratio is retained within the requested box. Data URLs, remote image URLs and image paths inside markup remain unsupported; resolve trusted paths in Java or supply named byte assets.

SVG requires the SVG namespace and positive finite intrinsic dimensions, or a usable `viewBox`. Absolute SVG dimensions support px/mm/cm/in/pt/pc. Relative/percentage viewport lengths are unsupported; omit those dimensions and provide a valid viewBox instead. Static shapes, text, groups, local `use` references, gradients, clipping, masks and patterns are accepted. Scripts, animation, filters, `foreignObject`, embedded/external images, stylesheets/imports, entities/DTDs, processing instructions and external resource references are rejected before rendering. URL references must be local fragments. SVG is limited to 2 MB, 10,000 XML nodes, depth 64 and a bounded local-reference expansion. Unusual intrinsic dimensions outside the documented supported geometry fail rather than overflow the fitting calculation.

Ordinary SVG geometry goes through native FOP/Batik PDF rendering, retaining vectors where supported; some paint effects may be rasterized by the renderer. SVG watermarks use a bounded PNG fallback at 150 dpi with a maximum side of 2048 pixels. PNG/JPEG handling is retained.

Use `image.withAltText("Company logo")`, or `<img ... alt="Company logo">`, to supply an image description for tagged output. Descriptions survive ordinary theme preparation and image refitting.

Full template or fragment:

```java
var letter = Letter.builder()
    .bodyHtml("<img src='asset:photo' width='60mm' height='40mm'>")
    .asset("photo", Path.of("assets/photo.jpg"))
    .build();
```

The application reads a trusted path through `.asset(name, Path)`, or supplies already-loaded bytes. The markup identifier `asset:photo` only looks in that supplied asset map; it cannot fetch a path or URL. Assets can be registered after fragment content calls because resolution happens during `build()`.

Image width/height attributes or CSS properties accept `mm`, `pt`, `px`, and percentages of the region's width/height. Unitless image attributes are pixels, with 96 px per inch. An image box with both dimensions supplied retains aspect ratio and may leave unused space on one axis. Unspecified body images fit a 60×40 mm box; header/footer images fit region defaults. Give explicit dimensions when the exact visual size matters. Images must stand alone; image-only paragraphs are accepted, mixed inline text/image paragraphs are not.

Typed body image:

```java
import com.nsdev.lettergenerator.model.block.ImageBlock;

var image = new ImageBlock(pngBytes, "image/png", 60d, 40d,
    Style.builder().textAlign(TextAlign.CENTER).spaceAfterMm(3).build());
var letter = Letter.builder().block(image).build();
```

The `d` suffix is useful for record fields typed as boxed `Double`. `new ImageBlock(..., 60d, 40d, ...)` supplies an explicit box, while convenience factories fit dimensions for you.

## 11. Signatures

```java
import com.nsdev.lettergenerator.signature.ImageSignature;
import com.nsdev.lettergenerator.signature.TextSignature;

var letter = Letter.builder()
    .body("With our best regards,")
    .signature(
        ImageSignature.of(signatureBytes).sizeMm(35, 15),
        TextSignature.of("Jane Smith\nCustomer Services")
            .style(Style.builder().fontSizePt(10).build()))
    .build();
```

Signature parts stack in argument order and flow after the body. The group stays together when it fits. `TextSignature` understands plain newlines and blank-line paragraphs. `ImageSignature.of(Path/byte[])` defaults to a 40×20 mm fit box; `sizeMm` returns a new immutable size override.

Use `signatureHtml(...)` for rich markup or `signatureBlocks(...)` for explicit blocks. Signature content may contain images and styled text; it cannot contain page breaks. A template uses `<signature>...</signature>`.

To anchor the signature above the footer on the final page, select the existing `BOTTOM_LAST` preset. The fluent builder reserves the full inner-page width and the configured height automatically:

```java
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;

var letter = Letter.builder()
    .body("Your letter content")
    .signaturePlacement(SignaturePlacement.BOTTOM_LAST)
    .signatureHeightMm(25)
    .signature(TextSignature.of("Jane Smith\nCustomer Services"))
    .footer("Example Company")
    .build();
```

The default placement is `FLOW`. `signatureHeightMm` defaults to 20 mm and must be finite and positive. With `BOTTOM_LAST`, the builder reserves that height only when signature content is present. Signature HTML also uses it as the basis for default and percentage-based image heights. Explicit signature parts and typed blocks keep their own dimensions; resize an image part with `ImageSignature.sizeMm(...)`. Placement and height options can be set before or after signature content. If the final body content cannot fit beside the reserved signature area, an additional last page may be needed.

The equivalent template configuration is:

```html
<signature placement="bottom-last" height-mm="25">
  <p>Jane Smith<br>Customer Services</p>
</signature>
```

Both attributes are optional: `placement` defaults to `flow`, and `height-mm` to 20. Styles and image content work in either placement. For explicit signature bounds through the typed model or JSON, see [section 16](#16-advanced-fixed-elements-and-last-page-signatures). Image/text signatures here are visible content, not cryptographic PDF signatures.

## 12. HTML templates and placeholders

### Full working shape

```html
<letter page="A4" margins-mm="20" footer-height-mm="18"
        style="font-family:Helvetica;font-size:11pt;line-height:1.2">
  <header align="right" width-mm="80" height-mm="30">
    <p>${customerName}<br>${addressLine1}<br>${postalAddress}</p>
  </header>
  <main>
    <h2>Confirmation</h2>
    <p>Dear <strong>${customerName}</strong>,</p>
    <p>Your price is <span style="color:#008000">${price}</span>.</p>
    <page-break></page-break>
    <p>Further information is available on our website.</p>
  </main>
  <signature><p style="font-style:italic">Jane Smith</p></signature>
  <footer style="font-size:8pt;text-align:center">
    <img src="asset:footer-banner" width="100%" height="6mm">
    <p style="margin-top:1mm;margin-bottom:0">${companyName}</p>
  </footer>
</letter>
```

```java
import java.util.Map;

var letter = Letter.template(templateText)
    .values(Map.of(
        "customerName", "Ana & Filhos <Lda>",
        "addressLine1", "Morada Fictícia, 00",
        "postalAddress", "0000-000 Localidade",
        "price", "0,1632",
        "companyName", "Example Company"))
    .asset("footer-banner", footerBytes)
    .build();
byte[] pdf = new LetterPdfGenerator().generate(letter);
```

Exactly one `letter` root and one `main` are required. Optional regions include `header`, `footer`, `signature`, `logo`, `watermark` and the semantic letter parts described in [section 26](#26-semantic-letter-parts-and-address-layouts). Region order in the source does not change their role. Body block order is preserved. A logo region contains one `img`; watermark syntax is described below.

### Substitution rules

The integrated API uses Apache Commons Text `StringSubstitutor` with **only your map** and `${keyToReplace}` syntax. It parses the template first, then replaces text/attribute values. A replacement like `Ana <strong> & ${other}` remains literal data, including the `${other}` substring. No recursive value substitution or markup injection occurs. Tag and attribute names cannot be generated through placeholders.

Missing keys fail with `InvalidLetterException`; no silent empty replacement is applied. Default-value expressions such as `${name:-fallback}`, nested variable names, environment/system-property lookups and expression evaluation are not supported. Supply supported scalars, immutable temporal values and nested maps/lists; use `param` with explicit locale settings for dates, money and numbers. Map values and asset bytes are snapshotted when supplied. [Section 28](#28-typed-data-conditions-and-repeated-rows) specifies typed data, scope and directive rules.

For CSS placeholders use a property value, for example `style="font-size:${size}"` with `size="12pt"`. A replacement cannot introduce a semicolon and another declaration. Numeric layout attributes remain numeric after replacement.

### Already-substituted markup

```java
var letter = Letter.fromHtml(resolvedMarkup).asset("logo", logoBytes).build();
```

This performs no replacement pass, and `.values(...)` on this builder throws an argument error. If your application uses StringSubstitutor before parsing, it is responsible for escaping replacement values in their HTML context. Prefer the integrated `.template(...).values(...)` path for ordinary scalar data.

Templates support finite named-boolean conditions and list repetition through the integrated `Letter.template(...)` path; see [section 28](#28-typed-data-conditions-and-repeated-rows). Pass bound values through `.values(...)` rather than concatenating unescaped data into markup.

### Supported markup

| Tags | Content/behavior |
| --- | --- |
| `p`, text-only `div`, `h1`–`h6` | Styled text blocks; headings keep with the following block |
| Grouping `div` | Shared inherited text styles for its child blocks |
| `strong`, `b`, `em`, `i`, `u`, `span` | Inline text styling |
| `br` | Line change |
| `a href="..."` | http/https/mailto link; optional `title` attribute |
| `ul`, `ol`, `li` | Text-only list items with inline formatting |
| `table`, `thead`, `tbody`, `tr`, `th`, `td` | Merged tables with text or block content in cells |
| `group` | Styled group with a soft keep; configurable with `keep-together` |
| `component ref="name"` | Include registered reusable blocks; use a paired closing tag |
| `page-number`, `page-count` | Current and total pages; empty elements with paired closing tags |
| `img` | Standalone named asset; optional `alt`, `width`, `height` |
| `page-break` | Body-only explicit page transition; paired closing tag |
| `param` | Empty void tag for formatted typed values; no closing tag |
| `if`, `else` | Named-boolean branches outside tables; template mode only |
| `sender`, `date`, `recipient`, `reference`, `subject`, `salutation`, `closing` | Top-level semantic letter regions |

Supported CSS is inline through `style`: `font-family`, `font-size`, `color`, `font-weight`, `font-style`, `text-decoration`, `text-align`, unitless `line-height`, `margin-top`, `margin-bottom`, table `background-color`, image `width`/`height`, page `keep-together`/`keep-with-next`/`widows`/`orphans`, and table/cell `padding`/`border`/`vertical-align`.

Font sizes accept pt/mm/px. Margins accept mm/pt/px and unitless zero. Colors accept `#RGB`, `#RRGGBB`, black, white, gray/grey, red, green, blue, navy and teal. Font weight accepts normal/bold/400/700; font style normal/italic; decoration none/underline. A font family names one available family, not a browser-style fallback list.

Root styles provide text/paragraph defaults. Styled containers preserve inherited scopes; HTML compilation may therefore produce transparent AUTO groups in the shared model. Region/group text properties inherit; put margins on individual paragraphs/text blocks. Background colors are supported on table elements, not ordinary text spans/paragraphs. Unsupported tags, attributes, styles and HTML error recovery fail with a location. Use letter markup without `html`, `head` or `body` wrappers, scripts, classes, stylesheets or browser positioning rules. This is a letter document format with an HTML vocabulary, not a browser renderer.

### Root and region attributes

| Element | Attribute | Meaning |
| --- | --- | --- |
| `letter` | `page` | A4 or LETTER, case-insensitive |
| `letter` | `margins-mm` | Uniform margins, default 20 |
| `letter` | `header-height-mm` | Default header content height, 25 |
| `letter` | `footer-height-mm` | Footer content height, 15 |
| `letter` | `page-width-mm`, `page-height-mm` | Paired positive custom paper dimensions |
| `letter` | `orientation` | PORTRAIT or LANDSCAPE |
| `letter` | `language-tag`, `date-pattern`, `date-style`, `currency`, `zone-id` | Literal explicit locale configuration; see section 27 |
| `signature` | `placement` | flow (default) or bottom-last, case-insensitive |
| `signature` | `height-mm` | Signature content height, default 20; reserved on the last page for bottom-last |
| `header`, `footer` | `pages` | all (fallback), first or following; default all |
| Content elements | `style-name` | Reference a named theme style below local CSS |
| `header` | `align` | left, center or right; box and default content alignment |
| `header` | `width-mm`, `height-mm` | Optional preset box dimensions |
| `header` | `x-mm`, `y-mm`, `width-mm`, `height-mm` together | Exact box relative to inner-page origin |

Root region tags may appear in any order; their child content keeps its source order.

The full template format currently offers uniform root margins. Use the fluent/advanced Java model when independent margin values are required.

## 13. Fonts: defaults, embedding and Unicode

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

## 14. Country and stationery variations

Set paper size, margins, header/address bounds, typography and assets for each letter through builder methods or template attributes.

Configure a layout before rendering the body:

```java
var letter = Letter.builder()
    .page(PageSize.LETTER)
    .margins(new Margins(15, 18, 17, 22))
    .header(recipientAddress)
    .headerBoundsMm(88, 23, 78, 28)
    .body(bodyText)
    .build();
```

Those measurements are illustrative, not a claim of conformance to a postal or national letter standard. Measure the actual envelope/window/stationery, convert paper-edge coordinates using section 5, and inspect a printed sample at 100% scale. Printer scaling and physical tolerances are outside PDF generation.

With HTML templates, use scalar geometry placeholders:

```html
<letter page="${paper}" margins-mm="${margin}">
  <header x-mm="${addressX}" y-mm="${addressY}"
          width-mm="${addressWidth}" height-mm="${addressHeight}">
    <p>${recipientName}<br>${street}<br>${postalAddress}</p>
  </header>
  <main><p>${message}</p></main>
</letter>
```

Supply those keys along with recipient data in `.values(...)`. Use separate placeholders for address lines because template whitespace follows HTML rules. Use explicit `LocaleSettings` with `param` for currency, dates and decimal separators; application text such as salutations remains caller-supplied. Choose/register fonts that cover the actual language.

## 15. Watermarks

```java
var draft = Letter.builder().body("Body").watermark("DRAFT").build();
```

The text factory uses Helvetica 50 pt, gray, 0.12 opacity, 45° counterclockwise rotation, centered on all pages. For explicit options:

```java
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;

var watermark = new TextWatermark("DRAFT", "Helvetica", 50d, "#B0B0B0",
    PageSelector.FIRST, 0.15, 35d);
var letter = Letter.builder().body("Body").watermark(watermark).build();
```

`ImageWatermark.of(Path/byte[])` supplies an image watermark with defaults; the explicit record sets box, selector, opacity and rotation. A template uses:

```html
<watermark opacity="0.12" rotation="45" pages="ALL"
           style="font-size:50pt;color:#bbb">DRAFT</watermark>
```

For an image watermark, put one named `img` inside `watermark` and omit text. Put image styles/dimensions on that `img`.

Watermarks are drawn behind existing PDF content, consume no layout space, and may be covered by opaque images/table backgrounds. Text watermarks use regular undecorated fonts. Vertical centering uses cap height, so lowercase/descender-heavy text is approximate. Omitting a watermark skips the PDFBox post-processing pass entirely.

## 16. Advanced fixed elements and last-page signatures

Use immutable model records when you need more than the ordinary header/footer API: multiple address/sender boxes, different first/following-page content, explicit footer positioning, or custom bounds within a last-page signature zone.

This complete construction uses a first-page address, following-page running header, repeated footer and bottom-last signature:

```java
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.layout.Zone;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.List;

var body = new Paragraph(List.of(new TextRun("Dear Ana, your request was received.", null, null)), null);
var address = new Paragraph(List.of(new TextRun("Cliente Exemplo\nMorada Fictícia, 00", null, null)), null);
var running = new Paragraph(List.of(new TextRun("Example Company", null, null)), null);
var footer = new Paragraph(List.of(new TextRun("example.com", null, null)), null);
var signed = new Paragraph(List.of(new TextRun("Jane Smith\nCustomer Services", null, null)), null);

var page = new PageSpec(PageSize.A4, Margins.uniform(20), 55d, 20d, 30d);
var letter = new LetterDocument(1, page, null,
    List.of(new Section("body", null, List.of(body))),
    List.of(
        new FixedElement("address", Zone.TOP, PageSelector.FIRST,
            new Bounds(85, 15, 80, 30), List.of(address)),
        new FixedElement("running-header", Zone.TOP, PageSelector.FOLLOWING,
            new Bounds(0, 0, 170, 15), List.of(running)),
        new FixedElement("footer", Zone.BOTTOM, PageSelector.ALL,
            new Bounds(0, 5, 170, 15), List.of(footer))),
    new Signature(SignaturePlacement.BOTTOM_LAST, List.of(signed), new Bounds(0, 0, 170, 25)),
    null);
byte[] pdf = new LetterPdfGenerator().generate(letter);
```

`TOP` bounds are relative to the top zone's upper-left; `BOTTOM` bounds are relative to the bottom zone's upper-left. Both zones begin at the inner page's left edge. Signature bounds are relative to the signature zone. Zone sizes reserve their full widths even on pages where no selected content appears. There is no wrapping beside a fixed element.

`FIRST` selects page 1, `FOLLOWING` selects pages 2 onward, and `ALL` selects every page. In a one-page letter, FIRST and ALL apply. Fixed boxes may overlap only when their page selectors cannot coincide. IDs must be unique. The fluent builder reserves IDs `letter.header`, `letter.footer` and `letter.logo` for its generated regions.

`SignaturePlacement.FLOW` has no bounds and appends after the body. `BOTTOM_LAST` requires a positive signature zone and bounds fitting it. When page layout cannot combine the final body content with the reserved signature area, generation may add a dedicated last signature page. The footer remains present there. The library checks rendered page masters and clipping; it does not silently omit a signature.

## 17. JSON input

```java
byte[] pdf = generator.generateJson(Files.readString(Path.of("letter.json")));
```

Minimal JSON:

```json
{
  "schemaVersion": 1,
  "sections": [{
    "name": "body",
    "blocks": [{
      "type": "paragraph",
      "runs": [{"text": "Olá João, request received."}]
    }]
  }]
}
```

`Letter.fromJson(json)` parses JSON into the shared model for `validate(...)`, copy methods or `render(...)`. `generator.renderJson(json[, options])` returns the same rendering report and profile choices as the other APIs. `generateJson(json)` retains the byte-only standard path. JSON maps to the immutable model, not the HTML template format. It has no placeholder substitution pass. Enums use uppercase names. Block `type` values are `paragraph`, `heading`, `list`, `image`, `table`, `pageBreak`, `group`, `component`, `letterPart`, `canvas`, `columns` and `box`; watermark `kind` is `text` or `image`. Image `data` is Base64. Unknown fields/discriminators, duplicate keys, malformed JSON, trailing documents, scalar coercion, numeric enum ordinals and fractional schema versions are rejected.

Optional model fields may be omitted or null and use their documented defaults. Required fields produce a field-path error. See the [model constructor reference](#21-public-api-reference) and public records for full structured input. Do not treat external Java class names as JSON discriminators.

### Structured list, table and watermark examples

A list block contains an array of items, each represented by an array of text runs:

```json
{"type":"list","ordered":true,"items":[[{"text":"First"}],[{"text":"Second"}]]}
```

A table block can specify styles and backgrounds per cell:

```json
{"type":"table","columnWeights":[2,1],
 "header":[
   {"runs":[{"text":"Component"}],"backgroundColor":"#10384A","style":{"color":"#FFFFFF"}},
   {"runs":[{"text":"Price"}],"backgroundColor":"#10384A","style":{"color":"#FFFFFF"}}],
 "rows":[[
   {"runs":[{"text":"Energy (€/kWh)"}]},
   {"runs":[{"text":"0,1632"}]}]]}
```

For a text watermark, put this object in the document's `watermark` property:

```json
{"kind":"text","text":"DRAFT","fontFamily":"Helvetica","fontSizePt":60,
 "color":"#808080","pages":"ALL","opacity":0.2,"rotationDegrees":35}
```

An image watermark uses Base64 bytes and an explicit millimeter box:

```json
{"kind":"image","data":"BASE64_PNG_OR_JPEG","mimeType":"image/png",
 "widthMm":80,"heightMm":40,"pages":"FIRST","opacity":0.15,"rotationDegrees":10}
```

Links permit `http`, `https` and `mailto`. Document text is escaped before internal FO construction; external XML entity loading and document-controlled filesystem/network resource access are disabled.

## 18. Application integration and concurrency

Build a generator once and reuse it. `LetterPdfGenerator` instances are safe for concurrent generation because each render owns its engine state. A no-argument generator needs no configuration; a custom-font generator retains immutable font bytes after its builder completes.

`LetterBuilder`, `HtmlLetterBuilder`, `TableBuilder` and style builders are mutable request-building objects. Do not share one mutable builder between concurrent requests. Each document build produces an independent immutable snapshot. The library copies image data and collection contents; later changes to caller-owned data do not mutate a built letter.

Generation returns a `byte[]` only after validation, layout and any watermark pass finish. Pass the returned bytes to the caller. For an HTTP response, the application sets `Content-Type: application/pdf` and the desired download filename. Framework/servlet integration is application-owned.

Do not rely on temporary filesystem paths in a deployed JAR for classpath resources. Package fonts as resources and register them through `registerClasspathFont`; supply image assets as bytes or through the supported path-taking methods. No font scanning, database connection, font auto-download or external image fetch is required. Supported public method signatures expose no FOP, PDFBox or Jackson engine types.

For a server/JVM without a display, set `-Djava.awt.headless=true` on the application's Java command. This is especially useful when the host sets a `DISPLAY` variable even though no display is accessible. The library does not change this global JVM setting. The project's rendering tests already run in headless mode.

Rendering uses memory for document state, assets and output bytes. Apply request size/concurrency budgets in the hosting application according to your workload. The library does not set a page-count limit or stream a partially generated PDF.

## 19. Errors and troubleshooting

```java
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.exception.PdfGenerationException;

try {
    byte[] pdf = generator.generate(letter);
    Files.write(Path.of("letter.pdf"), pdf);
} catch (InvalidLetterException error) {
    System.err.println("Invalid letter at " + error.fieldPath() + ": " + error.getMessage());
} catch (PdfGenerationException error) {
    System.err.println("Letter could not be laid out: " + error.getMessage());
}
```

Catch errors around `build()` too when compiling templates; template validation occurs there. Fluent argument checks can throw `IllegalArgumentException`, missing required content can throw `IllegalStateException`, and path-taking image/asset methods can throw `IOException`. Model validation occurs at `generate()`; overflow/glyph failures can only be known during rendering.

| Symptom | Cause and action |
| --- | --- |
| No accessible font-registry constructor | Use public `new LetterPdfGenerator()`; internal registry configuration is private |
| `${name}` is printed literally | You used plain/fragment content or `fromHtml`; use `Letter.template(...).values(...)` for substitution |
| Missing template variable | Add the key to the map or publish a template without that placeholder |
| A replacement's `<strong>` prints literally | Expected for map data; put formatting in the template, not replacement values |
| A replacement newline does not change lines | HTML whitespace collapses; use separate placeholders separated by `<br>`, or plain Java multiline text |
| Unknown family / missing variant | Register that exact family and every requested regular/bold/italic/bold-italic face |
| Missing glyph / replacement character rejected | Choose a font covering the actual character and verify the target-language output |
| Header is full width | Add `headerWidthMm` if you want a narrow positioned block; alignment alone aligns its content |
| Right-positioned box still has left-aligned paragraph | An explicit paragraph/region text style overrides default alignment |
| Address location is off by the page margin | Convert paper-edge coordinates to inner-page coordinates; inspect the box versus glyph baseline |
| Body starts too low | Header y+height and logo height reserve space; reduce the box or use a different layout configuration |
| Header/footer overflow | Increase content height/width, reduce image/text size or content; nothing is silently clipped |
| Header collides with logo | Use automatic placement with a suitable width, or move the exact box away from the separate logo |
| Unexpected extra page | Inspect explicit breaks, long unbreakable text, keep-together signature groups and bottom-last signature reservation |
| Image does not fill both dimensions | Aspect ratio is preserved within the supplied box |
| Missing `asset:name` | Supply it through `.asset(name, bytesOrPath)`; the library never fetches image references |
| Unsupported CSS/tag/HTML source error | Use the documented subset; remove browser wrappers/layout styles or fix malformed markup |
| `NoClassDefFoundError` when using a copied JAR | Include transitive runtime dependencies |

Avoid swallowing rendering errors and returning empty/partial PDFs. Log the field path and underlying exception in the application's normal error handling, with its own handling of recipient-sensitive data.

## 20. Run the examples and inspect output

From the repository, run all checks and generate example PDFs:

```sh
./mvnw clean verify
```

Focused rendering examples:

```sh
./mvnw -Dtest=HeaderPlacementTest test
./mvnw -Dtest=EnergyTemplateTest test
./mvnw -Dtest=EnergyReferenceLetterTest test
```

| Test/example | Output under `target/examples/` |
| --- | --- |
| `HeaderPlacementTest` | `header-exact-position.pdf` and PNG |
| `EnergyTemplateTest` | `energy-template-letter.pdf`, PNG, `template-assets/logo.png` and `footer.png` |
| `EnergyReferenceLetterTest` | Typed `energy-reference-letter.pdf`, fluent `energy-fluent-letter.pdf`, JSON `energy-json-letter.pdf`, and PNGs |
| `TemplateMarkupTest` | `template-features.pdf` and PNG; repeated styled/image footer across two pages |
| `HeaderPlacementExamples` | `header-left.pdf`, `header-center.pdf`, `header-right.pdf`, `header-window.pdf` |

Standalone consumer applications live in `examples/`. Automated tests and Java fixtures live in `src/test/java/`; reusable HTML, JSON, values and images live together in `src/test/resources/letters/energy/`. Test inputs are read from the classpath, so loading them does not depend on the working directory. Maven excludes these fixtures and test fonts from the published library JAR.

The reusable fixture sources are `src/test/java/com/nsdev/lettergenerator/fixtures/EnergyReferenceLetter.java` and `FluentEnergyLetter.java`. Edit these for the low-level/fluent energy examples, or edit the HTML/values files for the HTML-template example. `TableRenderingTest` separately checks styling, immutable input ownership and repeating table headers.

All four API tests generate the same anonymized reference scenario: typed Java, fluent Java, JSON and HTML. Each anchors the signature above the footer on the last page with `BOTTOM_LAST` and a 25 mm signature zone; the tests check its rendered position. Names, addresses, contract/delivery identifiers, company branding, websites and telephone details are synthetic; the logo and image footer use generic “Energia Exemplo” artwork. No original signature, logo or screenshot is embedded.

Run all four together:

```sh
./mvnw -Dtest=EnergyReferenceLetterTest,EnergyTemplateTest test
```

Every PDF must contain the expected selectable content in order, the synthetic logo/image footer, and one A4 page. Editable inputs are [energy-letter.json](src/test/resources/letters/energy/energy-letter.json), [energy-price-update.html](src/test/resources/letters/energy/energy-price-update.html) and [energy-values.json](src/test/resources/letters/energy/energy-values.json). Shared synthetic images are [energy-logo.png](src/test/resources/letters/energy/energy-logo.png) and [energy-footer.png](src/test/resources/letters/energy/energy-footer.png). The JSON test reads the native JSON fixture directly; it does not serialize a Java document to manufacture its input.

To run standalone applications against the packaged JAR and only production dependencies, use these commands from the repository on Linux/macOS:

```sh
./mvnw package dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/classpath.txt
mkdir -p target/example-classes
javac --release 17 -Xlint:all -Werror \
  -cp "target/letter-generator-1.0.0-SNAPSHOT.jar:$(cat target/classpath.txt)" \
  -d target/example-classes examples/GenerateLetter.java \
  examples/GenerateTemplateLetter.java examples/HeaderPlacementExamples.java
java -cp "target/example-classes:target/letter-generator-1.0.0-SNAPSHOT.jar:$(cat target/classpath.txt)" GenerateLetter
java -cp "target/example-classes:target/letter-generator-1.0.0-SNAPSHOT.jar:$(cat target/classpath.txt)" HeaderPlacementExamples
java -cp "target/example-classes:src/test/resources:target/letter-generator-1.0.0-SNAPSHOT.jar:$(cat target/classpath.txt)" GenerateTemplateLetter
```

`GenerateTemplateLetter` loads the reference template, values, synthetic images and fonts from the classpath by default; the command above adds `src/test/resources` to supply them. Its five optional filesystem arguments are template HTML, values JSON, logo PNG, footer PNG and output PDF. Supplied file paths override the corresponding default resources; fonts still come from the classpath. `GenerateLetter` optionally accepts JSON path and output path. `HeaderPlacementExamples` optionally accepts an output directory. On Windows adapt shell syntax and use `;` as the Java classpath separator.

Open a generated PDF in your PDF viewer, for example `xdg-open target/examples/header-right.pdf` on a Linux desktop. Compare printed address-window placement at actual size. Do not judge exact physical coordinates from a viewer's zoomed screenshot.

## 21. Public API reference

Public packages start with `com.nsdev.lettergenerator`. The `internal` subtree is implementation detail; applications should not import it.

### Entry points and generator

| API | Purpose |
| --- | --- |
| `Letter.builder()` | New mutable letter builder |
| `Letter.template(String)` | New template builder with map substitution |
| `Letter.fromHtml(String)` | New template builder for already-resolved markup |
| `Letter.fromJson(String)` | Strict JSON into the shared immutable model |
| `Letter.PAGE_BREAK` | Form-feed marker for plain body strings |
| `new LetterPdfGenerator()` | Reusable generator with built-in fonts |
| `LetterPdfGenerator.builder()` | Optional custom-font configuration |
| `generator.generate(LetterDocument)` | Validate/render and return complete PDF bytes |
| `generator.generateJson(String)` | Parse strict JSON, validate/render |
| `generator.renderJson(String[, RenderOptions])` | JSON rendering with reports and per-call output choices |
| `generator.validate(LetterDocument)` | Inspect declared model constraints without PDF layout |
| `generator.render(LetterDocument[, RenderOptions])` | PDF bytes, actual page count, diagnostics and optional debug geometry |
| `Table.builder(String... headers)` | Convenience table builder |

Generator builder methods: `registerFont(String, FontVariant, Path)`, `registerClasspathFont(String, FontVariant, String)`, `hyphenationPatterns(String languageTag, byte[] trustedXml)`, `build()`.

### LetterBuilder

| Method | Effect |
| --- | --- |
| `page(PageSize)` | Select A4 or LETTER |
| `pageSizeMm(double, double)`, `orientation(Orientation)`, `landscape()` | Custom paper and orientation |
| `canvas(CanvasBlock)` | Append an indivisible positioned canvas |
| `marginsMm(double)`, `margins(Margins)` | Uniform or independent page margins |
| `style(Style)` | Document text/paragraph defaults |
| `body(String...)` | Append literal plain paragraphs/line changes/page markers |
| `heading(String)` | Append a heading |
| `pageBreak()` | Append a body page transition |
| `table(TableBlock)`, `block(Block)` | Append typed content |
| `bodyHtml(String)` | Append formatted fragment |
| `group(String...)`, `group(Block...)` | Append a softly kept content group |
| `theme(LetterTheme)`, `component(String, LetterComponent)`, `include(String)` | Theme defaults, reusable registrations and body references |
| `body/header/footer/signature(LetterComponent)` | Reuse an immutable fragment directly |
| `header(String...)`, `header(Path/byte[])` | Replace header with text or image |
| `headerHtml(String)`, `headerBlocks(Block...)` | Replace header with formatted/typed content |
| `headerAlignment(TextAlign)` | Left/center/right box and default content alignment |
| `headerWidthMm(double)`, `headerHeightMm(double)` | Set or resize the header box |
| `headerBoundsMm(double x, double y, double width, double height)` | Exact header box relative to inner-page origin |
| `headerStyle(Style)` | Header default content style |
| Header/footer content methods with leading `PageSelector` | Replace content for ALL, FIRST or FOLLOWING |
| `headerStyle(PageSelector, Style)`, `footerStyle(PageSelector, Style)` | Selection-specific appearance |
| `footer(String...)`, `footer(Path/byte[])` | Replace footer with text or image |
| `footerHtml(String)`, `footerBlocks(Block...)` | Replace footer with formatted/typed content |
| `footerHeightMm(double)`, `footerStyle(Style)` | Footer size/default style |
| `logo(Path/byte[]/ImageBlock)` | Set a separate top-left logo |
| `signature(SignatureContent...)` | Replace signature with stacked text/image parts |
| `signatureHtml(String)`, `signatureBlocks(Block...)` | Replace signature with formatted/typed content |
| `signaturePlacement(SignaturePlacement)` | FLOW (default) or BOTTOM_LAST above the final footer |
| `signatureHeightMm(double)` | Reserved BOTTOM_LAST zone and HTML image height basis; default 20 mm |
| `watermark(String/Watermark)` | Set text-default or explicit watermark |
| `asset(String, Path/byte[])` | Snapshot a named image asset |
| `fixedElement(FixedElement)` | Append advanced fixed content to configured zones |
| `sender(Party)`, `recipient(Party)`, `date(LocalDate)` | Replace semantic identity/address/date parts |
| `subject(String)`, `reference(String, String)`, `salutation(String)`, `closing(String)` | Replace conventionally ordered semantic phrases |
| `part(LetterElement, List<Block>[, Style])`, `addressLayout(AddressLayout)` | Rich semantic parts and editable address positioning |
| `locale(LocaleSettings/Locale)`, `settings(DocumentSettings)`, `metadata(DocumentMetadata)` | Explicit language/formatting/document options |
| `direction(TextDirection)`, `fallbackFonts(String...)`, `hyphenate(boolean)` | Explicit international text layout choices |
| `attach(PdfAttachment)` | Append an existing PDF after the letter |
| `build()` | Produce an immutable LetterDocument snapshot |

Configuration calls replace their option; body and advanced fixed-element calls append. Repeated region content calls replace that region. Width/height setters after exact header bounds preserve its x/y. Builders are mutable; use a new instance per concurrent request.

### Templates, tables and signatures

| API | Methods/options |
| --- | --- |
| `HtmlLetterBuilder` | `values(Map<String, ?>)`, `asset(String, Path/byte[])`, `theme(...)`, `component(...)`, `locale(...)`, `settings(...)`, `compile()`, `validate([generator])`, `build()` |
| `PreparedLetterTemplate` | `requirements()`, `build()`, `bind(Map<String, ?>)`, `validate([generator])` |
| `LocaleSettings` | Stable country/date/currency/zone choices; `defaults()`, `locale()` |
| `ValueFormats` | `format(value, format, pattern, locale, currencyOverride)` |
| `TableBuilder` | `row(String...)`, `columnWeights(double...)`, `style(Style)`, `headerStyle(Style)`, `headerBackground(String)`, `row(List<TableCell>)`, `rowCells(TableCell...)`, `headerCells(TableCell...)`, `cellAppearance(...)`, `rowKeepTogether(...)`, `rowPolicy(...)`, `rowGroup(...)`, `options(...)`, `build()` |
| `Style.builder()` | Family, size, color, emphasis booleans, alignment, line-height, before/after spacing, `named(...)`, `keepTogether(...)`, `keepWithNext(...)`, `widows(...)`, `orphans(...)` |
| `TextSignature.of(String)` | Optional `.style(Style)`, immutable result |
| `ImageSignature.of(Path/byte[])` | Optional `.sizeMm(width, height)`, immutable result |
| `TextWatermark.of(String)` | Standard centered text watermark |
| `ImageWatermark.of(Path/byte[])` | Standard centered image watermark |

### Immutable model constructors

| Package/type | Fields in constructor order |
| --- | --- |
| `model.LetterDocument` | `schemaVersion, page, defaultStyle, sections, fixedElements, signature, watermark[, theme, components[, settings, attachments]]` |
| `model.DocumentSettings` | `locale, metadata, direction, fallbackFonts, hyphenate` |
| `localization.LocaleSettings` | `languageTag[, datePattern, dateStyle, currencyCode, zoneId]` |
| `letter.Party`, `letter.Address` | `name, organization, address`; `lines` |
| `model.block.LetterPartBlock` | `element, blocks, style` |
| `model.Section` | `name, style, blocks` |
| `model.layout.PageSpec` | `size, marginsMm, topZoneMm, bottomZoneMm, signatureZoneMm[, dimensions, orientation]` |
| `model.layout.Margins` | `top, right, bottom, left` |
| `model.layout.Bounds` | `x, y, width, height` |
| `model.layout.FixedElement` | `id, zone, pages, boundsMm, blocks` |
| `model.layout.Signature` | `placement, blocks, boundsMm` |
| `model.block.Paragraph` | `runs, style` |
| `model.block.Heading` | `runs, style[, semanticStyle]` |
| `model.block.GroupBlock` | `blocks, style[, defaultStyle]` |
| `model.block.ComponentBlock` | `name, style` |
| `model.block.ListBlock` | `items, ordered, style[, contentItems]` |
| `model.block.ImageBlock` | `data, mimeType, widthMm, heightMm, style[, altText]` |
| `model.block.TableBlock` | `columnWeights, header, rows, style[, options]` |
| `model.block.PageBreakBlock` | No fields; body only |
| `model.table.TableCell` | Legacy `runs, style, backgroundColor`; rich form also takes `blocks, columnSpan, rowSpan, appearance[, semanticStyle[, defaultAppearance]]` |
| `model.text.TextRun` | `text, style, href[, pageNumber]`; factories `currentPage()` and `pageCount()` |
| `model.style.Style` | `fontFamily, fontSizePt, color, bold, italic, underline, textAlign, lineHeight, spaceBeforeMm, spaceAfterMm[, keepTogether, keepWithNext, widows, orphans[, styleName[, baseStyle]]]` |
| `model.watermark.TextWatermark` | `text, fontFamily, fontSizePt, color, pages, opacity, rotationDegrees` |
| `model.watermark.ImageWatermark` | `data, mimeType, widthMm, heightMm, pages, opacity, rotationDegrees` |

Bracketed trailing constructor groups are optional overloads; original constructors remain available. `semanticStyle`, cell `defaultAppearance` and group `defaultStyle` preserve lower-priority authoring defaults; ordinary application styling uses `style`.

Use schema version `1`. Null optional style properties inherit. PageSpec null fields select defaults. Sections preserve input order; their names are labels without layout meaning. Every box requires finite nonnegative x/y and positive dimensions. Required content/fields are validated during generation; constructing a record is not a guarantee that it is renderable. Image array data has defensive copying and content-based equality.

## 22. Supported scope and limits

The supported scope is styled PDF letters with native text, flow pagination, PNG/JPEG/static SVG content, rich tables, bounded canvases, columns, decorated boxes, nested lists, reusable themes/stationery, fixed letter regions, visible signatures and optional text/image watermarks. Typed HTML templates support finite conditions, repeats and formatted parameters; prepared templates capture immutable authoring configuration. Optional metadata, native tagged/PDF/A output and standard-profile PDF packs are described below. The library checks geometry, resource references, font variants/glyphs, and rendering overflow, including painted glyph/underline bounds that can exceed logical line boxes. It does not shrink text silently; oversized images and nonbreaking text may require smaller dimensions or different content. It returns no partial PDF on failure.

HTML input supports the finite vocabulary above. General CSS/browser layout, arbitrary fonts from template URLs, scripts, classes/stylesheets, nested tables, inline images inside text runs, automatic postal-standard compliance, DOCX output and cryptographic signing are not exposed. Tagged output does not claim PDF/UA compliance. PDF/A-2b uses the native renderer and has explicit restrictions; see section 33. Use application logic or a future explicit feature for those requirements; do not assume browser behavior.

For an issue report, include a minimal template/model, typed binding data, required font/image assets with permission to share, the Java/library version, and the complete exception including its cause. Use fictional recipient data where possible. `./mvnw clean verify` is the project's full acceptance command; examples and previews are generated under `target`, so clean builds remove and recreate them.

## 23. Development and code quality

For a guided route through the source, start with [where to start reading](CONTRIBUTING.md#where-to-start-reading). It traces each input API through the shared model, validation, PDF layout and optional watermarking, and maps common changes to their owning classes. Package overviews and method contracts are available in source Javadocs; the contributor guide includes the command to generate browsable internal/API documentation.

```sh
./mvnw spotless:apply   # Apply the checked-in Java formatting convention
./mvnw clean verify     # Check format, compile with all warnings as errors, run tests and Checkstyle
```

Java source, tests, and the standalone example use four-space AOSP formatting with explicit imports. Checkstyle checks production and test code for braces, single statements, imports, naming, and other basic correctness rules. Production domain values must be named constants; fixture input/expected values remain literal. Dependency and formatter versions are pinned in the POM. PDFBox, FontBox, and PDFBox IO are aligned through dependency management; Maven Enforcer rejects PDF dependency downgrades.

The package layout separates responsibilities:

| Package under `com.nsdev.lettergenerator` | Responsibility |
| --- | --- |
| root, `builder`, `signature`, `model.*`, `font`, `exception`, `theme`, `component`, `diagnostics`, `render`, `letter`, `localization`, `template`, `document` | Supported application contract |
| `internal.composition`, `internal.table` | Theme/component preparation and shared table grid/appearance rules |
| `internal.builder`, `internal.builder.content` | Builder-specific region assembly and deferred content resolution |
| `internal.content`, `internal.util` | Shared text/image conversion, immediate argument checks and collection snapshots |
| `internal.defaults` | Letter and watermark default policies shared across input APIs |
| `internal.markup`, `internal.template` | Strict document markup, map binding and template composition |
| `internal.validation`, `internal.json` | Field-path validation and strict JSON parsing |
| `internal.font`, `internal.style`, `internal.layout` | Immutable font snapshots, inherited styles, dimensions and units |
| `internal.render.fo` | Structured FO block/style/page-layout serialization |
| `internal.render.fop` | FOP configuration, diagnostics, glyph-bound checks, resource allowlist and hardened FO transform |
| `internal.render.watermark`, `internal.output` | Optional watermarks, output constraints and PDF pack processing |
| `internal.hyphenation` | Explicit trusted pattern snapshots and native dictionary setup |
| `internal.render.resource`, `internal.xml` | Private render-resource identifiers and engine-independent XML helpers |

The test suite separates shared fixtures from integration tests. Image records provide byte-content equality/hash codes in addition to defensive copying. Pagination recovery uses typed diagnostics and retries only recoverable body overflow or a missing last-page master. Font, fixed-box, and parsing failures do not trigger a broad retry.

Template compilation delegates each top-level region to a dedicated `TemplateTagHandler`. An immutable registry defines the supported tags and their execution order; handlers receive a fresh compilation context. To extend the library with another top-level tag, implement its handler and register it once. Contributor instructions explain [how to add a template region](CONTRIBUTING.md#adding-a-template-region).

See [CONTRIBUTING.md](CONTRIBUTING.md) for contributor conventions. User-facing instructions and API examples are maintained in this README. Internal plans and implementation notes are kept locally under the ignored `docs/` directory.

## 24. Reusable themes and components

Supply immutable themes and components per document, and reuse them across requests. Create a new mutable letter builder for each letter.

### Role defaults and named styles

```java
import com.nsdev.lettergenerator.theme.LetterTheme;
import com.nsdev.lettergenerator.model.style.Style;

var company = LetterTheme.builder()
    .body(Style.builder().fontFamily("Helvetica").fontSizePt(11)
        .color("#24363D").spaceAfterMm(3).build())
    .heading(Style.builder().fontSizePt(16).bold().build())
    .header(Style.builder().fontSizePt(10).build())
    .footer(Style.builder().fontSizePt(8).color("#666666").build())
    .signature(Style.builder().fontSizePt(11).build())
    .tableHeader(Style.builder().bold().color("#FFFFFF").build())
    .named("notice", Style.builder().bold().color("#10384A").build())
    .named("urgent", Style.builder().named("notice").underline().build())
    .build();

var letter = Letter.builder()
    .theme(company)
    .heading("Confirmation")
    .bodyHtml("<p style-name='notice'>Your request has been accepted.</p>")
    .bodyHtml("<p style-name='urgent' style='font-weight:normal'>Please retain this letter.</p>")
    .build();
```

The available `StyleRole` values are BODY, HEADING, TABLE, TABLE_HEADER, TABLE_CELL, HEADER, FOOTER and SIGNATURE. `role(role, style)` registers any role; corresponding convenience methods are available on the theme builder. BODY is the fallback for roles without their own properties. Font registration belongs to `LetterPdfGenerator`. Themes select registered families, typography, text colors and block spacing. Reuse `TableOptions`/`CellAppearance` values for table fills, borders and padding; set a suitable header background when choosing a light header text color.

The cascade resolves built-in/semantic defaults, BODY and block-role defaults, contextual header/footer/signature/table-cell defaults, explicit document/section/container properties, a local named style, and finally local properties. Each property inherits separately; explicit false and zero values win. A contextual footer role therefore applies to paragraphs in the footer, and an explicit paragraph style overrides it. Inline text overrides the containing paragraph. Group margins and keeps apply to the group; its typography inherits into children.

Set `.theme(...)` on the builder before `.build()` so the theme participates when convenience defaults are assembled. `LetterDocument.withTheme(...)` is useful for typed models: it preserves styles already stored in the existing snapshot, including defaults materialized by an earlier builder. Changing a theme never mutates previously built documents.

For typed content, set `Style.builder().named("notice").bold(false).build()` on the block or run. For HTML, use `style-name="notice"` plus optional local CSS. Named styles may inherit another named style through `.named(...)`; unknown references, blank names and cycles are errors, including unused registered references. There are no filesystem or URL includes.

### Reuse headers, addresses, signatures and legal text

```java
import com.nsdev.lettergenerator.component.LetterComponent;
import com.nsdev.lettergenerator.model.block.ComponentBlock;

var address = LetterComponent.text("Example Company\n100 Example Avenue\nSample City");
var legal = LetterComponent.text("Example Company | example.invalid");
var signature = LetterComponent.text("Kind regards,\nCustomer Services");

var letter = Letter.builder()
    .theme(company)
    .header(address)
    .footer(legal)
    .signature(signature)
    .component("legalNotice", legal)
    .body("Dear Alex,")
    .include("legalNotice")
    .build();
```

`LetterComponent.of(Block...)` wraps any supported model content, including styled text, images and tables. `text(String...)` creates one paragraph per argument; a newline stays within that paragraph. Direct `body(component)` groups its content softly; header/footer/signature overloads reuse it in those regions. Use `new ComponentBlock("legalNotice", localStyle)` for a styled named reference anywhere blocks are accepted.

Named references use the document's component map. Components can reference other components; generation rejects unknown names, cycles and content invalid for its final location. A component with a page break cannot be inserted in a table cell or signature. References normally form softly kept groups; the reference's explicit keep can override that policy. Registrations and content lists are snapshotted, and images retain their defensive byte ownership.

```java
var template = Letter.template("""
    <letter>
      <header><component ref="address"></component></header>
      <main><h1 style-name="notice">Hello ${recipient}</h1>
        <component ref="legalNotice" style="font-size:9pt"></component></main>
      <footer><p>Page <page-number></page-number> of <page-count></page-count></p></footer>
    </letter>
    """)
    .theme(company)
    .component("address", address)
    .component("legalNotice", legal)
    .values(Map.of("recipient", "Alex Example"));
var letter = template.build();
```

`component` and page-field tags require paired closing tags; they are empty elements in this format, not HTML void tags. `style-name` belongs to content styles; logo wrappers and watermark wrappers require their existing explicit configuration.

### JSON equivalents

JSON carries the same composition values, with enum names in uppercase. This complete example uses a named style and a reusable fragment:

```json
{
  "schemaVersion": 1,
  "theme": {
    "roles": {"BODY": {"fontSizePt": 11}, "HEADING": {"fontSizePt": 16, "bold": true}},
    "namedStyles": {"notice": {"color": "#10384A", "bold": true}}
  },
  "components": {
    "legal": {"blocks": [{"type": "paragraph", "runs": [{"text": "Example Company | example.invalid"}]}]}
  },
  "sections": [{
    "name": "Body",
    "blocks": [
      {"type": "heading", "runs": [{"text": "Confirmation"}], "style": {"styleName": "notice"}},
      {"type": "group", "style": {"keepTogether": "ALWAYS"}, "blocks": [
        {"type": "paragraph", "runs": [{"text": "Dear Alex,"}]},
        {"type": "component", "name": "legal", "style": {"bold": false}}
      ]}
    ]
  }]
}
```

A rich JSON cell uses `blocks` instead of `runs`, with optional `columnSpan`, `rowSpan` and `appearance`. Table `options` holds `cellAppearance`, `rowKeepTogether`, `rowPolicies` and `rowGroups`:

```json
{
  "type": "table",
  "columnWeights": [2, 1],
  "header": [{"runs": [{"text": "Combined heading"}], "columnSpan": 2}],
  "rows": [[{"blocks": [{"type": "paragraph", "runs": [{"text": "Rich content"}]}], "columnSpan": 2}]],
  "options": {
    "cellAppearance": {
      "border": {"widthPt": 0.5, "style": "SOLID", "color": "#B8C9CF"},
      "padding": {"topMm": 2, "rightMm": 2, "bottomMm": 2, "leftMm": 2},
      "verticalAlignment": "TOP"
    },
    "rowKeepTogether": "PREFER",
    "rowPolicies": [{"rowIndex": 0, "keepTogether": "ALWAYS"}],
    "rowGroups": [{"startRow": 0, "rowCount": 1, "keepTogether": "PREFER"}]
  }
}
```

## 25. Validate, diagnose and preview layout

### Inspect a template before rendering

```java
var template = Letter.template(source)
    .values(Map.of("recipient", "Alex Example"))
    .asset("logo", logoBytes)
    .theme(company);

var report = template.validate();
// Use template.validate(generator) when the application registers custom fonts.
if (!report.isValid()) {
    for (var diagnostic : report.diagnostics()) {
        System.out.println(diagnostic.code() + " | " + diagnostic.path() + " | " + diagnostic.message());
    }
}
```

Inspection parses and binds literal values, then aggregates **missing scalar placeholder occurrences and invalid/missing referenced image assets** in active branches and repeat clones. Directive/parameter failures retain a first-error boundary; see section 29. It does not perform environment/system/URL lookup, recursive replacement or markup injection. Asset errors caused by a missing source variable are not duplicated as missing-asset errors. Once that preflight passes, compilation and semantic validation report the first remaining model error, including registered-font and declared-geometry checks. It generates no FO or final PDF.

`generator.validate(document)` validates a typed, fluent-built or JSON-derived model without running layout. It reports the first semantic input error. Both methods return immutable `ValidationReport` values with `isValid()` and `diagnostics()`. Successful input validation does not establish that text fits, how many pages it requires, or whether the engine can honor every keep.

### Return the PDF and an actual rendering report

```java
var result = generator.render(template.build());
Files.write(Path.of("letter.pdf"), result.pdfBytes());
int actualPages = result.pageCount();
for (var diagnostic : result.diagnostics()) {
    System.out.println(diagnostic.code() + ": " + diagnostic.message());
}
```

`RenderResult` owns its PDF bytes and returns a defensive copy on access. `pageCount()` is the accepted final count, including any dedicated signature page and appended attachment pages. `parts()` identifies the letter and each attachment range. Ordinary successful rendering usually has no diagnostics. `SIGNATURE_PAGE_RECOVERY` is informational: the bounded retry allocated a final page for the reserved signature zone. Diagnostic fields are `code`, `severity` (ERROR/WARNING/INFO), `phase` (INPUT/LAYOUT), `path` and `message`.

Existing `generate(...)` and `generateJson(...)` retain their byte-array/exception behavior. Invalid inputs still throw `InvalidLetterException`. Rejected layouts still throw `PdfGenerationException` and return no partial PDF. Its `diagnostics()` contains classified layout errors when available (`BODY_OVERFLOW` or `LAYOUT_ERROR`); failures outside those checks can have an empty list, with details retained in the exception cause.

### Debug allocated geometry

```java
import com.nsdev.lettergenerator.render.RenderOptions;

var debug = generator.render(template.build(), RenderOptions.debugPreview());
Files.write(Path.of("letter-debug.pdf"), debug.pdfBytes());
for (var box : debug.debugBoxes()) {
    System.out.println(box.pageNumber() + " | " + box.kind()
        + " | " + box.xMm() + ", " + box.yMm() + " mm");
}
```

Debug output outlines the inner margin area, allocated body, reserved header/footer zones, fixed content rectangles and final signature box. Coordinates use millimeters from the **physical page's upper-left corner**, with one-based page numbers. The `DECLARED_*` kinds describe allocated rectangles; they are not measurements of individual glyphs or painted image pixels. The overlay adds no text or pages, and normal rendering has an empty `debugBoxes()` list. Debug decoration is for authoring previews; choose ordinary rendering for the delivered letter.

Run `./mvnw test` to generate `target/examples/composition-features.pdf` and `composition-debug.pdf`, plus first-page PNG previews. `CompositionExampleTest` combines themed text, reusable legal content, first/continuation headers, current/total page fields, merged rich cells, row keeps and a bottom-last signature. The four anonymized energy examples remain separate regressions for the original APIs.

## 26. Semantic letter parts and address layouts

Use sender, date, recipient, reference, subject, salutation and closing methods for an ordinary letter. Calls replace the previous value for that role; their call order does not change the conventional display order. Body content still appends in call order.

```java
import com.nsdev.lettergenerator.letter.Address;
import com.nsdev.lettergenerator.letter.Party;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import java.time.LocalDate;

var letter = Letter.builder()
    .sender(Party.company("Example Company", Address.of("18 Harbour Road", "Example City EX1 2AB")))
    .recipient(Party.of("Alex Example", Address.of("24 Sample Lane", "Example Town EX2 3CD")))
    .date(LocalDate.of(2026, 10, 19))
    .reference("Account", "EX-1001")
    .subject("Service renewal confirmation")
    .salutation("Dear Alex,")
    .body("Your service has been renewed.")
    .closing("Kind regards,\nCustomer Services")
    .locale(new LocaleSettings("en-GB"))
    .build();
```

The order is sender, date, recipient, reference, subject, salutation, body, then closing. The subject defaults to bold 14 pt with a keep to the following content. Semantic theme roles are `SENDER`, `DATE`, `RECIPIENT`, `REFERENCE`, `SUBJECT`, `SALUTATION` and `CLOSING`; explicit local styles override those roles. Date formatting uses the **final** builder locale, so setting `.locale(...)` after `.date(...)` is supported. Postal lines retain their supplied order and line breaks; the library does not infer national address rules.

`Party(name, organization, address)` accepts an individual, an organization, or both. `Party.of(name, address)` and `Party.company(organization, address)` are conveniences. A party needs a nonblank name or organization; supplied identity fields and address lines must be nonblank. `Address.of(String...)` snapshots ordered lines. The fluent semantic methods check these inputs during authoring.

For rich content use `.part(LetterElement.SUBJECT, blocks)` or `.part(element, blocks, localStyle)`. The shared typed equivalent is `new LetterPartBlock(element, blocks, style)`. Typed and JSON block lists keep the order the caller supplies; fluent/HTML semantic authoring supplies the conventional ordering. JSON uses the `letterPart` block discriminator:

```json
{
  "schemaVersion": 1,
  "sections": [{"name": "Body", "blocks": [
    {"type": "letterPart", "element": "SUBJECT", "blocks": [
      {"type": "paragraph", "runs": [{"text": "Service renewal confirmation"}]}]},
    {"type": "paragraph", "runs": [{"text": "Your service has been renewed."}]}
  ]}]
}
```

HTML provides optional top-level `<sender>`, `<date>`, `<recipient>`, `<reference>`, `<subject>`, `<salutation>` and `<closing>` regions. Each occurs at most once and accepts supported rich blocks and inline styles. A `main` region remains required. A reference requires a nonblank literal `label`; the compiler inserts a bold label and colon before its content. A date can use an ISO local-date `value` or authored rich content:

```html
<letter language-tag="en-GB">
  <sender><p><strong>Example Company</strong><br>18 Harbour Road</p></sender>
  <date value="2026-10-19"></date>
  <recipient><p>Alex Example<br>24 Sample Lane</p></recipient>
  <reference label="Account"><p><em>EX-1001</em></p></reference>
  <subject>Service renewal confirmation</subject>
  <salutation>Dear Alex,</salutation>
  <main><p>Your service has been renewed.</p></main>
  <closing><p>Kind regards,<br>Customer Services</p></closing>
</letter>
```

For an address window, `.addressLayout(AddressLayout.windowEnvelope())` moves configured sender, recipient and date parts into a body canvas. The editable `AddressLayout(senderBounds, recipientBounds, dateBounds, heightMm)` uses millimeters relative to that canvas; a null bound leaves that part in ordinary flow. Positioned IDs are `letter.sender`, `letter.recipient` and `letter.date`. Bounds must fit the final available width and the declared panel height; measure the actual envelope and inspect a printed sample. The preset is an example arrangement, without a postal compliance guarantee.

## 27. Country-aware dates, numbers and currencies

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

## 28. Typed data, conditions and repeated rows

`.values(Map<String, ?>)` owns nested maps and lists immediately. Accepted leaf values are strings/character sequences, characters, booleans, byte/short/int/long values, `BigInteger`, `BigDecimal`, finite floats/doubles, and immutable `LocalDate`, `LocalTime`, `LocalDateTime`, `OffsetDateTime`, `OffsetTime`, `ZonedDateTime` or `Instant` values. Character sequences become strings. Null values, blank/non-string map keys, object cycles, arbitrary objects, bean reflection, arrays and unsupported number implementations fail with the input data path.

`${customer.name}` resolves a nested map value. At each map level, an exact literal dotted key wins over splitting that key into nested lookups: `"customer.name"` can be supplied directly. `${key}` remains a literal text substitution; use `param` for formatting:

```html
<letter language-tag="en-GB" currency="EUR">
  <main>
    <p>Hello ${customer.name}. Your appointment is <param key="appointment" format="date">.</p>
    <p>Balance: <param key="balance" format="currency">.</p>
    <p>Exact decimal format: <param key="balance" format="number" pattern="#,##0.00">.</p>
    <if flag="overdue">
      <p>Please contact us about this balance.</p>
      <else><p>Thank you for keeping your account up to date.</p></else>
    </if>
    <table column-weights="3,1">
      <thead><tr><th>Service</th><th>Amount</th></tr></thead>
      <tbody><tr data-each="items" data-as="item">
        <td>${item.description}</td><td><param key="item.amount" format="currency"></td>
      </tr></tbody>
    </table>
    <p data-if="showContact">Contact ${contact} for help.</p>
  </main>
</letter>
```

```java
var template = Letter.template(source).values(Map.of(
    "customer", Map.of("name", "Alex Example"),
    "appointment", LocalDate.of(2026, 10, 19),
    "balance", new BigDecimal("29.75"),
    "overdue", false,
    "items", List.of(
        Map.of("description", "Home service", "amount", new BigDecimal("24.50")),
        Map.of("description", "Additional support", "amount", new BigDecimal("5.25"))),
    "showContact", false));
var letter = template.build();
```

`param` is an **empty HTML void element**: use `<param ...>` or `<param .../>`, without `</param>`. Its required `key` selects a value; optional `format`, `pattern` and `currency` select the finite formatting rules above. Put styling on an enclosing `span` or `strong`, because the parameter itself becomes one literal text node.

| Directive | Exact behavior |
| --- | --- |
| `<if flag="name">...<else>...</else></if>` | Requires a named boolean; one optional direct `else`, last in the wrapper |
| `data-if="name"` | Removes that element when its named boolean is false |
| `data-each="path" data-as="item"` | Repeats trusted block/row markup once for each list item, in order |
| `${item.field}` | Resolves the current repeat alias; outer aliases remain available in nested repeats |

Conditions accept actual booleans, without expressions, negation or string truthiness. Use `data-if` on rows/cells inside tables; `if` wrappers are supported outside tables. Repetition supports rows and ordinary blocks such as paragraphs, `div`, lists, tables, images, canvases and boxes; it does not repeat letter regions or table structural containers. `data-as` requires a simple alias matching `[A-Za-z_][A-Za-z0-9_]*` and is valid only with `data-each`. On a node with both repeat and condition directives, the list is expanded first and the condition resolves inside each local scope.

A nested alias shadows the same outer/root name only for its cloned subtree. After that subtree, the outer binding is retained. Bound strings never become tags, attributes, directives or recursive placeholders. CSS property names stay literal; substituted CSS values cannot introduce another declaration.

Missing selected values fail with a template selector path. Missing conditions/collections also fail; an empty list emits no clones, a false condition emits no selected content, and unused branches do not require their bindings or assets. A blank string stays blank; it is not a missing key. Maps/lists cannot be printed as scalar text. The final document must still contain valid body content, and a repeated table must still satisfy ordinary table validation.

Directives run only through `Letter.template(...)`. `Letter.fromHtml(...)` expects resolved supported markup and leaves `${literal}` text intact; it does not evaluate directives. The compiled output is the same ordinary immutable block model used by typed Java, fluent Java and JSON.

## 29. Prepare templates once and inspect their contracts

Call `.compile()` after configuring a template's assets, theme, components, locale and optional default values. It parses and captures an immutable source prototype. Each subsequent build/bind clones that prototype and owns a fresh traversal and model; reuse one prepared template concurrently.

```java
import com.nsdev.lettergenerator.template.PreparedLetterTemplate;

PreparedLetterTemplate prepared = Letter.template(
    "<letter><main><p>Hello ${name}.</p></main></letter>")
    .values(Map.of("name", "Alex Example"))
    .compile();

var first = prepared.build(); // Uses captured default bindings.
var second = prepared.bind(Map.of("name", "Sam Example")); // Replaces the whole binding map for this call.
byte[] firstPdf = generator.generate(first);
byte[] secondPdf = generator.generate(second);
```

Later changes to the original builder, data maps/lists or asset byte arrays do not change a prepared template. Theme and component configuration follow the same shared preparation rules as ordinary `.build()`. Parsing is reused; binding, model validation and PDF layout still happen for each document. Builders remain mutable and should not be shared concurrently. A prepared `Letter.fromHtml(...).compile()` supports `.build()`, while `.bind(...)` rejects values because substitution is disabled.

`prepared.requirements()` returns immutable occurrence lists: `variables()`, `flags()`, `collections()`, `assets()`, `styles()` and `components()`. Every `TemplateRequirement` contains `name()` and a source selector `path()`, including an attribute suffix where relevant. Repeated occurrences are retained. `variables` includes `${...}` placeholders and `param` keys; `assets` includes literal `asset:name` image references. Dynamic references retain the original placeholder text. The manifest includes conditional branches and scoped alias paths, so it is a syntactic inventory rather than a statement that every name must be supplied at the root for every binding.

`prepared.validate([generator])` inspects the captured default binding/configuration without PDF layout. For another binding, use `generator.validate(prepared.bind(values))`. Inspection aggregates missing scalar placeholder occurrences in active branches/repeat clones and independent asset errors. Missing directive paths, parameter formatting failures and the first remaining semantic model error stop that phase with a located diagnostic. Success does not establish final content fit.

Run `V1LayoutExampleTest` for `target/examples/v1-layout-pack.pdf` and its PNG preview: a two-page letter with SVG stationery, positioned addresses, columns, nested lists, a decorated notice, a bottom-last signature and an appended PDF. It also renders the model through JSON and checks identical extracted content.

Run `V1AuthoringExampleTest` to produce `target/examples/v1-authoring.pdf` and `v1-authoring.png`. The example combines semantic regions, deferred/localized dates, formatted amounts, a repeated table, boolean branches and repeated binding from one prepared template. `PreparedTemplateTest` also exercises concurrent independent bindings.


## 30. Company stationery and table appearance

A theme combines typography with reusable stationery and table appearance. All four input APIs use the same preparation step. Explicit local values override shared defaults; `false`, zero padding, `Border.NONE` and `Keep.AUTO` are deliberate values.

```java
import com.nsdev.lettergenerator.theme.Stationery;
import com.nsdev.lettergenerator.theme.TableAppearance;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.Padding;
import com.nsdev.lettergenerator.model.layout.Keep;

var stationery = Stationery.builder()
    .header("Example Company")
    .headerAlignment(TextAlign.RIGHT)
    .footerHtml("<p><strong>Example Company</strong> — Customer services</p>")
    .build();
var company = LetterTheme.builder()
    .body(Style.builder().fontSizePt(11).color("#243746").build())
    .role(StyleRole.SUBJECT, Style.builder().fontSizePt(16).bold().build())
    .tableAppearance(new TableAppearance(
        new CellAppearance(null, Padding.uniform(2), null, "#F3F5F7"),
        new CellAppearance(null, null, null, "#10384A"),
        Keep.PREFER))
    .stationery(stationery)
    .build();

var letter = Letter.builder().theme(company).subject("Account update").body("Your update...").build();
var fromHtml = Letter.fromHtml("<letter><main>Your update...</main></letter>").theme(company).build();
```

`StationeryBuilder` uses ordinary letter region fitting and positioning. Configure `page(...)` and `margins(...)` for the intended sheet. `headerBounds(Bounds)` is relative to the inner page; `headerHeightMm`, `footerHeightMm`, `headerStyle`, `footerStyle`, `asset`, `logo` and rich `header(PageSelector, Block...)` / `footer(PageSelector, Block...)` support styled content and image blocks. An image footer can also use `asset("footer", bytes).footerHtml("<img src='asset:footer'>")`. Build separate stationery values when paper/margins or country layouts differ. Explicit geometry is checked against the final letter; inherited stationery does not silently rescale.

`new Stationery(elements, topZoneMm, bottomZoneMm)` is the typed/JSON form. Letter-local fixed elements replace inherited elements with the same stable ID. Generated IDs include `letter.header`, `letter.footer` and `letter.logo`; page variants add suffixes such as `.first` and `.following`. Other inherited regions remain. Top/bottom reservations use the larger configured height, so changing a local header cannot accidentally reclaim inherited footer/body space.

`TableAppearance(cells, header, rowKeepTogether)` sits below explicit table options and cell appearance. Header defaults additionally overlay common cell defaults; the fluent table's builtin header fill is a fallback rather than an explicit local override. `headerBackground(...)` is explicit and wins. Theme row defaults do not replace row-specific policies or groups. Typography still uses `TABLE`, `TABLE_HEADER` and `TABLE_CELL` roles.

JSON places these values under `theme.tableAppearance` and `theme.stationery`. Semantic roles additionally include `SENDER`, `RECIPIENT`, `DATE`, `SUBJECT`, `REFERENCE`, `SALUTATION` and `CLOSING`. `CompleteThemeFeaturesTest` verifies table appearance and stationery through typed Java, fluent Java, JSON and HTML.

## 31. Canvases, columns, boxes and custom paper

Use ordinary body flow for text that should paginate. Use a canvas when an address panel, reference field or other region needs explicit coordinates. A canvas consumes a fixed height in flow and moves as one indivisible unit. Coordinates are millimeters relative to its upper-left corner. Stable element IDs and explicit bounds also make this model suitable for a future visual editor; the library currently provides rendering and model data.

```java
import com.nsdev.lettergenerator.Canvas;
import com.nsdev.lettergenerator.model.layout.Bounds;

var panel = Canvas.builder(170, 70)
    .text("recipient", new Bounds(0, 25, 80, 35), "Alex Example\n200 Sample Street")
    .text("reference", new Bounds(100, 0, 60, 20), "Account EX-1001")
    .build();
var letter = Letter.builder().canvas(panel).body("Content follows the panel.").build();
```

`CanvasBuilder.at(id, Bounds, Block...)` accepts any supported rich blocks. The coordinate overload accepts `id, x, y, width, height, blocks`. `element(PositionedContent)` retains a preconstructed element. Styles are available on the canvas and each element. Every canvas requires unique nonblank IDs, positive finite dimensions and boxes wholly within its declared bounds. Elements paint in list order; overlap is intentional and allowed within a canvas. Element content must fit its box. Oversized canvases and painted content overflow fail rather than shrink or clip. Explicit page breaks are forbidden inside positioned elements.

```html
<canvas width-mm="170" height-mm="70">
  <box id="recipient" x-mm="0" y-mm="25" width-mm="80" height-mm="35">
    <p>Alex Example<br>200 Sample Street</p>
  </box>
  <box id="reference" x-mm="100" y-mm="0" width-mm="60" height-mm="20">
    <p><strong>Account</strong> EX-1001</p>
  </box>
</canvas>
```

The shared model is `CanvasBlock(widthMm, heightMm, elements, style)` with `PositionedContent(id, boundsMm, blocks, style)`. JSON uses `type:"canvas"`, the same dimensions, and `elements` containing `id`, `boundsMm`, `blocks` and optional `style`. The canvas origin moves with body pagination; these coordinates are not physical page-edge positions. Use fixed elements for repeating paper-relative content.

For independent flowing columns, use `ColumnsBlock(List<Column>, gapMm, style)`, where `Column(weight, blocks, style)` receives a proportional share of the width left after fixed gaps. Content in each column remains in that column; columns do not automatically rebalance newspaper-style. Native table pagination can continue long columns. Explicit page breaks within columns are unsupported.

```html
<columns gap-mm="10">
  <column weight="1"><p>Contact details</p></column>
  <column weight="2"><p>Longer explanatory content.</p></column>
</columns>
<box style="padding:4mm; border-left:2pt solid #10384A; background-color:#F3F5F7">
  <p>A highlighted flowing notice.</p>
</box>
```

A standalone `box` flows at its content height. Typed `BoxBlock(blocks, appearance, style)` uses `BoxAppearance(padding, borders, backgroundColor)` and `Borders(top, right, bottom, left)`; `Borders.uniform(border)` is convenient. Reuse `Padding` in millimeters and `Border` widths in points. HTML boxes accept ordinary text CSS plus `padding` (one/two/four lengths), uniform `border`, per-edge `border-top/right/bottom/left`, and `background-color`. Border values are `none` or width/style/color. Inline image/text layout and general browser CSS remain outside the supported vocabulary.

For paper, use `.page(PageSize.A4)` or `.page(PageSize.LETTER)`, `.landscape()`, or `.pageSizeMm(180, 240)`. Custom dimensions preserve their supplied orientation in the fluent call; a later `.orientation(Orientation.PORTRAIT/LANDSCAPE)` explicitly selects the shorter/longer horizontal edge. Typed/JSON `PageSpec` adds `dimensions: {widthMm, heightMm}` and `orientation`; its default orientation is PORTRAIT. `page.widthMm()` and `page.heightMm()` return effective physical dimensions. HTML uses paired `page-width-mm` / `page-height-mm` root attributes and optional `orientation`. Margins and region coordinates retain their existing meaning.

## 32. Writing direction, fallback fonts and hyphenation

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

## 33. Metadata, tagged output and PDF/A

Metadata is independent of the input API:

```java
import com.nsdev.lettergenerator.model.DocumentMetadata;
import com.nsdev.lettergenerator.render.OutputProfile;
import com.nsdev.lettergenerator.render.RenderOptions;

var letter = Letter.builder()
    .metadata(new DocumentMetadata("Account update", "Example Company", "Account information", "account,letter"))
    .body("Your account update...").build();
var tagged = generator.render(letter, new RenderOptions(false, OutputProfile.TAGGED));
```

`DocumentMetadata(title, author, subject, keywords)` contains optional text fields. The language comes from `settings.locale.languageTag`. `OutputProfile.STANDARD` retains ordinary behavior. `TAGGED` uses native FOP accessibility structure for text, headings, lists/tables and images, with alternate descriptions supplied by `ImageBlock.altText` / HTML `alt`. The current heading model marks headings as H1; full heading-level hierarchy is not represented. Positioned content follows author/list order, which may differ from visual order. Review reading order and provide meaningful descriptions. This mode does not claim PDF/UA compliance or replace an accessibility audit.

`PDF_A_2B` selects native PDF/A-2b output. Every emitted text font must be explicitly registered for embedding, including needed variants; built-in Helvetica/Courier/Times are not embedded. This includes headings, rich/nested content and page fields. Use an embedded default family via `.style(...)` or a theme. The native renderer enforces its conformance constraints and emits output intent/XMP metadata. Tests inspect native output intent, embedding and metadata, and exercise rejected configurations; they are not an independent PDF/A certification. Validate production archival output with an independent validator as part of your application's acceptance checks.

Tagged and PDF/A modes currently reject watermarks, debug overlays and appended PDF packs before rendering, because these processors cannot preserve those output guarantees. Ordinary metadata is supported with the standard profile and packs. `RenderOptions(boolean debug)` remains available; the two-argument form adds the profile. No partial PDF is returned for an unsupported combination.

## 34. Append existing PDFs

Use immutable attachments to produce a letter followed by existing PDF pages:

```java
import com.nsdev.lettergenerator.document.PdfAttachment;

var letter = Letter.builder().body("Please find the supporting pages attached.")
    .attach(PdfAttachment.of(Path.of("terms.pdf")))
    .attach(PdfAttachment.of(otherPdfBytes))
    .build();
var result = generator.render(letter);
Files.write(Path.of("complete-pack.pdf"), result.pdfBytes());
for (var part : result.parts()) {
    System.out.println(part.kind() + ": page " + part.firstPage() + ", count " + part.pageCount());
}
```

`PdfAttachment.of(byte[])` and `.of(Path)` own their bytes; later source changes do not alter the model. Invalid, empty and encrypted PDFs are rejected. `LetterDocument.withAttachments(List<PdfAttachment>)` is the typed-model equivalent. JSON uses `attachments:[{"data":"BASE64_PDF"}]`. There is no path/URL loading from JSON or HTML. An HTML-generated model can call `.withAttachments(...)` before rendering.

Attachments append in registration order. Existing content and annotations are retained; the generated letter's stationery, signatures, page fields and watermarks apply to letter pages. Dynamic “Page X of Y” fields count the generated letter, while `RenderResult.pageCount()` counts the full merged pack. `parts()` gives a LETTER range followed by one ATTACHMENT range for each input PDF, with one-based `firstPage` and positive `pageCount`. Appending is available only in the standard profile. This is page assembly, without cryptographic-signature preservation guarantees for signed source PDFs.
