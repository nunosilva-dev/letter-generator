/**
 * Per-attempt Apache FOP rendering and typed layout diagnostics. {@link
 * com.nsdev.lettergenerator.internal.render.fop.FopRenderer} consumes {@link
 * com.nsdev.lettergenerator.internal.render.fo.FoComposer.Composition} with a fresh factory, memory
 * resolver and engine state, then returns {@link
 * com.nsdev.lettergenerator.internal.render.fop.FopRenderResult} to {@link
 * com.nsdev.lettergenerator.internal.render.DocumentRenderer} for acceptance or its narrow
 * body-overflow recovery decision. Engine events and painted glyph/underline bounds are checked
 * independently; diagnostic message text never decides retry eligibility.
 */
package com.nsdev.lettergenerator.internal.render.fop;
