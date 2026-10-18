/**
 * Snapshots trusted font configuration for validation and rendering. {@link
 * com.nsdev.lettergenerator.internal.font.FontSource} describes a Java-configured file or classpath
 * resource. {@link com.nsdev.lettergenerator.internal.font.FontRegistry#load(java.util.List)} reads
 * and verifies TrueType outlines once, requires a regular face for every custom family, and rejects
 * duplicate faces or replacements of built-in families.
 *
 * <p>{@link com.nsdev.lettergenerator.internal.font.RegisteredFont} owns copied bytes and supplies
 * fresh streams to renderers; registry snapshots can be shared across generation calls. {@link
 * com.nsdev.lettergenerator.internal.font.StandardFontFamilies} centralizes the built-in
 * identifiers. Documents name registered families and variants; they cannot select font files.
 */
package com.nsdev.lettergenerator.internal.font;
