package com.nsdev.lettergenerator.builder;

import com.nsdev.lettergenerator.internal.content.PlainText;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.PositionedContent;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Mutable, call-local authoring builder producing owned immutable canvas models. */
public final class CanvasBuilder {
    /** Declared canvas width in millimeters. */
    private final double widthMm;

    /** Declared canvas height in millimeters. */
    private final double heightMm;

    /** Stable insertion order is also the native paint order. */
    private final List<PositionedContent> elements = new ArrayList<>();

    /** Nullable canvas-level inherited typography and spacing. */
    private Style style;

    /**
     * Creates an empty canvas declaration; generation performs path-aware geometry validation.
     *
     * @param widthMm declared width in millimeters
     * @param heightMm declared height in millimeters
     */
    public CanvasBuilder(double widthMm, double heightMm) {
        this.widthMm = widthMm;
        this.heightMm = heightMm;
    }

    /**
     * Appends bounded rich content with a stable identifier and inherited canvas typography.
     *
     * @param id identifier unique within the resulting canvas
     * @param bounds local millimeter rectangle
     * @param blocks ordered rich content, snapshotted immediately
     * @return this builder
     */
    public CanvasBuilder at(String id, Bounds bounds, Block... blocks) {
        elements.add(
                new PositionedContent(
                        id, bounds, blocks == null ? null : Arrays.asList(blocks), null));
        return this;
    }

    /**
     * Appends bounded rich content using explicit physical coordinates.
     *
     * @param id stable identifier
     * @param xMm left position
     * @param yMm top position
     * @param width declared element width
     * @param height declared element height
     * @param blocks ordered rich content
     * @return this builder
     */
    public CanvasBuilder at(
            String id, double xMm, double yMm, double width, double height, Block... blocks) {
        return at(id, new Bounds(xMm, yMm, width, height), blocks);
    }

    /**
     * Appends plain text converted through the ordinary letter paragraph rules.
     *
     * @param id stable identifier
     * @param bounds local millimeter rectangle
     * @param text literal text; markup is never interpreted
     * @return this builder
     */
    public CanvasBuilder text(String id, Bounds bounds, String text) {
        elements.add(new PositionedContent(id, bounds, PlainText.body(text), null));
        return this;
    }

    /**
     * Appends an already constructed immutable positioned element.
     *
     * @param element positioned content, checked during generation
     * @return this builder
     */
    public CanvasBuilder element(PositionedContent element) {
        elements.add(element);
        return this;
    }

    /**
     * Sets optional inherited canvas typography and outer spacing/pagination.
     *
     * @param value nullable block overrides
     * @return this builder
     */
    public CanvasBuilder style(Style value) {
        style = value;
        return this;
    }

    /**
     * Returns an immutable snapshot; later builder mutations do not alter this model.
     *
     * @return canvas ready for ordinary block insertion
     */
    public CanvasBlock build() {
        return new CanvasBlock(widthMm, heightMm, elements, style);
    }
}
