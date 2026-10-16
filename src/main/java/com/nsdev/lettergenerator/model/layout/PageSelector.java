package com.nsdev.lettergenerator.model.layout;

/** Pages selected for a fixed element or watermark. */
public enum PageSelector {
    FIRST,
    FOLLOWING,
    ALL;

    /** Matches a zero-based page index, as used by the rendering engines. */
    public boolean matchesPageIndex(int pageIndex) {
        return switch (this) {
            case FIRST -> pageIndex == 0;
            case FOLLOWING -> pageIndex > 0;
            case ALL -> pageIndex >= 0;
        };
    }

    public boolean mayCoincideWith(PageSelector other) {
        return this == other || this == ALL || other == ALL;
    }
}
