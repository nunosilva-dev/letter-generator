---
title: "Advanced fixed elements and last-page signatures"
description: "Letter Generator usage: advanced fixed elements and last-page signatures."
---

Use immutable model records when you need more than the ordinary header/footer API: multiple address/sender boxes, different first/following-page content, explicit footer positioning, or custom bounds within a last-page signature zone.

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
var address = new Paragraph(List.of(new TextRun("Cliente Exemplo\nMorada Fictícia, 00", null, null)), null);
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
