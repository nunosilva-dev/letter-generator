---
title: "Typed data, conditions and repeated rows"
description: "Letter Generator usage: typed data, conditions and repeated rows."
---

`.values(Map<String, ?>)` owns nested maps and lists immediately. Accepted leaf values are strings/character sequences, characters, booleans, byte/short/int/long values, `BigInteger`, `BigDecimal`, finite floats/doubles, and immutable `LocalDate`, `LocalTime`, `LocalDateTime`, `OffsetDateTime`, `OffsetTime`, `ZonedDateTime` or `Instant` values. Character sequences become strings. Null values, blank/non-string map keys, object cycles, arbitrary objects, bean reflection, arrays and unsupported number implementations fail with the input data path.

`${customer.name}` resolves a nested map value. At each map level, an exact literal dotted key wins over splitting that key into nested lookups: `"customer.name"` can be supplied directly. `${key}` remains a literal text substitution; use `param` for formatting:

```html
<letter language-tag="en-GB" currency="EUR">
  <main>
    <p>Hello ${customer.name}. Your appointment is <param key="appointment" format="date">.</p>
    <p>Balance: <param key="balance" format="currency">.</p>
    <p>Exact decimal format: <param key="balance" format="number" pattern="#,##0.00">.</p>
    <if flag="overdue">
      <p>Please contact us about this balance.</p>
      <else><p>Thank you for keeping your account up to date.</p></else>
    </if>
    <table column-weights="3,1">
      <thead><tr><th>Service</th><th>Amount</th></tr></thead>
      <tbody><tr data-each="items" data-as="item">
        <td>${item.description}</td><td><param key="item.amount" format="currency"></td>
      </tr></tbody>
    </table>
    <p data-if="showContact">Contact ${contact} for help.</p>
  </main>
</letter>
```

```java
var template = Letter.template(source).values(Map.of(
    "customer", Map.of("name", "Alex Example"),
    "appointment", LocalDate.of(2026, 10, 19),
    "balance", new BigDecimal("29.75"),
    "overdue", false,
    "items", List.of(
        Map.of("description", "Home service", "amount", new BigDecimal("24.50")),
        Map.of("description", "Additional support", "amount", new BigDecimal("5.25"))),
    "showContact", false));
var letter = template.build();
```

`param` is an **empty HTML void element**: use `<param ...>` or `<param .../>`, without `</param>`. Its required `key` selects a value; optional `format`, `pattern` and `currency` select the finite formatting rules above. Put styling on an enclosing `span` or `strong`, because the parameter itself becomes one literal text node.

| Directive | Exact behavior |
| --- | --- |
| `<if flag="name">...<else>...</else></if>` | Requires a named boolean; one optional direct `else`, last in the wrapper |
| `data-if="name"` | Removes that element when its named boolean is false |
| `data-each="path" data-as="item"` | Repeats trusted block/row markup once for each list item, in order |
| `${item.field}` | Resolves the current repeat alias; outer aliases remain available in nested repeats |

Conditions accept actual booleans, without expressions, negation or string truthiness. Use `data-if` on rows/cells inside tables; `if` wrappers are supported outside tables. Repetition supports rows and ordinary blocks such as paragraphs, `div`, lists, tables, images, canvases and boxes; it does not repeat letter regions or table structural containers. `data-as` requires a simple alias matching `[A-Za-z_][A-Za-z0-9_]*` and is valid only with `data-each`. On a node with both repeat and condition directives, the list is expanded first and the condition resolves inside each local scope.

A nested alias shadows the same outer/root name only for its cloned subtree. After that subtree, the outer binding is retained. Bound strings never become tags, attributes, directives or recursive placeholders. CSS property names stay literal; substituted CSS values cannot introduce another declaration.

Missing selected values fail with a template selector path. Missing conditions/collections also fail; an empty list emits no clones, a false condition emits no selected content, and unused branches do not require their bindings or assets. A blank string stays blank; it is not a missing key. Maps/lists cannot be printed as scalar text. The final document must still contain valid body content, and a repeated table must still satisfy ordinary table validation.

Directives run only through `Letter.template(...)`. `Letter.fromHtml(...)` expects resolved supported markup and leaves `${literal}` text intact; it does not evaluate directives. The compiled output is the same ordinary immutable block model used by typed Java, fluent Java and JSON.
