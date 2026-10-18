/**
 * Private resource identifiers and the FOP resource-access boundary. {@link
 * com.nsdev.lettergenerator.internal.resource.ResourceUris} assigns deterministic {@code memory:}
 * identifiers to in-memory images and registered fonts. FO composition and font configuration
 * register stream suppliers under those identifiers for the current render.
 *
 * <p>{@link com.nsdev.lettergenerator.internal.resource.MemoryResourceResolver} snapshots that
 * allowlist and opens only exact registered keys. It never falls back to file or network resolution
 * and rejects every output-stream request. Suppliers must return fresh streams because the engine
 * can reopen the same resource.
 */
package com.nsdev.lettergenerator.internal.resource;
