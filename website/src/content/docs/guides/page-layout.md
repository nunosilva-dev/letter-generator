---
title: "Paper, margins, units and layout"
description: "Letter Generator usage: paper, margins, units and layout."
---

Useful imports for geometry/style examples:

```java
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
```

| Setting | Default | Customization |
| --- | --- | --- |
| Paper | `PageSize.A4`, 210 × 297 mm | `page(PageSize.LETTER)`, 215.9 × 279.4 mm |
| Margins | 20 mm on each side | `marginsMm(15)` or `margins(new Margins(top, right, bottom, left))` |
| Header content height | 25 mm when present | `headerHeightMm(...)` or exact header bounds |
| Footer content height | 15 mm when present | `footerHeightMm(...)` |
| Body/header and body/footer separation | 5 mm | Advanced `PageSpec` for complete zone control |
| Body paragraph spacing | 3 mm after paragraphs | `style(Style.builder().spaceAfterMm(...).build())` |
| Text size | 11 pt | `fontSizePt(...)` |

`Margins` constructor order follows top, right, bottom, left:

```java
var letter = Letter.builder()
    .page(PageSize.LETTER)
    .margins(new Margins(15, 18, 17, 22))
    .body("Custom stationery")
    .build();
```

Page/box dimensions and spacing use **millimeters**. Font sizes use **points**. A line-height value is a **multiplier**, so 11 pt with 1.2 gives a 13.2 pt line box. Rotation uses degrees and opacity uses a value from 0 to 1.

The margins form the inner page. Header/logo content occupies a reserved top zone, footer content occupies a bottom zone, and the body flows between them. The library reserves each zone's full width; text does not wrap beside fixed elements. Empty fluent header/footer regions reserve no space unless advanced fixed elements use those zones.

`PageSpec.A4` and `PageSpec.LETTER` are immutable advanced-model presets with default margins and zero reserved zones. `Letter.builder()` creates the zones needed by its header/footer calls. This difference matters when constructing `LetterDocument` directly.
