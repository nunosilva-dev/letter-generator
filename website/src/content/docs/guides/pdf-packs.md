---
title: "Append existing PDFs"
description: "Letter Generator usage: append existing pdfs."
---

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
