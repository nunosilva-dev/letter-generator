package com.nsdev.lettergenerator.model.layout;

/**
 * Controls whether a visual signature follows body flow or occupies a reserved last-page region.
 */
public enum SignaturePlacement {
    /** Append the signature after all body sections; bounds must be null. */
    FLOW,
    /**
     * Place the signature in its bounds on the final page; requires a positive signature-zone
     * height.
     */
    BOTTOM_LAST
}
