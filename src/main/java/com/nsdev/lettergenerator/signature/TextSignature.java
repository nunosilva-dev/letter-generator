package com.nsdev.lettergenerator.signature;

import com.nsdev.lettergenerator.internal.builder.PlainText;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;

/**
 * Immutable plain-text signature part: single newlines change lines and blank lines separate
 * paragraphs. Form-feed page breaks are forbidden. Instances can be safely reused; {@link
 * #style(Style)} returns a new part rather than changing the receiver.
 */
public final class TextSignature implements SignatureContent {
    /** Original plain text, validated to exclude null and form-feed page breaks. */
    private final String text;

    /** Paragraph overrides, or null to inherit document typography. */
    private final Style style;

    /**
     * Validates plain region text before retaining its text and optional style overrides.
     *
     * @param text non-null plain text without form-feed page breaks
     * @param style paragraph overrides, or null to inherit document defaults
     * @throws IllegalArgumentException if text is null or contains a form-feed page break
     */
    private TextSignature(String text, Style style) {
        PlainText.region(style, text);
        this.text = text;
        this.style = style;
    }

    /**
     * Creates a signature with inherited document typography and no explicit style overrides. CRLF
     * and CR are normalized when producing paragraph blocks.
     *
     * @param text non-null plain text without form-feed page breaks
     * @return a new immutable text signature
     * @throws IllegalArgumentException if text is null or contains a form-feed page break
     */
    public static TextSignature of(String text) {
        return new TextSignature(text, null);
    }

    /**
     * Creates a copy with replacement style overrides for all signature paragraphs. Unspecified
     * fields inherit document typography; passing null removes explicit overrides. Detailed style
     * validity is checked when generating the PDF.
     *
     * @param style paragraph style overrides, or null to inherit the document style
     * @return a new signature; this instance is unchanged
     */
    public TextSignature style(Style style) {
        return new TextSignature(text, style);
    }

    /** {@inheritDoc} */
    @Override
    public List<Block> blocks() {
        return PlainText.region(style, text);
    }
}
