package com.nsdev.lettergenerator.builder;

import com.nsdev.lettergenerator.internal.builder.BuilderChecks;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Mutable convenience builder; each build returns an immutable snapshot. */
public final class TableBuilder {
    private static final String DEFAULT_HEADER_BACKGROUND = "#10384A";
    private static final Style DEFAULT_HEADER_STYLE =
            Style.builder().bold().color("#FFFFFF").build();
    private final List<String> headers;
    private final List<List<TableCell>> rows = new ArrayList<>();
    private List<Double> weights;
    private Style style;
    private Style headerStyle = DEFAULT_HEADER_STYLE;
    private String headerBackground = DEFAULT_HEADER_BACKGROUND;

    public TableBuilder(String... headers) {
        BuilderChecks.required(headers, "table headers");
        BuilderChecks.require(headers.length > 0, "At least one table column is required");
        this.headers =
                Arrays.stream(headers)
                        .map(value -> BuilderChecks.required(value, "column header"))
                        .toList();
        weights = Collections.nCopies(headers.length, 1d);
    }

    public TableBuilder row(String... values) {
        BuilderChecks.required(values, "row");
        BuilderChecks.require(
                values.length == headers.size(), "Row must contain " + headers.size() + " cells");
        rows.add(
                Arrays.stream(values)
                        .map(value -> cell(BuilderChecks.required(value, "cell text"), null, null))
                        .toList());
        return this;
    }

    public TableBuilder columnWeights(double... values) {
        BuilderChecks.required(values, "column weights");
        BuilderChecks.require(
                values.length == headers.size(), "Weights must match the column count");
        var copy = new ArrayList<Double>();
        double total = 0;
        for (double value : values) {
            copy.add(BuilderChecks.positive(value, "column weight"));
            total += value;
        }
        BuilderChecks.require(Double.isFinite(total), "Column weights must have a finite sum");
        weights = List.copyOf(copy);
        return this;
    }

    public TableBuilder style(Style value) {
        style = BuilderChecks.required(value, "table style");
        return this;
    }

    public TableBuilder headerStyle(Style value) {
        headerStyle = DEFAULT_HEADER_STYLE.overlay(BuilderChecks.required(value, "header style"));
        return this;
    }

    public TableBuilder headerBackground(String color) {
        headerBackground = BuilderChecks.required(color, "header background");
        return this;
    }

    public TableBlock build() {
        if (rows.isEmpty()) {
            throw new IllegalStateException("At least one table row is required");
        }
        return new TableBlock(
                weights,
                headers.stream().map(value -> cell(value, headerStyle, headerBackground)).toList(),
                rows,
                style);
    }

    private static TableCell cell(String text, Style style, String background) {
        return new TableCell(List.of(new TextRun(text, null, null)), style, background);
    }
}
