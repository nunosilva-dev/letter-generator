package com.nsdev.lettergenerator.theme;

import com.nsdev.lettergenerator.builder.StationeryBuilder;
import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import java.util.List;

/**
 * Immutable header, footer and logo geometry shared by letters using a theme. Geometry is relative
 * to the inner page rectangle; choose the same paper and margins when authoring the stationery.
 * Letter-local regions with the same stable identifier replace inherited regions.
 *
 * @param elements fixed top/bottom elements, defensively snapshotted
 * @param topZoneMm reserved top height including the desired body gap
 * @param bottomZoneMm reserved bottom height including the desired body gap
 */
public record Stationery(List<FixedElement> elements, double topZoneMm, double bottomZoneMm) {
    /**
     * Snapshots registrations without validating geometry.
     *
     * @param elements nullable fixed registrations
     * @param topZoneMm reserved top zone
     * @param bottomZoneMm reserved bottom zone
     */
    public Stationery {
        elements = elements == null ? List.of() : ImmutableLists.copyNullable(elements);
    }

    /**
     * @return independent convenience builder using ordinary letter defaults
     */
    public static StationeryBuilder builder() {
        return new StationeryBuilder();
    }
}
