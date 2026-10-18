/**
 * Pre-render validation of model structure and declared content constraints. {@link
 * com.nsdev.lettergenerator.internal.validation.InputValidator} is the entry point used by the PDF
 * generator for Java, JSON, and template-derived documents. It delegates geometry, nullable styles,
 * block/run content, images, and links to focused package-private validators.
 *
 * <p>Validation resolves style inheritance to check font variants, confirms reserved rectangles fit
 * and do not collide, and enforces body-only page breaks. Failures use {@link
 * com.nsdev.lettergenerator.exception.InvalidLetterException} with a model-field path. These checks
 * precede FO composition; actual pagination, overflow, and painted glyph bounds are checked later
 * by the rendering pipeline.
 */
package com.nsdev.lettergenerator.internal.validation;
