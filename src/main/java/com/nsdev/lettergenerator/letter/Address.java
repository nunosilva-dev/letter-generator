package com.nsdev.lettergenerator.letter;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import java.util.Arrays;
import java.util.List;

/**
 * Application-supplied postal address lines in their intended display order.
 *
 * @param lines address lines, defensively snapshotted and validated during generation
 */
public record Address(List<String> lines) {
    /**
     * Snapshots address lines without imposing country-specific postal rules.
     *
     * @param lines ordered lines
     */
    public Address {
        lines = ImmutableLists.copyNullable(lines);
    }

    /**
     * Creates an address from ordered literal lines.
     *
     * @param lines address lines
     * @return immutable address
     */
    public static Address of(String... lines) {
        return new Address(lines == null ? null : Arrays.asList(lines));
    }
}
