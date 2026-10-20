---
title: "Company stationery and table appearance"
description: "Letter Generator usage: company stationery and table appearance."
---

A theme combines typography with reusable stationery and table appearance. All four input APIs use the same preparation step. Explicit local values override shared defaults; `false`, zero padding, `Border.NONE` and `Keep.AUTO` are deliberate values.

```java
import com.nsdev.lettergenerator.theme.Stationery;
import com.nsdev.lettergenerator.theme.TableAppearance;
import com.nsdev.lettergenerator.model.table.CellAppearance;
import com.nsdev.lettergenerator.model.table.Padding;
import com.nsdev.lettergenerator.model.layout.Keep;

var stationery = Stationery.builder()
    .header("Example Company")
    .headerAlignment(TextAlign.RIGHT)
    .footerHtml("<p><strong>Example Company</strong> — Customer services</p>")
    .build();
var company = LetterTheme.builder()
    .body(Style.builder().fontSizePt(11).color("#243746").build())
    .role(StyleRole.SUBJECT, Style.builder().fontSizePt(16).bold().build())
    .tableAppearance(new TableAppearance(
        new CellAppearance(null, Padding.uniform(2), null, "#F3F5F7"),
        new CellAppearance(null, null, null, "#10384A"),
        Keep.PREFER))
    .stationery(stationery)
    .build();

var letter = Letter.builder().theme(company).subject("Account update").body("Your update...").build();
var fromHtml = Letter.fromHtml("<letter><main>Your update...</main></letter>").theme(company).build();
```

`StationeryBuilder` uses ordinary letter region fitting and positioning. Configure `page(...)` and `margins(...)` for the intended sheet. `headerBounds(Bounds)` is relative to the inner page; `headerHeightMm`, `footerHeightMm`, `headerStyle`, `footerStyle`, `asset`, `logo` and rich `header(PageSelector, Block...)` / `footer(PageSelector, Block...)` support styled content and image blocks. An image footer can also use `asset("footer", bytes).footerHtml("<img src='asset:footer'>")`. Build separate stationery values when paper/margins or country layouts differ. Explicit geometry is checked against the final letter; inherited stationery does not silently rescale.

`new Stationery(elements, topZoneMm, bottomZoneMm)` is the typed/JSON form. Letter-local fixed elements replace inherited elements with the same stable ID. Generated IDs include `letter.header`, `letter.footer` and `letter.logo`; page variants add suffixes such as `.first` and `.following`. Other inherited regions remain. Top/bottom reservations use the larger configured height, so changing a local header cannot accidentally reclaim inherited footer/body space.

`TableAppearance(cells, header, rowKeepTogether)` sits below explicit table options and cell appearance. Header defaults additionally overlay common cell defaults; the fluent table's builtin header fill is a fallback rather than an explicit local override. `headerBackground(...)` is explicit and wins. Theme row defaults do not replace row-specific policies or groups. Typography still uses `TABLE`, `TABLE_HEADER` and `TABLE_CELL` roles.

JSON places these values under `theme.tableAppearance` and `theme.stationery`. Semantic roles additionally include `SENDER`, `RECIPIENT`, `DATE`, `SUBJECT`, `REFERENCE`, `SALUTATION` and `CLOSING`. `CompleteThemeFeaturesTest` verifies table appearance and stationery through typed Java, fluent Java, JSON and HTML.
