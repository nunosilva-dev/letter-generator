package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.model.style.Style;

/**
 * Reference to a document-local reusable component. Preparation replaces the reference with an
 * ordinary group, applying reference styles below the component's own local block styles. JSON
 * selects this record with {@code type:"component"}.
 *
 * @param name registered component name, checked during generation
 * @param style nullable overrides inherited by the included content
 */
public record ComponentBlock(String name, Style style) implements Block {}
