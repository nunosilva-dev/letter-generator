package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.model.layout.PageSelector;
import java.util.List;

/**
 * The four first/last-page combinations used by FO composition and verified after rendering.
 *
 * <p>These identifiers connect generated page masters with FOP's actual page-viewport reports. They
 * are internal layout states, distinct from the public fixed-content {@link PageSelector} values.
 */
public enum PageMaster {
    /** A single-page letter: both first-page and last-page content applies. */
    ONLY("only", "only", true, true),
    /** The first page of a letter that continues onto another page. */
    FIRST("first", "first", true, false),
    /** A page that is neither the first nor the final page. */
    MIDDLE("middle", "rest", false, false),
    /** The final page of a multipage letter, including its reserved signature zone. */
    LAST("last", "last", false, true);

    /**
     * Ordered master alternatives: specific only/first/last states must precede the generic
     * rest-page fallback.
     */
    public static final List<PageMaster> SELECTION_ORDER = List.of(ONLY, FIRST, LAST, MIDDLE);

    /** Stable master name shared by FO serialization and viewport verification. */
    private final String identifier;

    /** FO conditional-page-master-reference position token. */
    private final String pagePosition;

    /** Whether FIRST-selected fixed elements apply. */
    private final boolean firstPage;

    /** Whether final-page body/signature reservations apply. */
    private final boolean lastPage;

    /**
     * Associates FO names/selectors with first/last-page behavior.
     *
     * @param identifier stable generated master name
     * @param pagePosition FO page-position token
     * @param firstPage whether first-page fixed content applies
     * @param lastPage whether last-page body/signature reservations apply
     */
    PageMaster(String identifier, String pagePosition, boolean firstPage, boolean lastPage) {
        this.identifier = identifier;
        this.pagePosition = pagePosition;
        this.firstPage = firstPage;
        this.lastPage = lastPage;
    }

    /**
     * Returns the stable internal FO master name.
     *
     * @return identifier written into the generated FO and reported by FOP
     */
    public String identifier() {
        return identifier;
    }

    /**
     * Returns the FO page-position selector used by the master alternative.
     *
     * @return only, first, rest or last
     */
    public String pagePosition() {
        return pagePosition;
    }

    /**
     * Indicates whether this master reserves final-page signature space.
     *
     * @return true for ONLY and LAST
     */
    public boolean isLastPage() {
        return lastPage;
    }

    /**
     * Names this master's independent before/static-content flow.
     *
     * @return generated top-region flow name
     */
    public String topRegion() {
        return "top-" + identifier;
    }

    /**
     * Names this master's independent after/static-content flow.
     *
     * @return generated bottom-region flow name
     */
    public String bottomRegion() {
        return "bottom-" + identifier;
    }

    /**
     * Evaluates a public fixed-content selector against this master's first-page state.
     *
     * @param selector non-null selector for a fixed element
     * @return whether the element belongs in this master
     */
    public boolean matches(PageSelector selector) {
        return switch (selector) {
            case ALL -> true;
            case FIRST -> firstPage;
            case FOLLOWING -> !firstPage;
        };
    }

    /**
     * Maps a page viewport's generated master name back to its layout state.
     *
     * @param identifier master name emitted by the composer
     * @return matching internal master
     * @throws IllegalArgumentException if FOP reports an unknown master name
     */
    public static PageMaster fromIdentifier(String identifier) {
        for (PageMaster master : values()) {
            if (master.identifier.equals(identifier)) {
                return master;
            }
        }
        throw new IllegalArgumentException("Unknown generated page master: " + identifier);
    }
}
