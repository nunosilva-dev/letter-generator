package com.nsdev.lettergenerator.internal.resource;

import java.net.URI;

/** Private URI namespace shared by composition and the deny-by-default resource resolver. */
public final class ResourceUris {
    public static final URI BASE = URI.create("memory:/");
    private static final String IMAGE_PREFIX = "memory:/image/";
    private static final String FONT_PREFIX = "memory:/font/";

    private ResourceUris() {}

    public static URI image(int index) {
        return URI.create(IMAGE_PREFIX + index);
    }

    public static URI font(int index) {
        return URI.create(FONT_PREFIX + index);
    }
}
