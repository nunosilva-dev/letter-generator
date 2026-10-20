---
title: Build and verify these docs
description: Maintainer instructions for reproducible documentation, runnable examples, public Javadocs and static hosting.
---

This page is for maintainers with an authorized source checkout. Application users can use the [downloadable example project](/downloads/letter-generator-examples.zip) without access to the library implementation.

## Prepare the verified artifacts

Use JDK 17 for the library acceptance run:

```sh
./mvnw clean verify
```

This regenerates tested PDFs/previews under `target/examples` and public Javadocs under `target/apidocs`. Documentation tests compile complete public sample programs and check their PDF text/page count.

The website needs Node >=22.22.0 and pnpm 11.25.0. From `website/`:

```sh
pnpm install --frozen-lockfile
pnpm verify
pnpm dev
```

`verify` runs script tests, prepares approved generated assets/downloads, checks reference coverage, builds Starlight and its search index, then checks local links/assets and fragment anchors. Missing Maven outputs fail with a preparation command rather than leaving broken gallery entries. For a production-output preview, use `pnpm preview` after building.

## What becomes public

`website/dist` is the static hosting output. The build includes authored documentation, approved example PDFs/images, runnable **example** source/resources and public Javadocs. It rejects internal Javadoc packages and excludes tool option files with local classpaths. It does not copy library implementation sources into the example archive.

The library's Maven source artifact is a separate publication setting. Building this site does not change that setting, publish the library, change a repository's visibility or deploy the website.

## Keep examples accurate

Canonical complete quickstarts live under `src/test/resources/documentation`. The site imports those sources directly; changes are checked by `DocumentationExamplesTest`. The energy inputs/assets and Java factories remain test fixtures and feed both accepted renders and the downloadable consumer project.

The tag/CSS/attribute catalogue is checked against the parser's explicit vocabulary. JSON fields/enums are generated from reachable public record declarations and their documented contracts. When a public feature changes, update its focused guide/reference and the relevant observable test, then run both Java and website verification.

## Hosting and versions

The build serves from a domain root and can be hosted as static files without a Java server. A live editable rendering playground is not part of this site. The displayed documentation version comes from the library POM; preserve release-specific builds when multiple supported releases exist. There is currently a single checkout version rather than a selector containing unavailable versions.
