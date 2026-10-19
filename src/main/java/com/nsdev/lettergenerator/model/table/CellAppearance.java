package com.nsdev.lettergenerator.model.table;

/**
 * Nullable cell appearance overrides. Each property inherits independently, first from table
 * defaults and then from the legacy defaults. Text properties remain in the cell's Style.
 *
 * @param border optional uniform border, in points
 * @param padding optional per-edge inner spacing, in millimeters
 * @param verticalAlignment optional content alignment
 * @param backgroundColor optional six-digit RGB fill; null inherits the table fill
 */
public record CellAppearance(
        Border border,
        Padding padding,
        VerticalAlignment verticalAlignment,
        String backgroundColor) {
    /** Complete legacy defaults; the background remains transparent. */
    public static final CellAppearance DEFAULT =
            new CellAppearance(Border.DEFAULT, Padding.DEFAULT, VerticalAlignment.MIDDLE, null);

    /**
     * Overlays specified local properties independently, including zero padding and no border.
     *
     * @param local optional local appearance
     * @return combined immutable appearance, or this instance when local is null
     */
    public CellAppearance overlay(CellAppearance local) {
        return local == null
                ? this
                : new CellAppearance(
                        local.border == null ? border : local.border,
                        local.padding == null ? padding : local.padding,
                        local.verticalAlignment == null
                                ? verticalAlignment
                                : local.verticalAlignment,
                        local.backgroundColor == null ? backgroundColor : local.backgroundColor);
    }
}
