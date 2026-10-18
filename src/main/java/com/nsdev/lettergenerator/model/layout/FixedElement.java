package com.nsdev.lettergenerator.model.layout;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.Block;
import java.util.List;

/**
 * Positioned content repeated on selected pages inside a reserved top or bottom zone. Unlike body
 * sections, fixed content cannot flow onto another page. Its bounds are relative to the selected
 * zone, not the physical page or body origin.
 *
 * <p>Generation requires unique nonblank identifiers, nonempty blocks, and bounds fitting the zone.
 * Elements in the same zone may touch but must not overlap on any selected page. Page-break blocks
 * are invalid here. Construction only snapshots the list.
 *
 * @param id document-unique label used to identify the element
 * @param zone reserved page region containing the element
 * @param pages pages on which to repeat this content
 * @param boundsMm rectangle relative to the zone's upper-left corner, in millimeters
 * @param blocks ordered region content; null is preserved for validation
 */
public record FixedElement(
        String id, Zone zone, PageSelector pages, Bounds boundsMm, List<Block> blocks) {
    /**
     * Snapshots content into an unmodifiable list while retaining null lists and entries. Geometry
     * and content are validated when the document is generated.
     *
     * @param id element identifier
     * @param zone containing region
     * @param pages page selection
     * @param boundsMm zone-relative rectangle in millimeters
     * @param blocks content to copy, or null
     */
    public FixedElement {
        blocks = ImmutableLists.copyNullable(blocks);
    }
}
