/**
 * Per-attempt Apache FOP rendering and typed layout diagnostics. {@link
 * com.nsdev.lettergenerator.internal.render.fop.FopRenderer} consumes {@link
 * com.nsdev.lettergenerator.internal.render.fo.FoComposer.Composition} with a fresh factory, memory
 * resolver and engine state, then returns {@link
 * com.nsdev.lettergenerator.internal.render.fop.FopRenderResult} to {@link
 * com.nsdev.lettergenerator.internal.render.DocumentRenderer} for acceptance or its narrow
 * body-overflow recovery decision. Engine events and painted glyph/underline bounds are checked
 * independently; diagnostic message text never decides retry eligibility.
 *
 * <p>Package-private {@link com.nsdev.lettergenerator.internal.render.fop.MemoryResourceResolver}
 * confines resource reads to the current attempt's registered memory URIs and rejects writes.
 * Package-private {@link com.nsdev.lettergenerator.internal.render.fop.SecureFoTransformer}
 * delivers generated FO through a hardened SAX identity transform that prohibits DTDs, external
 * entities and external stylesheets. These engine boundaries are used only by the FOP integration.
 */
package com.nsdev.lettergenerator.internal.render.fop;
