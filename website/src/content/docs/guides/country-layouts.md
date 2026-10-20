---
title: "Country and stationery variations"
description: "Letter Generator usage: country and stationery variations."
---

Set paper size, margins, header/address bounds, typography and assets for each letter through builder methods or template attributes.

Configure a layout before rendering the body:

```java
var letter = Letter.builder()
    .page(PageSize.LETTER)
    .margins(new Margins(15, 18, 17, 22))
    .header(recipientAddress)
    .headerBoundsMm(88, 23, 78, 28)
    .body(bodyText)
    .build();
```

Those measurements are illustrative, not a claim of conformance to a postal or national letter standard. Measure the actual envelope/window/stationery, convert paper-edge coordinates using [header placement](/guides/headers/), and inspect a printed sample at 100% scale. Printer scaling and physical tolerances are outside PDF generation.

With HTML templates, use scalar geometry placeholders:

```html
<letter page="${paper}" margins-mm="${margin}">
  <header x-mm="${addressX}" y-mm="${addressY}"
          width-mm="${addressWidth}" height-mm="${addressHeight}">
    <p>${recipientName}<br>${street}<br>${postalAddress}</p>
  </header>
  <main><p>${message}</p></main>
</letter>
```

Supply those keys along with recipient data in `.values(...)`. Use separate placeholders for address lines because template whitespace follows HTML rules. Use explicit `LocaleSettings` with `param` for currency, dates and decimal separators; application text such as salutations remains caller-supplied. Choose/register fonts that cover the actual language.
