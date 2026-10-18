package com.nsdev.lettergenerator.internal.xml;

/** XML 1.0 character ranges; isolated UTF-16 surrogates are excluded. */
public final class XmlCharacters {
    /** Start of the first printable XML character range, after permitted whitespace controls. */
    private static final int FIRST_PRINTABLE = 0x20;

    /** Last XML character before the UTF-16 surrogate range. */
    private static final int LAST_BEFORE_SURROGATES = 0xD7FF;

    /** First XML character after the UTF-16 surrogate range. */
    private static final int FIRST_AFTER_SURROGATES = 0xE000;

    /** Last permitted Basic Multilingual Plane code point; U+FFFE and U+FFFF are excluded. */
    private static final int LAST_BASIC_CHARACTER = 0xFFFD;

    /** First supplementary Unicode code point. */
    private static final int FIRST_SUPPLEMENTARY_CHARACTER = 0x10000;

    /** Largest Unicode code point allowed by the XML 1.0 character production. */
    private static final int LAST_UNICODE_CHARACTER = 0x10FFFF;

    /** Prevents instantiation of the Unicode character-range predicate. */
    private XmlCharacters() {}

    /**
     * Tests one Unicode code point against the XML 1.0 character repertoire.
     *
     * @param codePoint code point to check; isolated UTF-16 surrogate values are rejected
     * @return true for XML-safe tabs, line breaks and printable Unicode ranges
     */
    public static boolean isValid(int codePoint) {
        return codePoint == '\t'
                || codePoint == '\n'
                || codePoint == '\r'
                || (codePoint >= FIRST_PRINTABLE && codePoint <= LAST_BEFORE_SURROGATES)
                || (codePoint >= FIRST_AFTER_SURROGATES && codePoint <= LAST_BASIC_CHARACTER)
                || (codePoint >= FIRST_SUPPLEMENTARY_CHARACTER
                        && codePoint <= LAST_UNICODE_CHARACTER);
    }
}
