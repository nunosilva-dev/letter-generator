/**
 * Implementation packages behind the supported letter-building and PDF-generation APIs.
 *
 * <p>Read the pipeline in this order: builders create the model; template/markup or JSON input
 * readers adapt external representations; validation checks declared fields; render.fo serializes
 * FO; render.fop lays it out and checks painted bounds; render.watermark optionally decorates the
 * accepted PDF. Font, resource, style, geometry and XML packages provide shared infrastructure.
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
