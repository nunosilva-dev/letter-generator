package com.nsdev.lettergenerator.internal.render.fo;

final class FoLengths {
    private FoLengths() {}

    static String mm(double value) {
        return value + "mm";
    }

    static String pt(double value) {
        return value + "pt";
    }
}
