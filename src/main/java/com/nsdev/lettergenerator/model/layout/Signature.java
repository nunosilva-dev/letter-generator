package com.nsdev.lettergenerator.model.layout;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.Block;
import java.util.List;

/**
 * Visual signature content, either appended to the body or positioned on the final page. This is
 * rendered letter content, not a cryptographic PDF signature. FLOW requires null bounds;
 * BOTTOM_LAST requires bounds inside a positive-height {@link PageSpec#signatureZoneMm() signature
 * zone}. Page-break blocks are invalid in both modes.
 *
 * @param placement flowing or final-page placement; required during generation
 * @param blocks nonempty signature content; null is retained for generation-time validation
 * @param boundsMm zone-relative rectangle in millimeters for BOTTOM_LAST, or null for FLOW
 */
public record Signature(SignaturePlacement placement, List<Block> blocks, Bounds boundsMm) {
    /**
     * Snapshots the blocks without checking placement, bounds, or content. Null lists and entries
     * are preserved for generation-time validation.
     *
     * @param placement signature placement mode
     * @param blocks content to copy into an unmodifiable list, or null
     * @param boundsMm final-page signature-zone rectangle, or null for flow
     */
    public Signature {
        blocks = ImmutableLists.copyNullable(blocks);
    }
}
