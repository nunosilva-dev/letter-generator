/**
 * Immutable input model for PDF letters. A {@link com.nsdev.lettergenerator.model.LetterDocument}
 * combines ordered body sections with page geometry, inherited typography, fixed elements, an
 * optional visual signature, and an optional watermark. Java builders and JSON input both produce
 * these records.
 *
 * <p>Construction snapshots list/byte data and applies only the defaults documented by each record.
 * It does not imply a valid renderable document: generation checks required content, fonts,
 * geometry, links, and images, and reports invalid fields through {@link
 * com.nsdev.lettergenerator.exception.InvalidLetterException}. Nulls preserved in required lists or
 * entries support those diagnostics; they are not valid empty content. See {@link
 * com.nsdev.lettergenerator.model.block.Block} and {@link
 * com.nsdev.lettergenerator.model.watermark.Watermark} for JSON discriminators.
 */
package com.nsdev.lettergenerator.model;
