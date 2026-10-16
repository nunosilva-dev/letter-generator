package com.nsdev.lettergenerator.model.layout;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.Block;
import java.util.List;

/** Visual signature appended to flow or anchored in the final-page signature zone. */
public record Signature(SignaturePlacement placement, List<Block> blocks, Bounds boundsMm) {
    public Signature {
        blocks = ImmutableLists.copyNullable(blocks);
    }
}
