package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.model.style.Style;

/** Explicit page boundary in the body flow; invalid inside fixed regions or signatures. */
public record PageBreakBlock() implements Block {
    @Override
    public Style style() {
        return null;
    }
}
