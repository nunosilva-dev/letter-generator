---
title: "Reusable themes and components"
description: "Letter Generator usage: reusable themes and components."
---

Supply immutable themes and components per document, and reuse them across requests. Create a new mutable letter builder for each letter.

### Role defaults and named styles

```java
import com.nsdev.lettergenerator.theme.LetterTheme;
import com.nsdev.lettergenerator.model.style.Style;

var company = LetterTheme.builder()
    .body(Style.builder().fontFamily("Helvetica").fontSizePt(11)
        .color("#24363D").spaceAfterMm(3).build())
    .heading(Style.builder().fontSizePt(16).bold().build())
    .header(Style.builder().fontSizePt(10).build())
    .footer(Style.builder().fontSizePt(8).color("#666666").build())
    .signature(Style.builder().fontSizePt(11).build())
    .tableHeader(Style.builder().bold().color("#FFFFFF").build())
    .named("notice", Style.builder().bold().color("#10384A").build())
    .named("urgent", Style.builder().named("notice").underline().build())
    .build();

var letter = Letter.builder()
    .theme(company)
    .heading("Confirmation")
    .bodyHtml("<p style-name='notice'>Your request has been accepted.</p>")
    .bodyHtml("<p style-name='urgent' style='font-weight:normal'>Please retain this letter.</p>")
    .build();
```

The available `StyleRole` values are BODY, HEADING, TABLE, TABLE_HEADER, TABLE_CELL, HEADER, FOOTER, SIGNATURE, SENDER, RECIPIENT, DATE, SUBJECT, REFERENCE, SALUTATION and CLOSING. `role(role, style)` registers any role; corresponding convenience methods are available on the theme builder. BODY is the fallback for roles without their own properties. Font registration belongs to `LetterPdfGenerator`. Themes select registered families, typography, text colors and block spacing. Reuse `TableOptions`/`CellAppearance` values for table fills, borders and padding; set a suitable header background when choosing a light header text color.

The cascade resolves built-in/semantic defaults, BODY and block-role defaults, contextual header/footer/signature/table-cell defaults, explicit document/section/container properties, a local named style, and finally local properties. Each property inherits separately; explicit false and zero values win. A contextual footer role therefore applies to paragraphs in the footer, and an explicit paragraph style overrides it. Inline text overrides the containing paragraph. Group margins and keeps apply to the group; its typography inherits into children.

Set `.theme(...)` on the builder before `.build()` so the theme participates when convenience defaults are assembled. `LetterDocument.withTheme(...)` is useful for typed models: it preserves styles already stored in the existing snapshot, including defaults materialized by an earlier builder. Changing a theme never mutates previously built documents.

For typed content, set `Style.builder().named("notice").bold(false).build()` on the block or run. For HTML, use `style-name="notice"` plus optional local CSS. Named styles may inherit another named style through `.named(...)`; unknown references, blank names and cycles are errors, including unused registered references. There are no filesystem or URL includes.

### Reuse headers, addresses, signatures and legal text

```java
import com.nsdev.lettergenerator.component.LetterComponent;
import com.nsdev.lettergenerator.model.block.ComponentBlock;

var address = LetterComponent.text("Example Company\n100 Example Avenue\nSample City");
var legal = LetterComponent.text("Example Company | example.invalid");
var signature = LetterComponent.text("Kind regards,\nCustomer Services");

var letter = Letter.builder()
    .theme(company)
    .header(address)
    .footer(legal)
    .signature(signature)
    .component("legalNotice", legal)
    .body("Dear Alex,")
    .include("legalNotice")
    .build();
```

`LetterComponent.of(Block...)` wraps any supported model content, including styled text, images and tables. `text(String...)` creates one paragraph per argument; a newline stays within that paragraph. Direct `body(component)` groups its content softly; header/footer/signature overloads reuse it in those regions. Use `new ComponentBlock("legalNotice", localStyle)` for a styled named reference anywhere blocks are accepted.

Named references use the document's component map. Components can reference other components; generation rejects unknown names, cycles and content invalid for its final location. A component with a page break cannot be inserted in a table cell or signature. References normally form softly kept groups; the reference's explicit keep can override that policy. Registrations and content lists are snapshotted, and images retain their defensive byte ownership.

```java
var template = Letter.template("""
    <letter>
      <header><component ref="address"></component></header>
      <main><h1 style-name="notice">Hello ${recipient}</h1>
        <component ref="legalNotice" style="font-size:9pt"></component></main>
      <footer><p>Page <page-number></page-number> of <page-count></page-count></p></footer>
    </letter>
    """)
    .theme(company)
    .component("address", address)
    .component("legalNotice", legal)
    .values(Map.of("recipient", "Alex Example"));
var letter = template.build();
```

`component` and page-field tags require paired closing tags; they are empty elements in this format, not HTML void tags. `style-name` belongs to content styles; logo wrappers and watermark wrappers require their existing explicit configuration.

### JSON equivalents

JSON carries the same composition values, with enum names in uppercase. This complete example uses a named style and a reusable fragment:

```json
{
  "schemaVersion": 1,
  "theme": {
    "roles": {"BODY": {"fontSizePt": 11}, "HEADING": {"fontSizePt": 16, "bold": true}},
    "namedStyles": {"notice": {"color": "#10384A", "bold": true}}
  },
  "components": {
    "legal": {"blocks": [{"type": "paragraph", "runs": [{"text": "Example Company | example.invalid"}]}]}
  },
  "sections": [{
    "name": "Body",
    "blocks": [
      {"type": "heading", "runs": [{"text": "Confirmation"}], "style": {"styleName": "notice"}},
      {"type": "group", "style": {"keepTogether": "ALWAYS"}, "blocks": [
        {"type": "paragraph", "runs": [{"text": "Dear Alex,"}]},
        {"type": "component", "name": "legal", "style": {"bold": false}}
      ]}
    ]
  }]
}
```

A rich JSON cell uses `blocks` instead of `runs`, with optional `columnSpan`, `rowSpan` and `appearance`. Table `options` holds `cellAppearance`, `rowKeepTogether`, `rowPolicies` and `rowGroups`:

```json
{
  "type": "table",
  "columnWeights": [2, 1],
  "header": [{"runs": [{"text": "Combined heading"}], "columnSpan": 2}],
  "rows": [[{"blocks": [{"type": "paragraph", "runs": [{"text": "Rich content"}]}], "columnSpan": 2}]],
  "options": {
    "cellAppearance": {
      "border": {"widthPt": 0.5, "style": "SOLID", "color": "#B8C9CF"},
      "padding": {"topMm": 2, "rightMm": 2, "bottomMm": 2, "leftMm": 2},
      "verticalAlignment": "TOP"
    },
    "rowKeepTogether": "PREFER",
    "rowPolicies": [{"rowIndex": 0, "keepTogether": "ALWAYS"}],
    "rowGroups": [{"startRow": 0, "rowCount": 1, "keepTogether": "PREFER"}]
  }
}
```
