package com.nsdev.lettergenerator.signature;

import com.nsdev.lettergenerator.internal.builder.PlainText;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.List;

/** Plain signature text: newlines change lines and blank lines separate paragraphs. */
public final class TextSignature implements SignatureContent {
    private final String text;
    private final Style style;

    private TextSignature(String text, Style style) {
        PlainText.region(style, text);
        this.text = text;
        this.style = style;
    }

    public static TextSignature of(String text) {
        return new TextSignature(text, null);
    }

    public TextSignature style(Style style) {
        return new TextSignature(text, style);
    }

    @Override
    public List<Block> blocks() {
        return PlainText.region(style, text);
    }
}
