package com.nsdev.lettergenerator.internal.render.fop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.xmlgraphics.io.Resource;
import org.apache.xmlgraphics.io.ResourceResolver;

/**
 * FOP resource boundary backed by a per-render allowlist.
 *
 * <p>The composer and font configuration create private memory URIs. A lookup succeeds only when
 * that exact URI was registered; unknown file/network URIs and every write request fail. Suppliers
 * must return a fresh stream for each lookup because FOP may reopen a resource.
 */
final class MemoryResourceResolver implements ResourceResolver {
    /**
     * Immutable per-render URI allowlist; suppliers retain responsibility for creating fresh
     * streams.
     */
    private final Map<URI, Supplier<InputStream>> resources;

    /**
     * Snapshots the lookup map while retaining the supplied stream factories.
     *
     * @param resources exact memory URIs mapped to repeatable input-stream suppliers
     * @throws NullPointerException if the map, any key, or any supplier is null
     */
    MemoryResourceResolver(Map<URI, Supplier<InputStream>> resources) {
        this.resources = Map.copyOf(resources);
    }

    /**
     * Opens one previously registered resource; never falls back to external resolution.
     *
     * @param uri exact resource identifier emitted into generated FO or font configuration
     * @return wrapper around a fresh stream supplied by the allowlist
     * @throws IOException if the URI is absent
     * @throws NullPointerException if uri is null
     */
    @Override
    public Resource getResource(URI uri) throws IOException {
        Supplier<InputStream> source = resources.get(uri);
        if (source == null) {
            throw new IOException("External resource access denied: " + uri.getScheme());
        }
        return new Resource(source.get());
    }

    /**
     * Rejects every engine request to create or overwrite an external resource.
     *
     * @param uri requested destination
     * @return never returns normally
     * @throws IOException always, because resource writes are disabled
     */
    @Override
    public OutputStream getOutputStream(URI uri) throws IOException {
        throw new IOException("Resource writes are disabled");
    }
}
