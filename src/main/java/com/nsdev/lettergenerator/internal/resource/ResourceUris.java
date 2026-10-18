package com.nsdev.lettergenerator.internal.resource;

import java.net.URI;

/** Private URI namespace shared by composition and the deny-by-default resource resolver. */
public final class ResourceUris {
    /**
     * Base URI for generated FOP configuration; all allowed data is registered under the memory
     * scheme.
     */
    public static final URI BASE = URI.create("memory:/");

    /** Namespace prefix for image resources assigned by one FO composition. */
    private static final String IMAGE_PREFIX = "memory:/image/";

    /** Namespace prefix for font resources assigned by one FOP configuration. */
    private static final String FONT_PREFIX = "memory:/font/";

    /** Prevents instantiation of the shared URI naming convention. */
    private ResourceUris() {}

    /**
     * Creates a deterministic identifier for an image within one rendering operation.
     *
     * @param index zero-based resource index assigned by the composer or font configuration
     * @return private memory URI; callers must register its data with the resource resolver
     */
    public static URI image(int index) {
        return URI.create(IMAGE_PREFIX + index);
    }

    /**
     * Creates a deterministic identifier for a font within one rendering operation.
     *
     * @param index zero-based resource index assigned by the composer or font configuration
     * @return private memory URI; callers must register its data with the resource resolver
     */
    public static URI font(int index) {
        return URI.create(FONT_PREFIX + index);
    }
}
