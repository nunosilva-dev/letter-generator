/**
 * Physical faces for custom font registration with {@link
 * com.nsdev.lettergenerator.LetterPdfGenerator.Builder}. {@link
 * com.nsdev.lettergenerator.font.FontVariant} maps the resolved bold/italic style to a registered
 * TrueType face. Resources are loaded when the generator builder is built, so immutable generators
 * can be shared across documents without rereading font files.
 */
package com.nsdev.lettergenerator.font;
