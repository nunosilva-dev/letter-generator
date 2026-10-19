package com.nsdev.lettergenerator.letter;

/** Semantic content positions used by letter authoring and theme defaults. */
public enum LetterElement {
    /** Sender identity and address. */
    SENDER,
    /** Letter date. */
    DATE,
    /** Recipient identity and address. */
    RECIPIENT,
    /** Labeled reference information. */
    REFERENCE,
    /** Letter subject. */
    SUBJECT,
    /** Opening greeting. */
    SALUTATION,
    /** Closing phrase after body content. */
    CLOSING
}
