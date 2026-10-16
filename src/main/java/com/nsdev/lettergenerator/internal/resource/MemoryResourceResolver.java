package com.nsdev.lettergenerator.internal.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.xmlgraphics.io.Resource;
import org.apache.xmlgraphics.io.ResourceResolver;

/** Allows only resources explicitly supplied for this rendering call. */
public final class MemoryResourceResolver implements ResourceResolver {
    private final Map<URI, Supplier<InputStream>> resources;

    public MemoryResourceResolver(Map<URI, Supplier<InputStream>> resources) {
        this.resources = Map.copyOf(resources);
    }

    @Override
    public Resource getResource(URI uri) throws IOException {
        Supplier<InputStream> source = resources.get(uri);
        if (source == null) {
            throw new IOException("External resource access denied: " + uri.getScheme());
        }
        return new Resource(source.get());
    }

    @Override
    public OutputStream getOutputStream(URI uri) throws IOException {
        throw new IOException("Resource writes are disabled");
    }
}
