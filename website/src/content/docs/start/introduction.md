---
title: Introduction
description: Understand the document model, workflow and supported scope before choosing an API.
sidebar:
  order: 1
---

Letter Generator is a Java 17 library for producing PDF letters. Applications provide authored content, styles, data and assets; the library composes the pages and returns complete PDF bytes. It requires no web framework, database or hosted rendering service.

The library, documentation and examples use [Apache License 2.0](/LICENSE.txt). Dependencies and example fonts retain their own licences and notices.

The central workflow is **author → build a document → validate or render → save the result**. A built document is an immutable snapshot. Rendering is explicit: `build()` does not write a PDF or establish that the content fits.

You can author with [fluent Java](/apis/fluent/), the [typed model](/apis/typed/), [JSON](/apis/json/) or [letter HTML](/apis/html/). Those routes share pagination, themes, fonts, validation and PDF output. HTML adds literal placeholders and finite data directives; it is a controlled letter vocabulary rather than a browser renderer.

## What you can build

- Letters with logos, rich headers/footers and separate first/continuation stationery.
- Styled text, links, tables with spans/rich cells, nested lists and reusable components.
- Company themes, localized values, configured writing direction and registered Unicode fonts.
- Bounded positioned canvases, independent columns and decorated flowing boxes.
- Final-page text/image signatures, watermarks, metadata and optional PDF packs.

[Inspect real generated examples](/examples/gallery/) or [read the supported limits](/support/limits/).

## Defaults and ownership

Start with `new LetterPdfGenerator()` for engine-provided Helvetica, Times and Courier, without supplying font files. Broader character coverage and embedded archival fonts require [explicit registration](/guides/fonts/). Test/example DejaVu fonts are not production defaults.

Reuse a configured generator concurrently. Confine mutable builders to their authoring thread; completed documents and prepared templates snapshot supported collections and assets. A new PDF is generated for each render call.

## Documentation version

The version shown in the header comes from the library POM. These pages describe that exact checkout. The version does not imply Maven Central availability. Release-specific documentation can be retained when supported releases become available.

Continue with [installation](/start/installation/) and [your first letter](/start/quickstart/).
