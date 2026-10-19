package com.nsdev.lettergenerator.theme;

/** Semantic locations whose theme defaults precede explicit document and content styles. */
public enum StyleRole {
    /** Shared fallback typography for ordinary content. */
    BODY,
    /** Heading typography, before explicit heading overrides. */
    HEADING,
    /** Table-wide typography and spacing. */
    TABLE,
    /** Repeating table-header cell typography. */
    TABLE_HEADER,
    /** Table-body cell typography. */
    TABLE_CELL,
    /** Content in the reserved top page region. */
    HEADER,
    /** Content in the reserved bottom page region. */
    FOOTER,
    /** Flowing or final-page visual signature content. */
    SIGNATURE,
    /** Sender identity and postal address. */
    SENDER,
    /** Recipient identity and postal address. */
    RECIPIENT,
    /** Locale-formatted letter date. */
    DATE,
    /** Subject line. */
    SUBJECT,
    /** Labeled reference details. */
    REFERENCE,
    /** Opening greeting. */
    SALUTATION,
    /** Closing greeting before the signature. */
    CLOSING
}
