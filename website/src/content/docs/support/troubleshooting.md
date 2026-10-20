---
title: "Errors and troubleshooting"
description: "Letter Generator usage: errors and troubleshooting."
---

```java
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.exception.PdfGenerationException;

try {
    byte[] pdf = generator.generate(letter);
    Files.write(Path.of("letter.pdf"), pdf);
} catch (InvalidLetterException error) {
    System.err.println("Invalid letter at " + error.fieldPath() + ": " + error.getMessage());
} catch (PdfGenerationException error) {
    System.err.println("Letter could not be laid out: " + error.getMessage());
}
```

Catch errors around `build()` too when compiling templates; template validation occurs there. Fluent argument checks can throw `IllegalArgumentException`, missing required content can throw `IllegalStateException`, and path-taking image/asset methods can throw `IOException`. Model validation occurs at `generate()`; overflow/glyph failures can only be known during rendering.

| Symptom | Cause and action |
| --- | --- |
| No accessible font-registry constructor | Use public `new LetterPdfGenerator()`; internal registry configuration is private |
| `${name}` is printed literally | You used plain/fragment content or `fromHtml`; use `Letter.template(...).values(...)` for substitution |
| Missing template variable | Add the key to the map or publish a template without that placeholder |
| A replacement's `<strong>` prints literally | Expected for map data; put formatting in the template, not replacement values |
| A replacement newline does not change lines | HTML whitespace collapses; use separate placeholders separated by `<br>`, or plain Java multiline text |
| Unknown family / missing variant | Register that exact family and every requested regular/bold/italic/bold-italic face |
| Missing glyph / replacement character rejected | Choose a font covering the actual character and verify the target-language output |
| Header is full width | Add `headerWidthMm` if you want a narrow positioned block; alignment alone aligns its content |
| Right-positioned box still has left-aligned paragraph | An explicit paragraph/region text style overrides default alignment |
| Address location is off by the page margin | Convert paper-edge coordinates to inner-page coordinates; inspect the box versus glyph baseline |
| Body starts too low | Header y+height and logo height reserve space; reduce the box or use a different layout configuration |
| Header/footer overflow | Increase content height/width, reduce image/text size or content; nothing is silently clipped |
| Header collides with logo | Use automatic placement with a suitable width, or move the exact box away from the separate logo |
| Unexpected extra page | Inspect explicit breaks, long unbreakable text, keep-together signature groups and bottom-last signature reservation |
| Image does not fill both dimensions | Aspect ratio is preserved within the supplied box |
| Missing `asset:name` | Supply it through `.asset(name, bytesOrPath)`; the library never fetches image references |
| Unsupported CSS/tag/HTML source error | Use the documented subset; remove browser wrappers/layout styles or fix malformed markup |
| `NoClassDefFoundError` when using a copied JAR | Include transitive runtime dependencies |

Avoid swallowing rendering errors and returning empty/partial PDFs. Log the field path and underlying exception in the application's normal error handling, with its own handling of recipient-sensitive data.
