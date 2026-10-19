package com.nsdev.lettergenerator.render;

import java.util.Objects;

/**
 * One contiguous source part in output order; page numbers are one-based.
 *
 * @param kind generated letter or appended PDF
 * @param firstPage first page in the complete result
 * @param pageCount number of pages belonging to this part
 */
public record RenderPart(RenderPartKind kind, int firstPage, int pageCount) {
    /**
     * Checks that the part denotes a nonempty positive page range.
     *
     * @param kind non-null source kind
     * @param firstPage positive one-based start
     * @param pageCount positive part size
     */
    public RenderPart {
        Objects.requireNonNull(kind, "Part kind is required");
        if (firstPage < 1 || pageCount < 1) {
            throw new IllegalArgumentException("Part pages must be positive");
        }
    }
}
