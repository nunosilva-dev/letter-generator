---
title: Binding and directive reference
description: Literal placeholders, formatted parameters, boolean branches, repeated nodes and prepared-template behavior.
sidebar:
  order: 4
---

Template directives apply to `Letter.template(source)`, including prepared templates. They are disabled for `Letter.fromHtml(resolvedSource)`. Binding data is a map, not an expression context.

| Syntax | Input | Behavior |
| --- | --- | --- |
| `${key}` | Supported scalar | Literal substitution into parsed text/attributes; no markup insertion or recursive expansion |
| `${account.number}` | Exact flat key or nested map path | Exact flat dotted key takes precedence over nested-map lookup |
| `<param key="total" format="currency" currency="EUR">` | Compatible typed scalar | Locale-aware literal formatting; optional `pattern` and `currency` |
| `<if flag="show">…<else>…</else></if>` | Actual Boolean | Select one branch outside tables; at most one direct final else |
| `data-if="show"` | Actual Boolean | Keep/remove the actual parsed element; useful on real tr/td/th nodes |
| `data-each="items" data-as="item"` | List | Clone a trusted node per value and create a local alias |
| `${item.label}` | Current alias value/map | Resolve in the repeat's lexical scope; nested repeats can use their own aliases |

## Data values

Supported values include strings/characters, booleans, finite numeric values, supported immutable Java time values, nested string-keyed maps and Lists. Bindings are snapshotted. Nulls, object cycles and arbitrary application objects are rejected. There is no bean/property reflection, environment/system/URL lookup, executable code, expression language or placeholder-default syntax.

Missing values fail with source paths; an empty string is a present value. Inactive branches do not require their scalar values/assets. An empty repeated list removes its node; the resulting document/table must still meet ordinary content rules.

## Formatted parameters

`format` is `text`, `date`, `number` or `currency`; absent format selects text. Date patterns use Java date formatting; number/currency patterns use the finite formatting helper's numeric pattern contract. The document locale, selected currency and zone provide defaults. Supply actual numeric/temporal values for those formats rather than assuming arbitrary strings are parsed.

`param` is a void HTML element: **no `</param>`, child content, `style` or `style-name`**. Style its enclosing `span`/`strong`. See [localized formatting](/guides/localization/).

## Repeating nodes

Allowed repeated nodes are `tr`, `p`, `div`, `h1`–`h6`, `ul`, `ol`, `li`, `table`, `img`, `box`, `columns`, `canvas` and `component`. Aliases match `[A-Za-z_][A-Za-z0-9_]*`. `data-as` without `data-each` is rejected. Root regions and table structural wrappers are not repeat containers. Inside tables use real row/cell nodes and `data-if`, since HTML table parsing does not preserve arbitrary branch wrappers.

## Prepared templates and validation

`compile()` captures immutable authoring configuration and a parsed prototype. `bind(map)` replaces the captured bindings with the supplied map; it does not merge them. Each bind clones the prototype; binding/model validation/layout still run per document. A prepared template can be reused concurrently.

`requirements()` inventories occurrences, including inactive branches and scoped aliases. It is not a full application data schema. Template validation aggregates missing scalar occurrences and independent asset failures; directive/format and later semantic failures retain first-error boundaries. Successful preflight does not establish page fit.

See [typed data examples](/guides/template-data/), [prepared templates](/guides/prepared-templates/) and [diagnostics](/guides/diagnostics/).
