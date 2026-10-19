/**
 * Default policies shared across input APIs. {@link
 * com.nsdev.lettergenerator.internal.defaults.LetterLayoutDefaults} keeps Java and HTML letter
 * geometry aligned; {@link com.nsdev.lettergenerator.internal.defaults.WatermarkDefaults} keeps
 * image bounds and opacity aligned between model factories and markup.
 *
 * <p>Each holder owns one policy domain and contains only values with multiple consumers that must
 * agree. Constants used by one class, independent values that happen to be equal, and external
 * schema/diagnostic vocabulary remain beside their own behavior. These internal policies are not
 * application configuration APIs.
 */
package com.nsdev.lettergenerator.internal.defaults;
