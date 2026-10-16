package com.nsdev.lettergenerator;

import com.nsdev.lettergenerator.builder.TableBuilder;

/** Entry point for simple tables with text headers and rows. */
public final class Table {
    private Table() {}

    public static TableBuilder builder(String... headers) {
        return new TableBuilder(headers);
    }
}
