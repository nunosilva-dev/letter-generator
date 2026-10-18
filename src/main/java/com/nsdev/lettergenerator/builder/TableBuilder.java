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

/**
 * Mutable text-table builder with a fixed column count and ordered, appended rows. Obtain one from
 * {@link com.nsdev.lettergenerator.Table#builder(String...)} or its constructor. Columns start with
 * equal relative weights; header cells default to bold white text on {@code #10384A}. Configuration
 * calls replace their option, and {@link #build()} takes an independent immutable snapshot for a
 * letter's body.
 */
public final class TableBuilder {
    /** Six-digit RGB fill applied to each header cell unless replaced. */
    private static final String DEFAULT_HEADER_BACKGROUND = "#10384A";

    /** Default bold white header text; explicit header overrides are applied above it. */
    private static final Style DEFAULT_HEADER_STYLE =
            Style.builder().bold().color("#FFFFFF").build();

    /** Immutable header strings that define the fixed column count. */
    private final List<String> headers;

    /** Appended data rows retained until each immutable table snapshot is built. */
    private final List<List<TableCell>> rows = new ArrayList<>();

    /** Current immutable relative column weights, initially equal for every column. */
    private List<Double> weights;

    /** Inherited table text/spacing style, or null for document inheritance. */
    private Style style;

    /** Current header overrides composed over the bold white defaults. */
    private Style headerStyle = DEFAULT_HEADER_STYLE;

    /** Current header-cell RGB fill; full syntax validation happens during generation. */
    private String headerBackground = DEFAULT_HEADER_BACKGROUND;

    /**
     * Creates a table whose header array fixes the number of columns. Header strings are
     * snapshotted; empty strings are accepted. At least one data row is needed before building.
     *
     * @param headers non-null array containing at least one non-null header string
     * @throws IllegalArgumentException if headers are null or empty, or contain null
     */
    public TableBuilder(String... headers) {
        BuilderChecks.required(headers, "table headers");
        BuilderChecks.require(headers.length > 0, "At least one table column is required");
        this.headers =
                Arrays.stream(headers)
                        .map(value -> BuilderChecks.required(value, "column header"))
                        .toList();
        weights = Collections.nCopies(headers.length, 1d);
    }

    /**
     * Appends one data row, copying its cell strings into plain text runs. Later header style
     * changes affect header cells only.
     *
     * @param values non-null array with one non-null string per column, in column order
     * @return this builder
     * @throws IllegalArgumentException if the array or a cell is null, or its length differs from
     *     the header count
     */
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

    /**
     * Replaces column widths with relative, unitless weights. For example, {@code 1, 2} gives the
     * second column twice the first column's width. The values are copied; equal weights are the
     * default.
     *
     * @param values one finite positive weight per column, with a finite sum
     * @return this builder
     * @throws IllegalArgumentException if the array is null, its length differs from the column
     *     count, any value is nonpositive or nonfinite, or their sum is nonfinite
     */
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

    /**
     * Replaces the table's inherited text and spacing style. Detailed style validation is deferred
     * to PDF generation.
     *
     * @param value non-null table style
     * @return this builder
     * @throws IllegalArgumentException if {@code value} is null
     */
    public TableBuilder style(Style value) {
        style = BuilderChecks.required(value, "table style");
        return this;
    }

    /**
     * Replaces the header style overrides, overlaid on the default bold white text. This does not
     * change the separately configured header background.
     *
     * @param value non-null style whose specified fields override the header defaults
     * @return this builder
     * @throws IllegalArgumentException if {@code value} is null
     */
    public TableBuilder headerStyle(Style value) {
        headerStyle = DEFAULT_HEADER_STYLE.overlay(BuilderChecks.required(value, "header style"));
        return this;
    }

    /**
     * Replaces the header-cell background color, initially {@code #10384A}. Color syntax is
     * validated during PDF generation.
     *
     * @param color non-null color string in the model's supported hex notation
     * @return this builder
     * @throws IllegalArgumentException if {@code color} is null
     */
    public TableBuilder headerBackground(String color) {
        headerBackground = BuilderChecks.required(color, "header background");
        return this;
    }

    /**
     * Snapshots headers, appended rows, relative widths and current styles into a table block. The
     * builder can be changed and built again without changing earlier blocks.
     *
     * @return an immutable table block for a letter body
     * @throws IllegalStateException if no data row has been added
     */
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

    /**
     * Wraps plain cell text in a single run while preserving cell style and fill settings.
     *
     * @param text non-null text previously checked by the public builder method
     * @param style cell overrides, or null to inherit table typography
     * @param background header fill, or null for an unfilled data cell
     * @return immutable cell with one plain text run
     */
    private static TableCell cell(String text, Style style, String background) {
        return new TableCell(List.of(new TextRun(text, null, null)), style, background);
    }
}
