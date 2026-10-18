package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.model.style.Style;

/**
 * Explicit page boundary in the body flow. Generation rejects this block in fixed elements and
 * visual signatures. It has no style or text and does not create a new section.
 */
public record PageBreakBlock() implements Block {
    /**
     * Returns no style because a page boundary has no rendered content.
     *
     * @return null
     */
    @Override
    public Style style() {
        return null;
    }
}
