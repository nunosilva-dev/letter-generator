# Letter Generator

A framework-free Java 17 library that turns Java objects, HTML templates or JSON into PDF bytes. Apache FOP composes and paginates the letter; PDFBox runs only when a watermark is requested. Your application supplies content, styles and assets, manages template storage, and decides where to save or send the PDF.

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
- [12. Stored HTML templates and placeholders](#12-stored-html-templates-and-placeholders)
- [13. Fonts: defaults, embedding and Unicode](#13-fonts-defaults-embedding-and-unicode)
- [14. Database integration and template versions](#14-database-integration-and-template-versions)
- [15. Country and stationery variations](#15-country-and-stationery-variations)
- [16. Watermarks](#16-watermarks)
- [17. Advanced fixed elements and last-page signatures](#17-advanced-fixed-elements-and-last-page-signatures)
- [18. JSON input](#18-json-input)
- [19. Application integration and concurrency](#19-application-integration-and-concurrency)
- [20. Errors and troubleshooting](#20-errors-and-troubleshooting)
- [21. Run the examples and inspect output](#21-run-the-examples-and-inspect-output)
- [22. Public API reference](#22-public-api-reference)
- [23. Supported scope and limits](#23-supported-scope-and-limits)
- [24. Development and code quality](#24-development-and-code-quality)

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
| A database stores an editable template with `${key}` | `Letter.template(markup).values(map)` | Integrated literal-value substitution and named assets |
| Application already produced resolved markup | `Letter.fromHtml(markup)` | Explicitly performs no substitution |
| Most content is plain Java, some is formatted markup | `bodyHtml`, `headerHtml`, `footerHtml`, `signatureHtml` | Mix formatted fragments into fluent construction |
| Exact page selectors, multiple fixed boxes or bottom-last signature | Immutable model records | Full geometry contract |
| An application boundary exchanges structured documents | `generator.generateJson(json)` | Strict versioned JSON model |

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
    .header("ANA EXEMPLO\nRUA DA ENERGIA, 24\n1000-001 LISBOA")
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

The fluent header repeats on every page. For a first-page address block and a different following-page header, use [advanced fixed elements](#17-advanced-fixed-elements-and-last-page-signatures).

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

`.logo(Path/byte[])` fits a PNG/JPEG within 45×20 mm and places it at the top-left of the inner page, beside the automatic header. `.logo(ImageBlock)` supplies explicit image dimensions/style. The separate logo region repeats on every page. To place a brand image within an exact header rather than beside it, include that image in header content and omit the separate logo call.

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

Style inheritance is property-by-property: rendering defaults → document → section → block → run. Missing properties inherit. `Style.plain()` has no overrides. Explicit boolean values such as `.bold(false)` disable inherited emphasis. Region convenience styles are applied to their blocks, with explicit block styles taking precedence. A style does not change supplied text. `Style.overlay(other)` returns a new style with only the explicitly supplied properties of `other` overriding the original.

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

Column weights are positive proportions, not millimeters. Every row must match the header column count. At least one body row is required. The table fills its available region width and keeps native selectable text. Its optional header repeats when the table spans pages.

Cells have 1 mm padding and a thin white grid. Cell paragraph spacing is fixed at zero; table block spacing controls space around the table. Long tables paginate; rows stay together when possible. There are no row spans, column spans, nested blocks or inline images in cells.

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

### Lists

```java
var letter = Letter.builder()
    .bodyHtml("<ol><li>First item</li><li><strong>Second</strong> item</li></ol>")
    .build();
```

Use `ul` for bullets or `ol` for numbering. Each `li` contains text and inline formatting, not nested block elements/lists. Item margins are unsupported; style the surrounding list. Typed construction uses `ListBlock(items, ordered, style)` where each item is a list of `TextRun`.

## 10. Images and named assets

The supported image formats are PNG and JPEG. Image bytes are copied, format/MIME is checked, and aspect ratio is retained within the requested box. SVG, data URLs, remote image URLs and image paths inside markup are unsupported.

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

For a signature anchored near the bottom of the last page, use `SignaturePlacement.BOTTOM_LAST` and an explicit signature zone as shown in [section 17](#17-advanced-fixed-elements-and-last-page-signatures). Image/text signatures here are visible content, not cryptographic PDF signatures.

## 12. Stored HTML templates and placeholders

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
        "addressLine1", "Rua da Energia, 24",
        "postalAddress", "1000-001 Lisboa",
        "price", "0,1632",
        "companyName", "Example Company"))
    .asset("footer-banner", footerBytes)
    .build();
byte[] pdf = new LetterPdfGenerator().generate(letter);
```

Exactly one `letter` root and one `main` are required. Optional regions are one each of `header`, `footer`, `signature`, `logo`, and `watermark`. Region order in the source does not change their role. Body block order is preserved. A logo region contains one `img`; watermark syntax is described below.

### Substitution rules

The integrated API uses Apache Commons Text `StringSubstitutor` with **only your map** and `${keyToReplace}` syntax. It parses the template first, then replaces text/attribute values. A replacement like `Ana <strong> & ${other}` remains literal data, including the `${other}` substring. No recursive value substitution or markup injection occurs. Tag and attribute names cannot be generated through placeholders.

Missing keys fail with `InvalidLetterException`; no silent empty replacement is applied. Default-value expressions such as `${name:-fallback}`, nested variable names, environment/system-property lookups and expression evaluation are not supported. Supply strings, numbers, booleans or characters; format dates, money and locale-dependent numbers before calling the library. Map values and asset bytes are snapshotted when supplied.

For CSS placeholders use a property value, for example `style="font-size:${size}"` with `size="12pt"`. A replacement cannot introduce a semicolon and another declaration. Numeric layout attributes remain numeric after replacement.

### Already-substituted markup

```java
var letter = Letter.fromHtml(resolvedMarkup).asset("logo", logoBytes).build();
```

This performs no replacement pass, and `.values(...)` on this builder throws an argument error. If your application uses StringSubstitutor before parsing, it is responsible for escaping replacement values in their HTML context. Prefer the integrated `.template(...).values(...)` path for ordinary scalar data.

### Supported markup

| Tags | Content/behavior |
| --- | --- |
| `p`, text-only `div`, `h1`–`h6` | Styled text blocks; headings keep with the following block |
| Grouping `div` | Shared inherited text styles for its child blocks |
| `strong`, `b`, `em`, `i`, `u`, `span` | Inline text styling |
| `br` | Line change |
| `a href="..."` | http/https/mailto link; optional `title` attribute |
| `ul`, `ol`, `li` | Text-only list items with inline formatting |
| `table`, `thead`, `tbody`, `tr`, `th`, `td` | Rectangular text tables |
| `img` | Standalone named asset; optional `alt`, `width`, `height` |
| `page-break` | Body-only explicit page transition; paired closing tag |

Supported CSS is inline through `style`: `font-family`, `font-size`, `color`, `font-weight`, `font-style`, `text-decoration`, `text-align`, unitless `line-height`, `margin-top`, `margin-bottom`, table `background-color`, and image `width`/`height`.

Font sizes accept pt/mm/px. Margins accept mm/pt/px and unitless zero. Colors accept `#RGB`, `#RRGGBB`, black, white, gray/grey, red, green, blue, navy and teal. Font weight accepts normal/bold/400/700; font style normal/italic; decoration none/underline. A font family names one available family, not a browser-style fallback list.

Root styles provide text/paragraph defaults. Region/group text properties inherit; put margins on individual paragraphs/text blocks. Background colors are supported on table elements, not ordinary text spans/paragraphs. Unsupported tags, attributes, styles and HTML error recovery fail with a location. Use letter markup without `html`, `head` or `body` wrappers, scripts, classes, stylesheets or browser positioning rules. This is a letter document format with an HTML vocabulary, not a browser renderer.

### Root and header attributes

| Element | Attribute | Meaning |
| --- | --- | --- |
| `letter` | `page` | A4 or LETTER, case-insensitive |
| `letter` | `margins-mm` | Uniform margins, default 20 |
| `letter` | `header-height-mm` | Default header content height, 25 |
| `letter` | `footer-height-mm` | Footer content height, 15 |
| `header` | `align` | left, center or right; box and default content alignment |
| `header` | `width-mm`, `height-mm` | Optional preset box dimensions |
| `header` | `x-mm`, `y-mm`, `width-mm`, `height-mm` together | Exact box relative to inner-page origin |

The full template format currently offers uniform root margins. Use the fluent/advanced Java model when independent margin values are required.

## 13. Fonts: defaults, embedding and Unicode

### Defaults require no files

`new LetterPdfGenerator()` uses the engine's standard Helvetica, Times and Courier families, including regular, bold, italic and bold italic. Helvetica is the default document font. These standard fonts require no operating-system font lookup or application registration. They are **not embedded TrueType resources bundled in this library's JAR**; PDF viewers provide standard-font display behavior.

The DejaVu files under this repository's `src/test/resources/fonts` are licensed test/example fixtures. Their presence does not make DejaVu a production default. EDP examples explicitly register their `ReferenceSans` alias to those fixtures.

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

## 14. Database integration and template versions

Store the template as UTF-8 TEXT/CLOB, not one database row per paragraph. Store a stable template key, version and format alongside the text. Published versions should be immutable so a previously generated letter can be reproduced with the same template/data/assets.

```sql
CREATE TABLE letter_template (
    template_key VARCHAR(100) NOT NULL,
    template_version INTEGER NOT NULL,
    template_format VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    PRIMARY KEY (template_key, template_version)
);
```

Use `TemplateBuilder.FORMAT`, currently `LETTER_HTML_V1`, as the format identifier. This version identifies the document vocabulary; it is separate from your business template revision. [schema.sql](examples/templates/schema.sql) is an editable application-owned example, not a migration executed by the library.

An application's rendering service can follow this shape. `templateRepository`, `assetRepository`, `templateKey`, `version` and `values` belong to your application:

```java
var stored = templateRepository.load(templateKey, version);
if (!TemplateBuilder.FORMAT.equals(stored.format())) {
    throw new IllegalArgumentException("Unsupported stored template format");
}
var letter = Letter.template(stored.content())
    .values(values)
    .asset("company-logo", assetRepository.load(stored.logoVersion()))
    .asset("footer-banner", assetRepository.load(stored.footerVersion()))
    .build();
byte[] pdf = generator.generate(letter);
```

Choose the referenced image asset names in the template, then resolve them in application code. Store bytes in a BLOB or object store as appropriate; the library does not select storage technology or fetch document-controlled URLs. Version assets too if historical reproduction matters. Per-letter replacement values are request data, separate from the reusable template. Whether you retain those values or the generated PDF is an application retention decision.

Templates contain no loops/conditionals. Select a template version, assemble optional fragments, or construct programmatic tables in your application when business rules require dynamic structure. Do not concatenate unescaped recipient values into markup; use `.values(...)` for scalar data.

## 15. Country and stationery variations

Keep layout measurements separate from business content in application configuration: paper size, margins, header/address box, typography and assets can vary by organization, country or stationery. Do not infer a universal address-window location from a country name.

Fluent application configuration can select a layout before rendering the same body:

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

With stored templates, use scalar geometry placeholders:

```html
<letter page="${paper}" margins-mm="${margin}">
  <header x-mm="${addressX}" y-mm="${addressY}"
          width-mm="${addressWidth}" height-mm="${addressHeight}">
    <p>${recipientName}<br>${street}<br>${postalAddress}</p>
  </header>
  <main><p>${message}</p></main>
</letter>
```

Supply those keys along with recipient data in `.values(...)`. Use separate placeholders for address lines because template whitespace follows HTML rules. Format currency, dates, decimal separators and salutation in the application using the intended locale. Choose/register fonts that cover the actual language.

## 16. Watermarks

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

## 17. Advanced fixed elements and last-page signatures

Use immutable model records when you need more than the ordinary header/footer API: multiple address/sender boxes, different first/following-page content, explicit footer positioning, or a signature anchored at the bottom of the last page.

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
var address = new Paragraph(List.of(new TextRun("Ana Exemplo\nRua da Energia, 24", null, null)), null);
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

## 18. JSON input

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

JSON maps to the immutable model, not the HTML template format. It has no placeholder substitution pass. Enums use uppercase names. Block `type` values are `paragraph`, `heading`, `list`, `image`, `table`, `pageBreak`; watermark `kind` is `text` or `image`. Image `data` is Base64. Unknown fields/discriminators, duplicate keys, malformed JSON, trailing documents, scalar coercion, numeric enum ordinals and fractional schema versions are rejected.

Optional model fields may be omitted or null and use their documented defaults. Required fields produce a field-path error. See the [model constructor reference](#22-public-api-reference) and public records for full structured input. Do not treat external Java class names as JSON discriminators.

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

## 19. Application integration and concurrency

Build a generator once and reuse it. `LetterPdfGenerator` instances are safe for concurrent generation because each render owns its engine state. A no-argument generator needs no configuration; a custom-font generator retains immutable font bytes after its builder completes.

`LetterBuilder`, `TemplateBuilder`, `TableBuilder` and style builders are mutable request-building objects. Do not share one mutable builder between concurrent requests. Each document build produces an independent immutable snapshot. The library copies image data and collection contents; later changes to caller-owned data do not mutate a built letter.

Generation returns a `byte[]` only after validation, layout and any watermark pass finish. You can write it to a file, attach it to an application response, upload it to your storage, or pass it to another service. For an HTTP response, the application sets `Content-Type: application/pdf` and the desired download filename. Framework/servlet integration is application-owned.

Do not rely on temporary filesystem paths in a deployed JAR for classpath resources. Package fonts as resources and register them through `registerClasspathFont`; load application-owned assets through your resource/storage layer, then pass bytes. No font scanning, database connection, font auto-download or external image fetch is required. Supported public method signatures expose no FOP, PDFBox or Jackson engine types.

For a server/JVM without a display, set `-Djava.awt.headless=true` on the application's Java command. This is especially useful when the host sets a `DISPLAY` variable even though no display is accessible. The library does not change this global JVM setting. The project's rendering tests already run in headless mode.

Rendering uses memory for document state, assets and output bytes. Apply request size/concurrency budgets in the hosting application according to your workload. The library does not set a page-count limit or stream a partially generated PDF.

## 20. Errors and troubleshooting

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

## 21. Run the examples and inspect output

From the repository, run all checks and generate example PDFs:

```sh
./mvnw clean verify
```

Focused rendering examples:

```sh
./mvnw -Dtest=HeaderPlacementTest test
./mvnw -Dtest=EdpTemplateTest test
./mvnw -Dtest=EdpReferenceLetterTest test
```

| Test/example | Output under `target/examples/` |
| --- | --- |
| `HeaderPlacementTest` | `header-exact-position.pdf` and PNG |
| `EdpTemplateTest` | `edp-template-letter.pdf`, PNG, `template-assets/logo.png` and `footer.png` |
| `EdpReferenceLetterTest` | `edp-reference-letter.pdf`, `edp-fluent-letter.pdf` and PNGs |
| `TemplateMarkupTest` | `template-features.pdf` and PNG; repeated styled/image footer across two pages |
| `HeaderPlacementExamples` | `header-left.pdf`, `header-center.pdf`, `header-right.pdf`, `header-window.pdf` |

The reusable fixture sources are `src/test/java/com/nsdev/lettergenerator/fixtures/EdpReferenceLetter.java` and `FluentEdpLetter.java`. Edit these for the low-level/fluent EDP examples, or edit the HTML/values files for the stored-template example. `TableRenderingTest` separately checks styling, immutable input ownership and repeating table headers.

The EDP fixtures use fictional recipient/contract data and an approximate programmatically drawn logo; they recreate content/layout rather than embedding the reference screenshot. Editable template/data are [edp-price-update.html](examples/templates/edp-price-update.html) and [edp-values.json](examples/templates/edp-values.json).

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
java -cp "target/example-classes:target/letter-generator-1.0.0-SNAPSHOT.jar:$(cat target/classpath.txt)" GenerateTemplateLetter
```

`GenerateTemplateLetter` uses the test-produced assets, editable EDP template and test-font paths by default; run the EDP test first or supply its template/data/asset paths. Its five optional arguments are template HTML, values JSON, logo PNG, footer PNG and output PDF. `GenerateLetter` optionally accepts JSON path and output path. `HeaderPlacementExamples` optionally accepts an output directory. On Windows adapt shell syntax and use `;` as the Java classpath separator.

Open a generated PDF in your PDF viewer, for example `xdg-open target/examples/header-right.pdf` on a Linux desktop. Compare printed address-window placement at actual size. Do not judge exact physical coordinates from a viewer's zoomed screenshot.

## 22. Public API reference

Public packages start with `com.nsdev.lettergenerator`. The `internal` subtree is implementation detail; applications should not import it.

### Entry points and generator

| API | Purpose |
| --- | --- |
| `Letter.builder()` | New mutable letter builder |
| `Letter.template(String)` | New template builder with map substitution |
| `Letter.fromHtml(String)` | New template builder for already-resolved markup |
| `Letter.PAGE_BREAK` | Form-feed marker for plain body strings |
| `new LetterPdfGenerator()` | Reusable generator with built-in fonts |
| `LetterPdfGenerator.builder()` | Optional custom-font configuration |
| `generator.generate(LetterDocument)` | Validate/render and return complete PDF bytes |
| `generator.generateJson(String)` | Parse strict JSON, validate/render |
| `Table.builder(String... headers)` | Convenience table builder |

Generator builder methods: `registerFont(String, FontVariant, Path)`, `registerClasspathFont(String, FontVariant, String)`, `build()`.

### LetterBuilder

| Method | Effect |
| --- | --- |
| `page(PageSize)` | Select A4 or LETTER |
| `marginsMm(double)`, `margins(Margins)` | Uniform or independent page margins |
| `style(Style)` | Document text/paragraph defaults |
| `body(String...)` | Append literal plain paragraphs/line changes/page markers |
| `heading(String)` | Append a heading |
| `pageBreak()` | Append a body page transition |
| `table(TableBlock)`, `block(Block)` | Append typed content |
| `bodyHtml(String)` | Append formatted fragment |
| `header(String...)`, `header(Path/byte[])` | Replace header with text or image |
| `headerHtml(String)`, `headerBlocks(Block...)` | Replace header with formatted/typed content |
| `headerAlignment(TextAlign)` | Left/center/right box and default content alignment |
| `headerWidthMm(double)`, `headerHeightMm(double)` | Set or resize the header box |
| `headerBoundsMm(double x, double y, double width, double height)` | Exact header box relative to inner-page origin |
| `headerStyle(Style)` | Header default content style |
| `footer(String...)`, `footer(Path/byte[])` | Replace footer with text or image |
| `footerHtml(String)`, `footerBlocks(Block...)` | Replace footer with formatted/typed content |
| `footerHeightMm(double)`, `footerStyle(Style)` | Footer size/default style |
| `logo(Path/byte[]/ImageBlock)` | Set a separate top-left logo |
| `signature(SignatureContent...)` | Replace signature with stacked text/image parts |
| `signatureHtml(String)`, `signatureBlocks(Block...)` | Replace signature with formatted/typed content |
| `watermark(String/Watermark)` | Set text-default or explicit watermark |
| `asset(String, Path/byte[])` | Snapshot a named image asset |
| `fixedElement(FixedElement)` | Append advanced fixed content to configured zones |
| `build()` | Produce an immutable LetterDocument snapshot |

Configuration calls replace their option; body and advanced fixed-element calls append. Repeated region content calls replace that region. Width/height setters after exact header bounds preserve its x/y. Builders are mutable; use a new instance per concurrent request.

### Templates, tables and signatures

| API | Methods/options |
| --- | --- |
| `TemplateBuilder` | `values(Map<String, ?>)`, `asset(String, Path/byte[])`, `build()`; `FORMAT` constant |
| `TableBuilder` | `row(String...)`, `columnWeights(double...)`, `style(Style)`, `headerStyle(Style)`, `headerBackground(String)`, `build()` |
| `Style.builder()` | Family, size, color, emphasis booleans, alignment, line-height and before/after spacing |
| `TextSignature.of(String)` | Optional `.style(Style)`, immutable result |
| `ImageSignature.of(Path/byte[])` | Optional `.sizeMm(width, height)`, immutable result |
| `TextWatermark.of(String)` | Standard centered text watermark |
| `ImageWatermark.of(Path/byte[])` | Standard centered image watermark |

### Immutable model constructors

| Package/type | Fields in constructor order |
| --- | --- |
| `model.LetterDocument` | `schemaVersion, page, defaultStyle, sections, fixedElements, signature, watermark` |
| `model.Section` | `name, style, blocks` |
| `model.layout.PageSpec` | `size, marginsMm, topZoneMm, bottomZoneMm, signatureZoneMm` |
| `model.layout.Margins` | `top, right, bottom, left` |
| `model.layout.Bounds` | `x, y, width, height` |
| `model.layout.FixedElement` | `id, zone, pages, boundsMm, blocks` |
| `model.layout.Signature` | `placement, blocks, boundsMm` |
| `model.block.Paragraph`, `Heading` | `runs, style` |
| `model.block.ListBlock` | `items, ordered, style` |
| `model.block.ImageBlock` | `data, mimeType, widthMm, heightMm, style` |
| `model.block.TableBlock` | `columnWeights, header, rows, style` |
| `model.block.PageBreakBlock` | No fields; body only |
| `model.table.TableCell` | `runs, style, backgroundColor` |
| `model.text.TextRun` | `text, style, href` |
| `model.style.Style` | `fontFamily, fontSizePt, color, bold, italic, underline, textAlign, lineHeight, spaceBeforeMm, spaceAfterMm` |
| `model.watermark.TextWatermark` | `text, fontFamily, fontSizePt, color, pages, opacity, rotationDegrees` |
| `model.watermark.ImageWatermark` | `data, mimeType, widthMm, heightMm, pages, opacity, rotationDegrees` |

Use schema version `1`. Null optional style properties inherit. PageSpec null fields select defaults. Sections preserve input order; their names are labels without layout meaning. Every box requires finite nonnegative x/y and positive dimensions. Required content/fields are validated during generation; constructing a record is not a guarantee that it is renderable. Image array data has defensive copying and content-based equality.

## 23. Supported scope and limits

The supported scope is styled PDF letters with native text, flow pagination, PNG/JPEG content, tables, fixed letter regions, visible signatures and optional text/image watermarks. The library checks geometry, resource references, font variants/glyphs, and rendering overflow, including painted glyph/underline bounds that can exceed logical line boxes. It does not shrink text silently; oversized images and nonbreaking text may require smaller dimensions or different content. It returns no partial PDF on failure.

HTML input supports the finite vocabulary above. General CSS/browser layout, arbitrary fonts from template URLs, scripts, classes/stylesheets, SVG, loops, conditionals, nested table blocks/spans, automatic postal-standard compliance, DOCX output, cryptographic signing, automatic page numbers and PDF conformance profiles such as PDF/A are not exposed by this MVP. Use application logic or a future explicit feature for those requirements; do not assume browser behavior.

For an issue report, include a minimal template/model, scalar values, required font/image assets with permission to share, the Java/library version, and the complete exception including its cause. Use fictional recipient data where possible. `./mvnw clean verify` is the project's full acceptance command; examples and previews are generated under `target`, so clean builds remove and recreate them.

## 24. Development and code quality

```sh
./mvnw spotless:apply   # Apply the checked-in Java formatting convention
./mvnw clean verify     # Check format, compile with all warnings as errors, run tests and Checkstyle
```

Java source, tests, and the standalone example use four-space AOSP formatting with explicit imports. Checkstyle checks production and test code for braces, single statements, imports, naming, and other basic correctness rules. Production domain values must be named constants; fixture input/expected values remain literal. Dependency and formatter versions are pinned in the POM. PDFBox, FontBox, and PDFBox IO are aligned through dependency management; Maven Enforcer rejects PDF dependency downgrades.

The package layout separates responsibilities:

| Package under `com.nsdev.lettergenerator` | Responsibility |
| --- | --- |
| root, `builder`, `template`, `signature`, `model.*`, `font`, `exception` | Supported application contract |
| `internal.markup`, `internal.template` | Strict document markup, map binding and template composition |
| `internal.validation`, `internal.json` | Field-path validation and strict JSON parsing |
| `internal.font`, `internal.style`, `internal.layout` | Immutable font snapshots, inherited styles, dimensions and units |
| `internal.render.fo` | Structured FO block/style/page-layout serialization |
| `internal.render.fop` | FOP configuration, diagnostics, and glyph-bound checks |
| `internal.render.watermark` | Optional PDFBox processing |
| `internal.resource`, `internal.xml` | In-memory resource allowlist and secure XML handling |

The test suite separates shared fixtures from integration tests. Image records provide byte-content equality/hash codes in addition to defensive copying. Pagination recovery uses typed diagnostics and retries only recoverable body overflow or a missing last-page master. Font, fixed-box, and parsing failures do not trigger a broad retry.

See [CONTRIBUTING.md](CONTRIBUTING.md) for contributor conventions. User-facing instructions and API examples are maintained in this README. Internal plans and implementation notes are kept locally under the ignored `docs/` directory.
