package com.nsdev.lettergenerator.render;

/** Identifies the origin of a contiguous range in a generated PDF pack. */
public enum RenderPartKind {
    /** Pages generated from the letter model. */
    LETTER,
    /** Pages copied from one explicitly supplied PDF attachment. */
    ATTACHMENT
}
