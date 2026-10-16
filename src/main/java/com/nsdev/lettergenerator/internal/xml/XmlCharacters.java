package com.nsdev.lettergenerator.internal.xml;

/** XML 1.0 character ranges; isolated UTF-16 surrogates are excluded. */
public final class XmlCharacters {
    private static final int FIRST_PRINTABLE = 0x20;
    private static final int LAST_BEFORE_SURROGATES = 0xD7FF;
    private static final int FIRST_AFTER_SURROGATES = 0xE000;
    private static final int LAST_BASIC_CHARACTER = 0xFFFD;
    private static final int FIRST_SUPPLEMENTARY_CHARACTER = 0x10000;
    private static final int LAST_UNICODE_CHARACTER = 0x10FFFF;

    private XmlCharacters() {}

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
