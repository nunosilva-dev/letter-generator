---
title: "Images and named assets"
description: "Letter Generator usage: images and named assets."
---

The supported image formats are PNG, JPEG and **static SVG** (`image/svg+xml`). Existing image entry points accept all three: body, table cells, positioned content, header, footer, logo, signature and image watermark. Image bytes are copied, format/MIME is checked, and aspect ratio is retained within the requested box. Data URLs, remote image URLs and image paths inside markup remain unsupported; resolve trusted paths in Java or supply named byte assets.

SVG requires the SVG namespace and positive finite intrinsic dimensions, or a usable `viewBox`. Absolute SVG dimensions support px/mm/cm/in/pt/pc. Relative/percentage viewport lengths are unsupported; omit those dimensions and provide a valid viewBox instead. Static shapes, text, groups, local `use` references, gradients, clipping, masks and patterns are accepted. Scripts, animation, filters, `foreignObject`, embedded/external images, stylesheets/imports, entities/DTDs, processing instructions and external resource references are rejected before rendering. URL references must be local fragments. SVG is limited to 2 MB, 10,000 XML nodes, depth 64 and a bounded local-reference expansion. Unusual intrinsic dimensions outside the documented supported geometry fail rather than overflow the fitting calculation.

Ordinary SVG geometry goes through native FOP/Batik PDF rendering, retaining vectors where supported; some paint effects may be rasterized by the renderer. SVG watermarks use a bounded PNG fallback at 150 dpi with a maximum side of 2048 pixels. PNG/JPEG handling is retained.

Use `image.withAltText("Company logo")`, or `<img ... alt="Company logo">`, to supply an image description for tagged output. Descriptions survive ordinary theme preparation and image refitting.

Full template or fragment:

```java
var letter = Letter.builder()
    .bodyHtml("<img src='asset:photo' width='60mm' height='40mm'>")
    .asset("photo", Path.of("assets/photo.jpg"))
    .build();
```

The application reads a trusted path through `.asset(name, Path)`, or supplies already-loaded bytes. The markup identifier `asset:photo` only looks in that supplied asset map; it cannot fetch a path or URL. Assets can be registered after fragment content calls because resolution happens during `build()`.

Image width/height attributes or CSS properties accept `mm`, `pt`, `px`, and percentages of the region's width/height. Unitless image attributes are pixels, with 96 px per inch. An image box with both dimensions supplied retains aspect ratio and may leave unused space on one axis. Unspecified body images fit a 60×40 mm box; header/footer images fit region defaults. Give explicit dimensions when the exact visual size matters. Images must stand alone; image-only paragraphs are accepted, mixed inline text/image paragraphs are not.

Typed body image:

```java
import com.nsdev.lettergenerator.model.block.ImageBlock;

var image = new ImageBlock(pngBytes, "image/png", 60d, 40d,
    Style.builder().textAlign(TextAlign.CENTER).spaceAfterMm(3).build());
var letter = Letter.builder().block(image).build();
```

The `d` suffix is useful for record fields typed as boxed `Double`. `new ImageBlock(..., 60d, 40d, ...)` supplies an explicit box, while convenience factories fit dimensions for you.
