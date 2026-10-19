package com.nsdev.lettergenerator.model.layout;

/** Document reading direction; AUTO derives the base direction from the selected language. */
public enum TextDirection {
    /** Select the base direction from the document language. */
    AUTO,
    /** Left-to-right writing. */
    LTR,
    /** Right-to-left writing. */
    RTL
}
