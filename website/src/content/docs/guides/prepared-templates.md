---
title: "Prepare templates once and inspect their contracts"
description: "Letter Generator usage: prepare templates once and inspect their contracts."
---

Call `.compile()` after configuring a template's assets, theme, components, locale and optional default values. It parses and captures an immutable source prototype. Each subsequent build/bind clones that prototype and owns a fresh traversal and model; reuse one prepared template concurrently.

```java
import com.nsdev.lettergenerator.template.PreparedLetterTemplate;

PreparedLetterTemplate prepared = Letter.template(
    "<letter><main><p>Hello ${name}.</p></main></letter>")
    .values(Map.of("name", "Alex Example"))
    .compile();

var first = prepared.build(); // Uses captured default bindings.
var second = prepared.bind(Map.of("name", "Sam Example")); // Replaces the whole binding map for this call.
byte[] firstPdf = generator.generate(first);
byte[] secondPdf = generator.generate(second);
```

Later changes to the original builder, data maps/lists or asset byte arrays do not change a prepared template. Theme and component configuration follow the same shared preparation rules as ordinary `.build()`. Parsing is reused; binding, model validation and PDF layout still happen for each document. Builders remain mutable and should not be shared concurrently. A prepared `Letter.fromHtml(...).compile()` supports `.build()`, while `.bind(...)` rejects values because substitution is disabled.

`prepared.requirements()` returns immutable occurrence lists: `variables()`, `flags()`, `collections()`, `assets()`, `styles()` and `components()`. Every `TemplateRequirement` contains `name()` and a source selector `path()`, including an attribute suffix where relevant. Repeated occurrences are retained. `variables` includes `${...}` placeholders and `param` keys; `assets` includes literal `asset:name` image references. Dynamic references retain the original placeholder text. The manifest includes conditional branches and scoped alias paths, so it is a syntactic inventory rather than a statement that every name must be supplied at the root for every binding.

`prepared.validate([generator])` inspects the captured default binding/configuration without PDF layout. For another binding, use `generator.validate(prepared.bind(values))`. Inspection aggregates missing scalar placeholder occurrences in active branches/repeat clones and independent asset errors. Missing directive paths, parameter formatting failures and the first remaining semantic model error stop that phase with a located diagnostic. Success does not establish final content fit.

Run `V1LayoutExampleTest` for `target/examples/v1-layout-pack.pdf` and its PNG preview: a two-page letter with SVG stationery, positioned addresses, columns, nested lists, a decorated notice, a bottom-last signature and an appended PDF. It also renders the model through JSON and checks identical extracted content.

Run `V1AuthoringExampleTest` to produce `target/examples/v1-authoring.pdf` and `v1-authoring.png`. The example combines semantic regions, deferred/localized dates, formatted amounts, a repeated table, boolean branches and repeated binding from one prepared template. `PreparedTemplateTest` also exercises concurrent independent bindings.
