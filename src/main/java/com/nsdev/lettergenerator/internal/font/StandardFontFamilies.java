package com.nsdev.lettergenerator.internal.font;

import java.util.Set;

/** Built-in family identifiers shared by validation and both rendering engines. */
public final class StandardFontFamilies {
    /** Built-in proportional sans-serif family. */
    public static final String HELVETICA = "Helvetica";

    /** Built-in proportional serif family. */
    public static final String TIMES = "Times";

    /** Built-in monospaced family. */
    public static final String COURIER = "Courier";

    /** Exact built-in family identifiers accepted by validators and renderers. */
    private static final Set<String> FAMILIES = Set.of(HELVETICA, TIMES, COURIER);

    /** Prevents construction of this static helper. */
    private StandardFontFamilies() {}

    /**
     * Tests the exact built-in identifiers; aliases and case folding are not applied.
     *
     * @param family candidate identifier, possibly null
     * @return true only for Helvetica, Times or Courier
     */
    public static boolean contains(String family) {
        return family != null && FAMILIES.contains(family);
    }
}
