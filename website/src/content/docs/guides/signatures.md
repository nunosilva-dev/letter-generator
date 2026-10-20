---
title: "Signatures"
description: "Letter Generator usage: signatures."
---

```java
import com.nsdev.lettergenerator.signature.ImageSignature;
import com.nsdev.lettergenerator.signature.TextSignature;

var letter = Letter.builder()
    .body("With our best regards,")
    .signature(
        ImageSignature.of(signatureBytes).sizeMm(35, 15),
        TextSignature.of("Jane Smith\nCustomer Services")
            .style(Style.builder().fontSizePt(10).build()))
    .build();
```

Signature parts stack in argument order and flow after the body. The group stays together when it fits. `TextSignature` understands plain newlines and blank-line paragraphs. `ImageSignature.of(Path/byte[])` defaults to a 40×20 mm fit box; `sizeMm` returns a new immutable size override.

Use `signatureHtml(...)` for rich markup or `signatureBlocks(...)` for explicit blocks. Signature content may contain images and styled text; it cannot contain page breaks. A template uses `<signature>...</signature>`.

To anchor the signature above the footer on the final page, select the existing `BOTTOM_LAST` preset. The fluent builder reserves the full inner-page width and the configured height automatically:

```java
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;

var letter = Letter.builder()
    .body("Your letter content")
    .signaturePlacement(SignaturePlacement.BOTTOM_LAST)
    .signatureHeightMm(25)
    .signature(TextSignature.of("Jane Smith\nCustomer Services"))
    .footer("Example Company")
    .build();
```

The default placement is `FLOW`. `signatureHeightMm` defaults to 20 mm and must be finite and positive. With `BOTTOM_LAST`, the builder reserves that height only when signature content is present. Signature HTML also uses it as the basis for default and percentage-based image heights. Explicit signature parts and typed blocks keep their own dimensions; resize an image part with `ImageSignature.sizeMm(...)`. Placement and height options can be set before or after signature content. If the final body content cannot fit beside the reserved signature area, an additional last page may be needed.

The equivalent template configuration is:

```html
<signature placement="bottom-last" height-mm="25">
  <p>Jane Smith<br>Customer Services</p>
</signature>
```

Both attributes are optional: `placement` defaults to `flow`, and `height-mm` to 20. Styles and image content work in either placement. For explicit signature bounds through the typed model or JSON, see [fixed regions](/guides/fixed-regions/). Image/text signatures here are visible content, not cryptographic PDF signatures.
