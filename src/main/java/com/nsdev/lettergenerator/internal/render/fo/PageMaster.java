package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.model.layout.PageSelector;
import java.util.List;

/** FOP master identifiers and the first/last-page semantics they encode. */
public enum PageMaster {
    ONLY("only", "only", true, true),
    FIRST("first", "first", true, false),
    MIDDLE("middle", "rest", false, false),
    LAST("last", "last", false, true);

    public static final List<PageMaster> SELECTION_ORDER = List.of(ONLY, FIRST, LAST, MIDDLE);
    private final String identifier;
    private final String pagePosition;
    private final boolean firstPage;
    private final boolean lastPage;

    PageMaster(String identifier, String pagePosition, boolean firstPage, boolean lastPage) {
        this.identifier = identifier;
        this.pagePosition = pagePosition;
        this.firstPage = firstPage;
        this.lastPage = lastPage;
    }

    public String identifier() {
        return identifier;
    }

    public String pagePosition() {
        return pagePosition;
    }

    public boolean isLastPage() {
        return lastPage;
    }

    public String topRegion() {
        return "top-" + identifier;
    }

    public String bottomRegion() {
        return "bottom-" + identifier;
    }

    public boolean matches(PageSelector selector) {
        return switch (selector) {
            case ALL -> true;
            case FIRST -> firstPage;
            case FOLLOWING -> !firstPage;
        };
    }

    public static PageMaster fromIdentifier(String identifier) {
        for (PageMaster master : values()) {
            if (master.identifier.equals(identifier)) {
                return master;
            }
        }
        throw new IllegalArgumentException("Unknown generated page master: " + identifier);
    }
}
