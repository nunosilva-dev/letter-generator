/**
 * Coordinates the transition from validated models to accepted PDF bytes.
 *
 * <p>Start with {@link com.nsdev.lettergenerator.internal.render.DocumentRenderer}. It creates an
 * FO composition, invokes the FOP boundary, accepts or rejects typed diagnostics and applies the
 * optional watermark and declared-geometry debug overlay. Complete render results report actual
 * page count and accepted recovery diagnostics. A positive signature reservation allows one
 * targeted last-page retry; other failures are terminal. Mutable engine state is created per
 * attempt, not retained by the generator.
 *
 * @see com.nsdev.lettergenerator.internal.render.fo.FoComposer
 * @see com.nsdev.lettergenerator.internal.render.fop.FopRenderResult
 * @see com.nsdev.lettergenerator.internal.validation.InputValidator
 */
package com.nsdev.lettergenerator.internal.render;
