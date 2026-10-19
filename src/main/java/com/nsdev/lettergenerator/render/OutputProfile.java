package com.nsdev.lettergenerator.render;

/** Native output modes; conformance modes reject incompatible later processing. */
public enum OutputProfile {
    /** Ordinary PDF with optional metadata and optional appended PDFs. */
    STANDARD,
    /** Native structure tags; this mode does not guarantee PDF/UA conformance or reading order. */
    TAGGED,
    /** Native PDF/A-2b mode requiring embedded fonts and no later PDF modification. */
    PDF_A_2B
}
