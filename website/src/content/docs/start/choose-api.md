---
title: Choose an API
description: Compare the four input routes and choose the most comfortable authoring model for your application.
sidebar:
  order: 3
---

All four routes produce a `LetterDocument` and use the same generator. Choose by how you want to author content; rendering quality is not tied to one route.

| API | Best starting point when… | Authoring responsibility |
| --- | --- | --- |
| [Fluent Java](/apis/fluent/) | Application code assembles letters and you want convenient defaults and chains | Build content/regions with `Letter.builder()`; use typed values for advanced details |
| [Typed Java](/apis/typed/) | You need explicit immutable structure or another tool constructs the model | Construct page, section, block, style and region values directly |
| [JSON](/apis/json/) | Content arrives as a serialized document model | Supply strict schema-version-1 JSON with exact property/enum names |
| [HTML templates](/apis/html/) | You prefer authored markup and binding a key/value map | Use the supported tags/CSS, register named assets, and bind literal values |

## How the routes connect

- `Letter.builder().build()` creates a document from fluent calls.
- Typed constructors create that document directly.
- `Letter.fromJson(json)` parses the shared model. `generateJson`/`renderJson` also render it.
- `Letter.template(source).values(values).build()` binds markup and creates the model.
- `Letter.fromHtml(resolvedMarkup).build()` compiles already-resolved markup without template directives.
- `generator.generate(document)` returns PDF bytes; `generator.render(document)` also returns page counts and diagnostics.

Themes/components and output settings act on the shared model. Literal Java text does not parse HTML or substitute placeholders; JSON has no template-expression engine.

## Differences to know before choosing

HTML deliberately exposes a finite tag/attribute vocabulary, currently including uniform root margins. Independent per-side margins and some advanced settings require Java model/builder configuration. Appended PDFs can be added to an HTML-generated model with `withAttachments(...)`; there is no attachment-fetching HTML tag.

A fluent builder can accept typed blocks, so adopting the convenient API does not require rebuilding the entire letter when one part needs explicit model control.

Start with the [same minimal letter through all four APIs](/start/quickstart/), then compare the [complete energy example](/examples/energy-letter/).
