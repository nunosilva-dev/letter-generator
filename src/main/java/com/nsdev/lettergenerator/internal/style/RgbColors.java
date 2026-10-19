package com.nsdev.lettergenerator.internal.style;

import java.util.regex.Pattern;

/** Shared six-digit RGB syntax for normalized model values and HTML color conversion. */
public final class RgbColors {
    /** Accepted six-digit hexadecimal form, with either letter case. */
    private static final Pattern HEX_RGB = Pattern.compile("#[0-9a-fA-F]{6}");

    /** Prevents instances of this color-syntax utility. */
    private RgbColors() {}

    /**
     * Checks the complete #RRGGBB form without normalizing case or expanding CSS shorthand.
     *
     * @param value candidate color, or null
     * @return true only for a hash followed by exactly six hexadecimal digits
     */
    public static boolean isHexRgb(String value) {
        return value != null && HEX_RGB.matcher(value).matches();
    }
}
