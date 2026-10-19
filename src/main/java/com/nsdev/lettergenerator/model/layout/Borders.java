package com.nsdev.lettergenerator.model.layout;

import com.nsdev.lettergenerator.model.table.Border;

/**
 * Independent optional physical borders for a flowing box.
 *
 * @param top nullable top border; null means no border
 * @param right nullable right border; null means no border
 * @param bottom nullable bottom border; null means no border
 * @param left nullable left border; null means no border
 */
public record Borders(Border top, Border right, Border bottom, Border left) {
    /**
     * Applies an immutable border value to every edge.
     *
     * @param border nullable uniform border
     * @return independent edge declarations with the same appearance
     */
    public static Borders uniform(Border border) {
        return new Borders(border, border, border, border);
    }
}
