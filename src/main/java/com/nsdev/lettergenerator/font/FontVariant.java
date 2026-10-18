package com.nsdev.lettergenerator.font;

/**
 * Physical font face selected from resolved bold and italic style flags. Custom families register
 * their actual faces through {@link
 * com.nsdev.lettergenerator.LetterPdfGenerator.Builder#registerFont(String, FontVariant,
 * java.nio.file.Path)}. A regular face is required for each custom family, and a requested
 * bold/italic variant must also be registered; the generator does not synthesize missing custom
 * faces.
 */
public enum FontVariant {
    /** Upright, normal-weight face; required for every custom font family. */
    REGULAR(false, false),
    /** Bold, upright face. */
    BOLD(true, false),
    /** Italic, normal-weight face. */
    ITALIC(false, true),
    /** Face combining bold weight and italic posture. */
    BOLD_ITALIC(true, true);

    /** Whether this face supplies the bold weight requested by resolved text styles. */
    private final boolean bold;

    /** Whether this face supplies the italic posture requested by resolved text styles. */
    private final boolean italic;

    /**
     * Stores the style flags represented by this physical font face.
     *
     * @param bold whether this face supplies bold weight
     * @param italic whether this face supplies italic posture
     */
    FontVariant(boolean bold, boolean italic) {
        this.bold = bold;
        this.italic = italic;
    }

    /**
     * Reports whether this physical face supplies bold weight.
     *
     * @return true for {@link #BOLD} and {@link #BOLD_ITALIC}
     */
    public boolean isBold() {
        return bold;
    }

    /**
     * Reports whether this physical face supplies italic posture.
     *
     * @return true for {@link #ITALIC} and {@link #BOLD_ITALIC}
     */
    public boolean isItalic() {
        return italic;
    }

    /**
     * Maps resolved style flags to the exact physical face required for rendering.
     *
     * @param bold whether the resolved text style requests bold weight
     * @param italic whether the resolved text style requests italic posture
     * @return the matching variant, or {@link #REGULAR} when both flags are false
     */
    public static FontVariant fromStyle(boolean bold, boolean italic) {
        if (bold) {
            return italic ? BOLD_ITALIC : BOLD;
        }
        return italic ? ITALIC : REGULAR;
    }
}
