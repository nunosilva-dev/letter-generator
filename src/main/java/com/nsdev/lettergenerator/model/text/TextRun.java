package com.nsdev.lettergenerator.model.text;

import com.nsdev.lettergenerator.model.style.Style;

/** Literal text with optional style overrides and an http, https, or mailto link. */
public record TextRun(String text, Style style, String href) {}
