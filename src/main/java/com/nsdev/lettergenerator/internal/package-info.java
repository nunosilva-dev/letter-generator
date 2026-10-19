/**
 * Implementation packages behind the supported letter-building and PDF-generation APIs.
 *
 * <p>Read the pipeline in this order: builders create the model; template/markup or JSON input
 * readers adapt external representations; validation checks declared fields; render.fo serializes
 * FO; render.fop lays it out and checks painted bounds; render.watermark optionally decorates the
 * accepted PDF. Content conversion, font, style, geometry, utility and XML packages provide shared
 * infrastructure. Cross-input default policies live in internal.defaults. Engine resource
 * identifiers live under render.resource; the FOP integration owns its resource resolver and
 * hardened FO transform.
 *
 * <p>Types in this hierarchy are implementation details even when public visibility is needed
 * between packages. Applications should use the root entry points, public builders and model types.
 * Internal names and engine-specific signatures may change independently.
 *
 * @see com.nsdev.lettergenerator.Letter
 * @see com.nsdev.lettergenerator.LetterPdfGenerator
 * @see com.nsdev.lettergenerator.model.LetterDocument
 * @see com.nsdev.lettergenerator.internal.template.TemplateCompiler
 * @see com.nsdev.lettergenerator.internal.validation.InputValidator
 * @see com.nsdev.lettergenerator.internal.render.DocumentRenderer
 */
package com.nsdev.lettergenerator.internal;
