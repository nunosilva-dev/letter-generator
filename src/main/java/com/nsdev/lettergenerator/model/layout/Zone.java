package com.nsdev.lettergenerator.model.layout;

/** Reserved region for positioned fixed elements, inside the outer page margins. */
public enum Zone {
    /** Region above the body, measured downward from the top inner-page edge. */
    TOP,
    /**
     * Region below the body, measured downward from its own top edge; below the last-page signature
     * zone.
     */
    BOTTOM
}
