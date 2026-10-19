package com.nsdev.lettergenerator.internal.template;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns typed template data and resolves map-only scoped paths. */
public final class TemplateData {
    /** Prevents construction. */
    private TemplateData() {}

    /**
     * Snapshots nested maps/lists and finite supported scalar values.
     *
     * @param values non-null input map
     * @return immutable owned map
     */
    public static Map<String, Object> snapshot(Map<String, ?> values) {
        if (values == null) {
            throw new IllegalArgumentException("Template values must not be null");
        }
        return map(values, "values", new IdentityHashMap<>());
    }

    /**
     * Copies a map with nonblank string keys and detects object cycles.
     *
     * @param source map
     * @param path diagnostic path
     * @param active containers on the current recursion path
     * @return immutable map
     */
    private static Map<String, Object> map(
            Map<?, ?> source, String path, IdentityHashMap<Object, Boolean> active) {
        enter(source, path, active);
        var copy = new LinkedHashMap<String, Object>();
        source.forEach(
                (key, value) -> {
                    if (!(key instanceof String name) || name.isBlank()) {
                        throw new IllegalArgumentException(
                                "Nonblank string key required at " + path);
                    }
                    copy.put(name, owned(value, path + "." + name, active));
                });
        active.remove(source);
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Copies one supported scalar/container without invoking bean methods.
     *
     * @param value input value
     * @param path input path
     * @param active active containers
     * @return immutable value
     */
    private static Object owned(
            Object value, String path, IdentityHashMap<Object, Boolean> active) {
        if (value instanceof Map<?, ?> mapping) {
            return map(mapping, path, active);
        }
        if (value instanceof List<?> list) {
            enter(value, path, active);
            var copy = new ArrayList<Object>();
            for (int index = 0; index < list.size(); index++) {
                copy.add(owned(list.get(index), path + "[" + index + "]", active));
            }
            active.remove(value);
            return List.copyOf(copy);
        }
        if (value instanceof CharSequence sequence) {
            return sequence.toString();
        }
        if (value instanceof Character
                || value instanceof Boolean
                || value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof BigInteger
                || value instanceof BigDecimal
                || value instanceof Float number && Float.isFinite(number)
                || value instanceof Double number && Double.isFinite(number)
                || value instanceof LocalDate
                || value instanceof LocalTime
                || value instanceof LocalDateTime
                || value instanceof OffsetDateTime
                || value instanceof OffsetTime
                || value instanceof ZonedDateTime
                || value instanceof Instant) {
            return value;
        }
        throw new IllegalArgumentException("Unsupported or null template value at " + path);
    }

    /**
     * Detects cycles while allowing repeated independent references.
     *
     * @param value container
     * @param path input path
     * @param active active identity set
     */
    private static void enter(Object value, String path, IdentityHashMap<Object, Boolean> active) {
        if (active.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Cyclic template data at " + path);
        }
    }

    /**
     * Resolves exact flat keys before nested map traversal; no reflection is used.
     *
     * @param values scope map
     * @param key exact key or dotted map path
     * @return value, or null for a missing path
     */
    public static Object find(Map<String, ?> values, String key) {
        Object current = values;
        String remaining = key;
        while (current instanceof Map<?, ?> mapping) {
            if (mapping.containsKey(remaining)) {
                return mapping.get(remaining);
            }
            int separator = remaining.indexOf('.');
            if (separator < 0 || !mapping.containsKey(remaining.substring(0, separator))) {
                return null;
            }
            current = mapping.get(remaining.substring(0, separator));
            remaining = remaining.substring(separator + 1);
        }
        return null;
    }
}
