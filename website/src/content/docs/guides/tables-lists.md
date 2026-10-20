---
title: "Tables and lists"
description: "Letter Generator usage: tables and lists."
---

### Fluent table

```java
import com.nsdev.lettergenerator.Table;

var prices = Table.builder("Component", "Old price", "New price")
    .columnWeights(2, 1, 1)
    .headerBackground("#10384A")
    .headerStyle(Style.builder().color("#FFFFFF").bold().build())
    .row("Energy (EUR/kWh)", "0.2008", "0.1632")
    .row("Power (EUR/day)", "0.0645", "0.0815")
    .build();

var letter = Letter.builder().body("New prices:").table(prices).build();
```

Column weights are positive proportions, not millimeters. Every physical row must cover the column grid exactly, including cells continued by row spans. At least one body row is required. The table fills its available region width and keeps native selectable text. Its optional header repeats when the table spans pages.

Cells default to 1 mm padding, white 0.25 pt borders and middle vertical alignment. Plain cell paragraph spacing defaults to zero; rich blocks can override spacing. Long tables paginate, repeating their header. Rows prefer staying together when they fit. See the rich-cell and row-policy options below.

For per-cell styling, instantiate `TableCell(runs, style, backgroundColor)` and `TableBlock(columnWeights, header, rows, style)`. An empty header list produces a headerless advanced table. The convenience `Table.builder(...)` always defines a header.

### Template table

```html
<table column-weights="2,1">
  <thead style="background-color:#10384A;color:white">
    <tr><th>Service</th><th>Status</th></tr>
  </thead>
  <tbody>
    <tr style="background-color:#eee"><td>${service}</td><td>${status}</td></tr>
  </tbody>
</table>
```

Text/background styling inherits from table to section to row to cell. One header row is supported. Place all table text inside `td` or `th`; structural text between rows/cells is an error.

### Rich cells, spans and appearance

```java
import com.nsdev.lettergenerator.model.table.Border;
import com.nsdev.lettergenerator.model.table.BorderStyle;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.Padding;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.table.VerticalAlignment;
import com.nsdev.lettergenerator.model.layout.Keep;

var table = Table.builder("Item", "Description", "Amount")
    .cellAppearance(new CellAppearance(
        new Border(0.5, BorderStyle.SOLID, "#B8C9CF"),
        Padding.uniform(2), VerticalAlignment.TOP, null))
    .headerCells(TableCell.text("Details").span(2, 1), TableCell.text("Amount"))
    .rowCells(TableCell.content(List.of(descriptionParagraph, imageBlock)).span(2, 1),
              TableCell.text("EUR 25.00"))
    .rowKeepTogether(Keep.PREFER)
    .build();
```

Use `TableCell.text(...)` for inline content or `TableCell.content(List<Block>)` for paragraphs, headings, nested lists, images and layout containers. These forms are exclusive. `content(List.of())` is an empty visual cell. `span(columns, rows)`, `withStyle(style)` and `withAppearance(appearance)` return immutable copies. Cell images use the merged width after padding and borders; oversized explicit images are rejected. Nested tables and page breaks are rejected, including inside components/groups.

Every row must cover the physical column grid exactly. A cell starts at the next unoccupied column. Continuing row spans supply their occupied columns; omit those cells from later rows. An empty row is valid when previous spans cover all columns. Column weights still describe physical columns. Spans cannot overlap or extend beyond a section; the single repeating header row cannot span into body rows. Fully covered continuation rows have no independent height and are collapsed for native FO rendering.

Appearance order is legacy defaults, table `cellAppearance`, cell `appearance`, then legacy cell `backgroundColor`. Borders are uniform on four edges and measured in points; padding is top/right/bottom/left in millimeters. Alignment is TOP, MIDDLE or BOTTOM. Null properties inherit independently; `Border.NONE` and `Padding.uniform(0)` are explicit overrides. Background null inherits; transparency overrides and per-edge border styles are not currently exposed.

```html
<table column-weights="2,1,1" row-keep="prefer"
       style="border:0.5pt solid #B8C9CF;padding:2mm;vertical-align:top">
  <thead><tr><th colspan="2">Details</th><th>Amount</th></tr></thead>
  <tbody keep-together="always">
    <tr row-keep="always"><td rowspan="2"><p>Two-row description</p></td>
      <td>First value</td><td>EUR 10.00</td></tr>
    <tr><td>Second value</td><td>EUR 15.00</td></tr>
  </tbody>
</table>
```

Table, section, row and cell appearance inherits toward cells. `padding` accepts one, two (vertical/horizontal), or four lengths; `border` accepts `none` or `width style color`, where style is solid, dashed, dotted or none. `vertical-align` accepts top/middle/bottom. `colspan` and `rowspan` are positive integers.

### Row splitting and row groups

`rowKeepTogether(Keep.AUTO)` allows native row splitting; PREFER is the default and ALWAYS strongly keeps each row. `rowPolicy(new TableRowPolicy(rowIndex, keepTogether, keepWithNext))` overrides a zero-based **body** row. `rowGroup(new TableRowGroup(startRow, rowCount, keepTogether))` requests that a contiguous set of body rows travel together. Groups must be disjoint and in range; their stronger keep takes precedence over weaker row policies. Headers use the table's default row keep and repeat after page breaks.

The HTML equivalents are table/tr `row-keep`, tr `keep-with-next`, and tbody `keep-together` attributes, each taking auto/prefer/always. Cell or rich-block keeps can additionally constrain splitting. FOP may reject an oversized row/group with a strong keep; row splitting with merged cells remains subject to native engine limits.

### Lists

```java
var letter = Letter.builder()
    .bodyHtml("<ol><li>First item</li><li><strong>Second</strong> item</li></ol>")
    .build();
```

Use `ul` for bullets or `ol` for numbering. An `li` contains inline text followed by optional nested `ul`/`ol` children. Each nested list indents relative to its parent and numbers independently from one. Put item text before child lists; arbitrary paragraph/table/image blocks within items remain unsupported. Item margins are unsupported; style the surrounding list.

Typed flat lists retain `ListBlock(items, ordered, style)`, where each item is a list of `TextRun`. Hierarchical lists use `ListBlock.nested(List<ListItem>, ordered, style)` and `new ListItem(runs, children)`. `children` is a list of child `ListBlock` values. JSON uses `contentItems` for hierarchical items and `items` for flat items; exactly one form must be present.
