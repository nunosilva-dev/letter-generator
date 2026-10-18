/**
 * Reads the supported HTML fragment and inline-CSS vocabulary into immutable model blocks. {@link
 * com.nsdev.lettergenerator.internal.markup.HtmlParser} parses fragments for both {@link
 * com.nsdev.lettergenerator.builder.LetterBuilder} and the template compiler; {@link
 * com.nsdev.lettergenerator.internal.markup.HtmlBlocks} dispatches block children, while inline
 * text and tables use package-private readers. This is a deliberately restricted markup language,
 * not a browser layout engine: unsupported elements, attributes, CSS and parser errors are rejected
 * through {@link com.nsdev.lettergenerator.internal.markup.MarkupSupport}.
 *
 * <p>{@link com.nsdev.lettergenerator.internal.markup.HtmlStyles} overlays inherited text
 * properties, semantic tag defaults, then local CSS; paragraph spacing does not inherit. Image
 * sources resolve only against the caller's named byte map, through {@link
 * com.nsdev.lettergenerator.internal.markup.HtmlImages}; markup never opens paths or URLs.
 * Dimensions become millimeters and font sizes become points. Link and full document validation
 * continue after parsing in the shared validation pipeline.
 */
package com.nsdev.lettergenerator.internal.markup;
