---
title: "Semantic letter parts and address layouts"
description: "Letter Generator usage: semantic letter parts and address layouts."
---

Use sender, date, recipient, reference, subject, salutation and closing methods for an ordinary letter. Calls replace the previous value for that role; their call order does not change the conventional display order. Body content still appends in call order.

```java
import com.nsdev.lettergenerator.letter.Address;
import com.nsdev.lettergenerator.letter.Party;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import java.time.LocalDate;

var letter = Letter.builder()
    .sender(Party.company("Example Company", Address.of("18 Harbour Road", "Example City EX1 2AB")))
    .recipient(Party.of("Alex Example", Address.of("24 Sample Lane", "Example Town EX2 3CD")))
    .date(LocalDate.of(2026, 10, 19))
    .reference("Account", "EX-1001")
    .subject("Service renewal confirmation")
    .salutation("Dear Alex,")
    .body("Your service has been renewed.")
    .closing("Kind regards,\nCustomer Services")
    .locale(new LocaleSettings("en-GB"))
    .build();
```

The order is sender, date, recipient, reference, subject, salutation, body, then closing. The subject defaults to bold 14 pt with a keep to the following content. Semantic theme roles are `SENDER`, `DATE`, `RECIPIENT`, `REFERENCE`, `SUBJECT`, `SALUTATION` and `CLOSING`; explicit local styles override those roles. Date formatting uses the **final** builder locale, so setting `.locale(...)` after `.date(...)` is supported. Postal lines retain their supplied order and line breaks; the library does not infer national address rules.

`Party(name, organization, address)` accepts an individual, an organization, or both. `Party.of(name, address)` and `Party.company(organization, address)` are conveniences. A party needs a nonblank name or organization; supplied identity fields and address lines must be nonblank. `Address.of(String...)` snapshots ordered lines. The fluent semantic methods check these inputs during authoring.

For rich content use `.part(LetterElement.SUBJECT, blocks)` or `.part(element, blocks, localStyle)`. The shared typed equivalent is `new LetterPartBlock(element, blocks, style)`. Typed and JSON block lists keep the order the caller supplies; fluent/HTML semantic authoring supplies the conventional ordering. JSON uses the `letterPart` block discriminator:

```json
{
  "schemaVersion": 1,
  "sections": [{"name": "Body", "blocks": [
    {"type": "letterPart", "element": "SUBJECT", "blocks": [
      {"type": "paragraph", "runs": [{"text": "Service renewal confirmation"}]}]},
    {"type": "paragraph", "runs": [{"text": "Your service has been renewed."}]}
  ]}]
}
```

HTML provides optional top-level `<sender>`, `<date>`, `<recipient>`, `<reference>`, `<subject>`, `<salutation>` and `<closing>` regions. Each occurs at most once and accepts supported rich blocks and inline styles. A `main` region remains required. A reference requires a nonblank literal `label`; the compiler inserts a bold label and colon before its content. A date can use an ISO local-date `value` or authored rich content:

```html
<letter language-tag="en-GB">
  <sender><p><strong>Example Company</strong><br>18 Harbour Road</p></sender>
  <date value="2026-10-19"></date>
  <recipient><p>Alex Example<br>24 Sample Lane</p></recipient>
  <reference label="Account"><p><em>EX-1001</em></p></reference>
  <subject>Service renewal confirmation</subject>
  <salutation>Dear Alex,</salutation>
  <main><p>Your service has been renewed.</p></main>
  <closing><p>Kind regards,<br>Customer Services</p></closing>
</letter>
```

For an address window, `.addressLayout(AddressLayout.windowEnvelope())` moves configured sender, recipient and date parts into a body canvas. The editable `AddressLayout(senderBounds, recipientBounds, dateBounds, heightMm)` uses millimeters relative to that canvas; a null bound leaves that part in ordinary flow. Positioned IDs are `letter.sender`, `letter.recipient` and `letter.date`. Bounds must fit the final available width and the declared panel height; measure the actual envelope and inspect a printed sample. The preset is an example arrangement, without a postal compliance guarantee.
