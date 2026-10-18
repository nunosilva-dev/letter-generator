/**
 * Public entry points for letter construction and PDF generation. Start with {@link
 * com.nsdev.lettergenerator.Letter} and {@link com.nsdev.lettergenerator.Table}; their {@link
 * com.nsdev.lettergenerator.builder} builders produce immutable {@link
 * com.nsdev.lettergenerator.model.LetterDocument} snapshots. {@link
 * com.nsdev.lettergenerator.LetterPdfGenerator} validates and renders those snapshots or strict
 * JSON, and can be reused concurrently. See {@link com.nsdev.lettergenerator.signature} for
 * signature factories, {@link com.nsdev.lettergenerator.font} for custom font faces, and {@link
 * com.nsdev.lettergenerator.exception} for validation and rendering failures.
 */
package com.nsdev.lettergenerator;
