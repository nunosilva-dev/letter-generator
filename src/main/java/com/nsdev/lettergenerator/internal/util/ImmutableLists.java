package com.nsdev.lettergenerator.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Preserves nulls for field-path validation while preventing caller mutation. */
public final class ImmutableLists {
    /** Prevents instantiation of the model list-snapshot helper. */
    private ImmutableLists() {}

    /**
     * Snapshots a list without rejecting null elements before field-path validation can see them.
     *
     * <p>This is a shallow copy: element objects retain their own ownership rules. A null source
     * remains null; unlike {@link java.util.List#copyOf}, null members are preserved in the
     * unmodifiable result.
     *
     * @param <T> list element type
     * @param source caller-owned list, or null
     * @return independent unmodifiable list preserving element order and nulls, or null
     */
    public static <T> List<T> copyNullable(List<T> source) {
        return source == null ? null : Collections.unmodifiableList(new ArrayList<>(source));
    }
}
