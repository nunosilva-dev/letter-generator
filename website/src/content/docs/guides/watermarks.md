---
title: "Watermarks"
description: "Letter Generator usage: watermarks."
---

```java
var draft = Letter.builder().body("Body").watermark("DRAFT").build();
```

The text factory uses Helvetica 50 pt, gray, 0.12 opacity, 45° counterclockwise rotation, centered on all pages. For explicit options:

```java
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.model.watermark.ImageWatermark;

var watermark = new TextWatermark("DRAFT", "Helvetica", 50d, "#B0B0B0",
    PageSelector.FIRST, 0.15, 35d);
var letter = Letter.builder().body("Body").watermark(watermark).build();
```

`ImageWatermark.of(Path/byte[])` supplies an image watermark with defaults; the explicit record sets box, selector, opacity and rotation. A template uses:

```html
<watermark opacity="0.12" rotation="45" pages="ALL"
           style="font-size:50pt;color:#bbb">DRAFT</watermark>
```

For an image watermark, put one named `img` inside `watermark` and omit text. Put image styles/dimensions on that `img`.

Watermarks are drawn behind existing PDF content, consume no layout space, and may be covered by opaque images/table backgrounds. Text watermarks use regular undecorated fonts. Vertical centering uses cap height, so lowercase/descender-heavy text is approximate. Omitting a watermark skips the PDFBox post-processing pass entirely.
