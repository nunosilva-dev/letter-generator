package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.style.Style;

/** Serializes explicitly requested page controls without changing legacy defaults. */
final class FoPaginationWriter {
    /** Prevents instances of this page-control writer. */
    private FoPaginationWriter() {}

    /**
     * Writes only explicit controls, layering them over the block's ordinary renderer policy.
     *
     * @param xml stream with an open block/table start tag
     * @param style raw nullable overrides
     */
    static void write(XmlBuilder xml, Style style) {
        if (style == null) {
            return;
        }
        if (style.keepTogether() != null) {
            xml.attribute("keep-together.within-page", strength(style.keepTogether()));
        }
        if (style.keepWithNext() != null) {
            xml.attribute("keep-with-next.within-page", strength(style.keepWithNext()));
        }
        if (style.widows() != null) {
            xml.attribute("widows", style.widows());
        }
        if (style.orphans() != null) {
            xml.attribute("orphans", style.orphans());
        }
    }

    /**
     * Maps a model keep strength to its native FO spelling.
     *
     * @param keep non-null keep strength
     * @return auto, a soft priority, or always
     */
    static String strength(Keep keep) {
        return switch (keep) {
            case AUTO -> "auto";
            case PREFER -> "1";
            case ALWAYS -> "always";
        };
    }
}
