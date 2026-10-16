package com.nsdev.lettergenerator.font;

/** Required font face for resolved bold/italic styling. */
public enum FontVariant {
    REGULAR(false, false),
    BOLD(true, false),
    ITALIC(false, true),
    BOLD_ITALIC(true, true);

    private final boolean bold;
    private final boolean italic;

    FontVariant(boolean bold, boolean italic) {
        this.bold = bold;
        this.italic = italic;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public static FontVariant fromStyle(boolean bold, boolean italic) {
        if (bold) {
            return italic ? BOLD_ITALIC : BOLD;
        }
        return italic ? ITALIC : REGULAR;
    }
}
