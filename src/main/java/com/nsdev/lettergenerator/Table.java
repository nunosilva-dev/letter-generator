package com.nsdev.lettergenerator;

import com.nsdev.lettergenerator.builder.TableBuilder;

/**
 * Convenience entry point for text tables embedded with {@link
 * com.nsdev.lettergenerator.builder.LetterBuilder#table(com.nsdev.lettergenerator.model.block.TableBlock)}.
 * The builder fixes the column count from the headers and appends rows before producing an
 * immutable table block.
 */
public final class Table {
    /** Prevents construction of this static entry-point utility. */
    private Table() {}

    /**
     * Starts a table with equally weighted columns and a bold white-on-dark header row. At least
     * one data row must be added before building.
     *
     * @param headers non-null header array containing at least one non-null string
     * @return a new mutable table builder
     * @throws IllegalArgumentException if the array is null or empty, or a header is null
     */
    public static TableBuilder builder(String... headers) {
        return new TableBuilder(headers);
    }
}
