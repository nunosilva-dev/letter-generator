---
title: "Public API reference"
description: "Letter Generator usage: public api reference."
---

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
