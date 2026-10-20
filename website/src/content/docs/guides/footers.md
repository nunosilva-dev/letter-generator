---
title: "Footers and logos"
description: "Letter Generator usage: footers and logos."
---

Plain footers:

```java
var letter = Letter.builder()
    .body("Body")
    .footer("Example Company | example.com\nRegistered office: Lisboa")
    .footerStyle(Style.builder().fontSizePt(8).color("#666666")
        .textAlign(TextAlign.CENTER).build())
    .footerHeightMm(18)
    .build();
```

Default footer text uses 9 pt and `#666666`. Header/footer paragraphs default to zero paragraph spacing. Footer content repeats on every page. Increasing footer height reduces the available body area and preserves the 5 mm gap above the footer.

An image is valid footer content:

```java
var letter = Letter.builder().body("Body").footer(footerBytes).build();
```

Use a rich fragment to combine images and text:

```java
var letter = Letter.builder()
    .body("Body")
    .footerHtml("""
        <img src="asset:banner" width="100%" height="6mm">
        <p style="font-size:7pt;text-align:center;margin-top:1mm">${literal}</p>
        """)
    .asset("banner", footerBytes)
    .footerHeightMm(15)
    .build();
```

This fragment does not substitute `${literal}`; it prints that text. Use the full template API when replacements are needed. A footer may also contain a table or explicit immutable blocks through `footerBlocks(...)`.

`.logo(Path/byte[])` fits a PNG/JPEG/static SVG within 45×20 mm and places it at the top-left of the inner page, beside the automatic header. `.logo(ImageBlock)` supplies explicit image dimensions/style. The separate logo region repeats on every page. To place a brand image within an exact header rather than beside it, include that image in header content and omit the separate logo call.
