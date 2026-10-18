package com.nsdev.lettergenerator.model.text;

import com.nsdev.lettergenerator.model.style.Style;

/**
 * Literal inline text with optional style overrides and an external hyperlink. Text is XML-escaped
 * during rendering, and spaces and line feeds are preserved by text blocks; it is never interpreted
 * as HTML markup. Styles apply after the containing block or cell.
 *
 * <p>Construction performs no validation. Generation permits empty text but requires nonnull
 * XML-compatible text. A supplied link must be a syntactically valid absolute HTTP, HTTPS, or
 * mailto URI with a nonempty scheme-specific part; HTTP(S) links also require a host.
 *
 * @param text literal text, including any spaces or line feeds
 * @param style optional run-level overrides, or null to inherit from the block or cell
 * @param href external hyperlink target, or null for unlinked text
 */
public record TextRun(String text, Style style, String href) {}
