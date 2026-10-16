package com.nsdev.lettergenerator.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Preserves nulls for field-path validation while preventing caller mutation. */
public final class ImmutableLists {
    private ImmutableLists() {}

    public static <T> List<T> copyNullable(List<T> source) {
        return source == null ? null : Collections.unmodifiableList(new ArrayList<>(source));
    }
}
