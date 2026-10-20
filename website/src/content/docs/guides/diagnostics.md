---
title: "Validate, diagnose and preview layout"
description: "Letter Generator usage: validate, diagnose and preview layout."
---

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

Inspection parses and binds literal values, then aggregates **missing scalar placeholder occurrences and invalid/missing referenced image assets** in active branches and repeat clones. Directive/parameter failures retain a first-error boundary; see [prepared templates](/guides/prepared-templates/). It does not perform environment/system/URL lookup, recursive replacement or markup injection. Asset errors caused by a missing source variable are not duplicated as missing-asset errors. Once that preflight passes, compilation and semantic validation report the first remaining model error, including registered-font and declared-geometry checks. It generates no FO or final PDF.

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
