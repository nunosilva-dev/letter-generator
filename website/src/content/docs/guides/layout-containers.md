---
title: "Canvases, columns, boxes and custom paper"
description: "Letter Generator usage: canvases, columns, boxes and custom paper."
---

Use ordinary body flow for text that should paginate. Use a canvas when an address panel, reference field or other region needs explicit coordinates. A canvas consumes a fixed height in flow and moves as one indivisible unit. Coordinates are millimeters relative to its upper-left corner. Stable element IDs and explicit bounds also make this model suitable for a future visual editor; the library currently provides rendering and model data.

```java
import com.nsdev.lettergenerator.Canvas;
import com.nsdev.lettergenerator.model.layout.Bounds;

var panel = Canvas.builder(170, 70)
    .text("recipient", new Bounds(0, 25, 80, 35), "Alex Example\n200 Sample Street")
    .text("reference", new Bounds(100, 0, 60, 20), "Account EX-1001")
    .build();
var letter = Letter.builder().canvas(panel).body("Content follows the panel.").build();
```

`CanvasBuilder.at(id, Bounds, Block...)` accepts any supported rich blocks. The coordinate overload accepts `id, x, y, width, height, blocks`. `element(PositionedContent)` retains a preconstructed element. Styles are available on the canvas and each element. Every canvas requires unique nonblank IDs, positive finite dimensions and boxes wholly within its declared bounds. Elements paint in list order; overlap is intentional and allowed within a canvas. Element content must fit its box. Oversized canvases and painted content overflow fail rather than shrink or clip. Explicit page breaks are forbidden inside positioned elements.

```html
<canvas width-mm="170" height-mm="70">
  <box id="recipient" x-mm="0" y-mm="25" width-mm="80" height-mm="35">
    <p>Alex Example<br>200 Sample Street</p>
  </box>
  <box id="reference" x-mm="100" y-mm="0" width-mm="60" height-mm="20">
    <p><strong>Account</strong> EX-1001</p>
  </box>
</canvas>
```

The shared model is `CanvasBlock(widthMm, heightMm, elements, style)` with `PositionedContent(id, boundsMm, blocks, style)`. JSON uses `type:"canvas"`, the same dimensions, and `elements` containing `id`, `boundsMm`, `blocks` and optional `style`. The canvas origin moves with body pagination; these coordinates are not physical page-edge positions. Use fixed elements for repeating paper-relative content.

For independent flowing columns, use `ColumnsBlock(List<Column>, gapMm, style)`, where `Column(weight, blocks, style)` receives a proportional share of the width left after fixed gaps. Content in each column remains in that column; columns do not automatically rebalance newspaper-style. Native table pagination can continue long columns. Explicit page breaks within columns are unsupported.

```html
<columns gap-mm="10">
  <column weight="1"><p>Contact details</p></column>
  <column weight="2"><p>Longer explanatory content.</p></column>
</columns>
<box style="padding:4mm; border-left:2pt solid #10384A; background-color:#F3F5F7">
  <p>A highlighted flowing notice.</p>
</box>
```

A standalone `box` flows at its content height. Typed `BoxBlock(blocks, appearance, style)` uses `BoxAppearance(padding, borders, backgroundColor)` and `Borders(top, right, bottom, left)`; `Borders.uniform(border)` is convenient. Reuse `Padding` in millimeters and `Border` widths in points. HTML boxes accept ordinary text CSS plus `padding` (one/two/four lengths), uniform `border`, per-edge `border-top/right/bottom/left`, and `background-color`. Border values are `none` or width/style/color. Inline image/text layout and general browser CSS remain outside the supported vocabulary.

For paper, use `.page(PageSize.A4)` or `.page(PageSize.LETTER)`, `.landscape()`, or `.pageSizeMm(180, 240)`. Custom dimensions preserve their supplied orientation in the fluent call; a later `.orientation(Orientation.PORTRAIT/LANDSCAPE)` explicitly selects the shorter/longer horizontal edge. Typed/JSON `PageSpec` adds `dimensions: {widthMm, heightMm}` and `orientation`; its default orientation is PORTRAIT. `page.widthMm()` and `page.heightMm()` return effective physical dimensions. HTML uses paired `page-width-mm` / `page-height-mm` root attributes and optional `orientation`. Margins and region coordinates retain their existing meaning.
