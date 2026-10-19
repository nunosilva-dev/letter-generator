/**
 * Mutable convenience builders that produce independent immutable model snapshots. Start with
 * {@link com.nsdev.lettergenerator.Letter} or {@link com.nsdev.lettergenerator.Table}. Use {@link
 * com.nsdev.lettergenerator.builder.LetterBuilder} for ordered Java content, {@link
 * com.nsdev.lettergenerator.builder.HtmlLetterBuilder} for supported letter markup, and {@link
 * com.nsdev.lettergenerator.builder.TableBuilder} for text tables. Build snapshots before passing
 * them to {@link com.nsdev.lettergenerator.LetterPdfGenerator}; font and complete document
 * validation belong to generation.
 *
 * <p>Deferred Java/HTML region content is resolved by {@link
 * com.nsdev.lettergenerator.internal.builder.content} against the builder's final settings.
 */
package com.nsdev.lettergenerator.builder;
