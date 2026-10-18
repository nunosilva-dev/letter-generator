package com.nsdev.lettergenerator.model.layout;

/**
 * Page selection for fixed elements and watermarks. Page indexes used by {@link
 * #matchesPageIndex(int)} are zero-based; a single-page document has only its first page.
 */
public enum PageSelector {
    /** Only the first page, including a document consisting of a single page. */
    FIRST,
    /** Every page after the first; selects nothing in a single-page document. */
    FOLLOWING,
    /** Every page, including the first. */
    ALL;

    /**
     * Tests this selection against a zero-based renderer page index.
     *
     * @param pageIndex zero for the first page, one for the second, and so on
     * @return true when selected; false for every negative index
     */
    public boolean matchesPageIndex(int pageIndex) {
        return switch (this) {
            case FIRST -> pageIndex == 0;
            case FOLLOWING -> pageIndex > 0;
            case ALL -> pageIndex >= 0;
        };
    }

    /**
     * Tests whether two selections can address the same page. FIRST and FOLLOWING are disjoint; ALL
     * can coincide with either. Fixed-element collision checks use this relation.
     *
     * @param other page selection to compare; callers should supply a nonnull selector
     * @return true if either selector is ALL or both selectors are identical
     */
    public boolean mayCoincideWith(PageSelector other) {
        return this == other || this == ALL || other == ALL;
    }
}
