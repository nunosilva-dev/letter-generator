---
title: "Application integration and concurrency"
description: "Letter Generator usage: application integration and concurrency."
---

Build a generator once and reuse it. `LetterPdfGenerator` instances are safe for concurrent generation because each render owns its engine state. A no-argument generator needs no configuration; a custom-font generator retains immutable font bytes after its builder completes.

`LetterBuilder`, `HtmlLetterBuilder`, `TableBuilder` and style builders are mutable request-building objects. Do not share one mutable builder between concurrent requests. Each document build produces an independent immutable snapshot. The library copies image data and collection contents; later changes to caller-owned data do not mutate a built letter.

Generation returns a `byte[]` only after validation, layout and any watermark pass finish. Pass the returned bytes to the caller. For an HTTP response, the application sets `Content-Type: application/pdf` and the desired download filename. Framework/servlet integration is application-owned.

Do not rely on temporary filesystem paths in a deployed JAR for classpath resources. Package fonts as resources and register them through `registerClasspathFont`; supply image assets as bytes or through the supported path-taking methods. No font scanning, database connection, font auto-download or external image fetch is required. Supported public method signatures expose no FOP, PDFBox or Jackson engine types.

For a server/JVM without a display, set `-Djava.awt.headless=true` on the application's Java command. This is especially useful when the host sets a `DISPLAY` variable even though no display is accessible. The library does not change this global JVM setting. The project's rendering tests already run in headless mode.

Rendering uses memory for document state, assets and output bytes. Apply request size/concurrency budgets in the hosting application according to your workload. The library does not set a page-count limit or stream a partially generated PDF.
