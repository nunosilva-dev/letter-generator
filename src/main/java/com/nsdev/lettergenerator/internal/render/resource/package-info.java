/**
 * Private resource identifiers shared by FO composition and FOP font configuration. {@link
 * com.nsdev.lettergenerator.internal.render.resource.ResourceUris} assigns deterministic {@code
 * memory:} identifiers to in-memory images and registered fonts. Each render registers its bytes
 * under these identifiers; the FOP package owns resolution through a per-attempt allowlist.
 *
 * <p>This package defines identifiers only. It never opens files or network resources, creates
 * streams, or supplies fallback lookup behavior.
 */
package com.nsdev.lettergenerator.internal.render.resource;
