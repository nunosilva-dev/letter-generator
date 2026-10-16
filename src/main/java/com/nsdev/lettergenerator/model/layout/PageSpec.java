package com.nsdev.lettergenerator.model.layout;

/** Page size and reserved zones. Null properties select the documented defaults. */
public record PageSpec(
        PageSize size,
        Margins marginsMm,
        Double topZoneMm,
        Double bottomZoneMm,
        Double signatureZoneMm) {
    private static final double DEFAULT_ZONE_MM = 0.0;
    public static final PageSpec A4 = new PageSpec(PageSize.A4, null, null, null, null);
    public static final PageSpec LETTER = new PageSpec(PageSize.LETTER, null, null, null, null);

    public PageSpec {
        size = size == null ? PageSize.A4 : size;
        marginsMm = marginsMm == null ? Margins.defaults() : marginsMm;
        topZoneMm = topZoneMm == null ? DEFAULT_ZONE_MM : topZoneMm;
        bottomZoneMm = bottomZoneMm == null ? DEFAULT_ZONE_MM : bottomZoneMm;
        signatureZoneMm = signatureZoneMm == null ? DEFAULT_ZONE_MM : signatureZoneMm;
    }

    public static PageSpec defaults() {
        return A4;
    }
}
