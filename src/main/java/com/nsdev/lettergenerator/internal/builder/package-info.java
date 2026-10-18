/**
 * Internal compilation helpers for the public fluent {@link
 * com.nsdev.lettergenerator.builder.LetterBuilder}. {@link
 * com.nsdev.lettergenerator.internal.builder.PlainText} and {@link
 * com.nsdev.lettergenerator.internal.builder.ImageBlocks} turn convenience inputs into model
 * blocks; markup conversion lives in {@link com.nsdev.lettergenerator.internal.markup}. {@link
 * com.nsdev.lettergenerator.internal.builder.HeaderLayout} defers header placement until the final
 * logo and page width are known, and {@link
 * com.nsdev.lettergenerator.internal.builder.LetterRegions} produces explicit fixed regions and
 * body reservations. Geometry here is in millimeters.
 *
 * <p>{@link com.nsdev.lettergenerator.internal.builder.BuilderChecks} reports immediate argument
 * errors as {@link java.lang.IllegalArgumentException}; the shared document validator still checks
 * the assembled model before rendering. Helpers return snapshots rather than retaining mutable
 * builder output lists; image decoding defensively copies caller bytes.
 */
package com.nsdev.lettergenerator.internal.builder;
