/**
 * Unchecked failures exposed by {@link com.nsdev.lettergenerator.LetterPdfGenerator}. {@link
 * com.nsdev.lettergenerator.exception.InvalidLetterException} carries an input field path, while
 * {@link com.nsdev.lettergenerator.exception.PdfGenerationException} represents PDF composition,
 * layout or post-processing failure. Both extend {@link
 * com.nsdev.lettergenerator.exception.LetterPdfException}. Convenience builders can also reject
 * immediate arguments with standard Java argument/state exceptions.
 */
package com.nsdev.lettergenerator.exception;
