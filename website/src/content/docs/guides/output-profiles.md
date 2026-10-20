---
title: "Metadata, tagged output and PDF/A"
description: "Letter Generator usage: metadata, tagged output and pdf/a."
---

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
