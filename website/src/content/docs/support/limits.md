---
title: "Supported scope and limits"
description: "Letter Generator usage: supported scope and limits."
---

The supported scope is styled PDF letters with native text, flow pagination, PNG/JPEG/static SVG content, rich tables, bounded canvases, columns, decorated boxes, nested lists, reusable themes/stationery, fixed letter regions, visible signatures and optional text/image watermarks. Typed HTML templates support finite conditions, repeats and formatted parameters; prepared templates capture immutable authoring configuration. Optional metadata, native tagged/PDF/A output and standard-profile PDF packs are described below. The library checks geometry, resource references, font variants/glyphs, and rendering overflow, including painted glyph/underline bounds that can exceed logical line boxes. It does not shrink text silently; oversized images and nonbreaking text may require smaller dimensions or different content. It returns no partial PDF on failure.

HTML input supports the finite vocabulary above. General CSS/browser layout, arbitrary fonts from template URLs, scripts, classes/stylesheets, nested tables, inline images inside text runs, automatic postal-standard compliance, DOCX output and cryptographic signing are not exposed. Tagged output does not claim PDF/UA compliance. PDF/A-2b uses the native renderer and has explicit restrictions; see [output profiles](/guides/output-profiles/). Use application logic or a future explicit feature for those requirements; do not assume browser behavior.

For an issue report, include a minimal template/model, typed binding data, required font/image assets with permission to share, the Java/library version, and the complete exception including its cause. Use fictional recipient data where possible. `./mvnw clean verify` is the project's full acceptance command; examples and previews are generated under `target`, so clean builds remove and recreate them.
