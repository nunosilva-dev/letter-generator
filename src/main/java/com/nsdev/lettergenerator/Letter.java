package com.nsdev.lettergenerator;

import com.nsdev.lettergenerator.builder.HtmlLetterBuilder;
import com.nsdev.lettergenerator.builder.LetterBuilder;

/**
 * Starting point for content-first letters and the supported HTML letter template format. Use
 * {@link #builder()} for Java content, {@link #template(String)} for markup with scalar bindings,
 * or {@link #fromHtml(String)} for already-resolved markup. Build a {@link
 * com.nsdev.lettergenerator.model.LetterDocument} and pass it to {@link
 * LetterPdfGenerator#generate(com.nsdev.lettergenerator.model.LetterDocument)}.
 *
 * <p>Java builders default to A4 paper, 20 mm margins and the library's letter typography. Builders
 * are mutable; their built document snapshots are immutable.
 */
public final class Letter {
    /**
     * Form-feed marker accepted by {@link
     * com.nsdev.lettergenerator.builder.LetterBuilder#body(String...)}. Use it as a separate body
     * argument or inside a body string to insert a page break. Header, footer and plain signature
     * text reject this marker.
     */
    public static final String PAGE_BREAK = "\f";

    /** Prevents construction of this static entry-point utility. */
    private Letter() {}

    /**
     * Creates a fresh mutable Java letter builder with default page, region and text settings.
     *
     * @return an independent builder with no body content
     */
    public static LetterBuilder builder() {
        return new LetterBuilder();
    }

    /**
     * Starts a template builder that substitutes {@code ${name}} bindings in parsed text and
     * attribute values. Replacement text cannot introduce markup nodes. The source must use the
     * supported single {@code <letter>} root format, checked when built.
     *
     * @param markup non-null letter template source
     * @return a fresh builder that accepts placeholder values and named image assets
     * @throws IllegalArgumentException if {@code markup} is null
     */
    public static HtmlLetterBuilder template(String markup) {
        return HtmlLetterBuilder.template(markup);
    }

    /**
     * Starts a builder for already-resolved markup in the supported {@code <letter>} format.
     * Placeholder substitution is disabled; supplying values to this builder is rejected. Parsing
     * and markup validation occur when the builder is built.
     *
     * @param resolvedMarkup non-null letter markup whose values have already been resolved
     * @return a fresh builder that accepts named image assets
     * @throws IllegalArgumentException if {@code resolvedMarkup} is null
     */
    public static HtmlLetterBuilder fromHtml(String resolvedMarkup) {
        return HtmlLetterBuilder.resolvedHtml(resolvedMarkup);
    }
}
