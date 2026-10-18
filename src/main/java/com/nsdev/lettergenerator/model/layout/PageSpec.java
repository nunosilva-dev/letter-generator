package com.nsdev.lettergenerator.model.layout;

/**
 * Physical page layout with reserved regions inside the page margins. The top zone is above the
 * body and the bottom zone is below it. On the last page, the signature zone sits above the bottom
 * zone and reduces the available body height by its own height, even if no signature is supplied.
 * All zones span the width remaining between left/right margins.
 *
 * <p>The constructor replaces nulls with defaults but does not check dimensions. Generation
 * requires finite nonnegative margins/zones and positive body space on ordinary and last pages.
 *
 * @param size portrait paper size; null becomes {@link PageSize#A4}
 * @param marginsMm outer page insets; null becomes 20 mm on every side
 * @param topZoneMm reserved height above body content, in millimeters; null becomes zero
 * @param bottomZoneMm reserved height below body content, in millimeters; null becomes zero
 * @param signatureZoneMm additional reserved last-page height above the bottom zone, in
 *     millimeters; null becomes zero
 */
public record PageSpec(
        PageSize size,
        Margins marginsMm,
        Double topZoneMm,
        Double bottomZoneMm,
        Double signatureZoneMm) {
    /** Height used for any omitted reserved region, in millimeters. */
    private static final double DEFAULT_ZONE_MM = 0.0;

    /** A4 with 20 mm margins and zero-height reserved zones. */
    public static final PageSpec A4 = new PageSpec(PageSize.A4, null, null, null, null);

    /** US Letter with 20 mm margins and zero-height reserved zones. */
    public static final PageSpec LETTER = new PageSpec(PageSize.LETTER, null, null, null, null);

    /**
     * Replaces omitted layout properties with defaults; numeric validity is checked during PDF
     * generation.
     *
     * @param size paper size, or null for A4
     * @param marginsMm margins, or null for 20 mm on all sides
     * @param topZoneMm top region height in millimeters, or null for zero
     * @param bottomZoneMm bottom region height in millimeters, or null for zero
     * @param signatureZoneMm last-page signature region height in millimeters, or null for zero
     */
    public PageSpec {
        size = size == null ? PageSize.A4 : size;
        marginsMm = marginsMm == null ? Margins.defaults() : marginsMm;
        topZoneMm = topZoneMm == null ? DEFAULT_ZONE_MM : topZoneMm;
        bottomZoneMm = bottomZoneMm == null ? DEFAULT_ZONE_MM : bottomZoneMm;
        signatureZoneMm = signatureZoneMm == null ? DEFAULT_ZONE_MM : signatureZoneMm;
    }

    /**
     * Returns the default A4 specification.
     *
     * @return {@link #A4}, with 20 mm margins and no reserved zones
     */
    public static PageSpec defaults() {
        return A4;
    }
}
