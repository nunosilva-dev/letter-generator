package com.nsdev.lettergenerator.render;

import java.util.Objects;

/**
 * Per-call PDF output choices and optional authoring overlays.
 *
 * @param debug whether to draw declared layout rectangles over accepted PDF content
 * @param profile requested native output profile
 */
public record RenderOptions(boolean debug, OutputProfile profile) {
    /**
     * Requires an explicit profile; incompatible content is rejected before native rendering.
     *
     * @param debug authoring overlay flag
     * @param profile non-null native output profile
     */
    public RenderOptions {
        Objects.requireNonNull(profile, "Output profile is required");
    }

    /**
     * Preserves ordinary rendering and authoring previews through the original constructor.
     *
     * @param debug whether to draw declared layout rectangles
     */
    public RenderOptions(boolean debug) {
        this(debug, OutputProfile.STANDARD);
    }

    /**
     * @return ordinary rendering without overlays or debug geometry
     */
    public static RenderOptions defaults() {
        return new RenderOptions(false);
    }

    /**
     * @return rendering options that draw declared layout bounds for authoring previews
     */
    public static RenderOptions debugPreview() {
        return new RenderOptions(true);
    }
}
