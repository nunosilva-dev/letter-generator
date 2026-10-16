package com.nsdev.lettergenerator.internal.font;

import java.util.Set;

/** Built-in family identifiers shared by validation and both rendering engines. */
public final class StandardFontFamilies {
    public static final String HELVETICA = "Helvetica";
    public static final String TIMES = "Times";
    public static final String COURIER = "Courier";
    private static final Set<String> FAMILIES = Set.of(HELVETICA, TIMES, COURIER);

    private StandardFontFamilies() {}

    public static boolean contains(String family) {
        return family != null && FAMILIES.contains(family);
    }
}
