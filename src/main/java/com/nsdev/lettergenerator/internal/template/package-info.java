/**
 * Compiles a complete {@code letter} template into the shared document model. {@link
 * com.nsdev.lettergenerator.builder.HtmlLetterBuilder} supplies markup, scalar values and named
 * image bytes to {@link com.nsdev.lettergenerator.internal.template.TemplateCompiler}. The compiler
 * parses first, substitutes literal values in existing nodes, reads page defaults, and dispatches
 * regions through an immutable registry of stateless, package-private handlers. Each call creates a
 * fresh builder and context; there is no public custom-tag registration API.
 *
 * <p>Regions execute in logo, main, header, footer, signature, watermark order, independent of
 * their source order. Blocks within a region keep source order. Reading uses {@link
 * com.nsdev.lettergenerator.internal.markup.HtmlBlocks}; {@link
 * com.nsdev.lettergenerator.builder.LetterBuilder#build()} resolves final geometry, while the
 * generator performs complete shared-model validation before rendering. Unknown/duplicate regions,
 * unsupported attributes and invalid substitutions fail with template paths rather than being
 * silently ignored.
 */
package com.nsdev.lettergenerator.internal.template;
