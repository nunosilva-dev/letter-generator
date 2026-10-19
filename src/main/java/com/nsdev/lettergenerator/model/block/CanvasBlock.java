package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.layout.PositionedContent;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;

/**
 * A fixed-size flow unit containing arbitrary bounded elements in stable paint order. It consumes
 * its declared height and moves to a following page as one unit when necessary. Native content
 * overflow fails generation instead of clipping or shrinking the canvas.
 *
 * @param widthMm positive finite canvas width, no larger than available width
 * @param heightMm positive finite canvas height
 * @param elements ordered owned element list with unique identifiers
 * @param style nullable inherited typography and outer spacing/pagination overrides
 */
public record CanvasBlock(
        double widthMm, double heightMm, List<PositionedContent> elements, Style style)
        implements Block {
    /**
     * Snapshots elements while deferring geometry and content validation until generation.
     *
     * @param widthMm declared width
     * @param heightMm declared height
     * @param elements source element list
     * @param style nullable block overrides
     */
    public CanvasBlock {
        elements = ImmutableLists.copyNullable(elements);
    }
}
