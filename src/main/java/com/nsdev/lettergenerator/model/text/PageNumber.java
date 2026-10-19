package com.nsdev.lettergenerator.model.text;

/** Dynamic physical page field evaluated by the renderer after pagination. */
public enum PageNumber {
    /** Number of the page on which the run is painted, starting at one. */
    CURRENT,
    /** Total physical pages, including an allocated signature-recovery page. */
    TOTAL
}
