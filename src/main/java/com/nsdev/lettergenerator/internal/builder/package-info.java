/**
 * Internal compilation helpers for the public fluent {@link
 * com.nsdev.lettergenerator.builder.LetterBuilder}. {@link
 * com.nsdev.lettergenerator.internal.builder.HeaderLayout} defers header placement until the final
 * logo and page width are known, and {@link
 * com.nsdev.lettergenerator.internal.builder.LetterRegions} produces explicit fixed regions and
 * body reservations. Geometry here is in millimeters. Deferred content creation, resolution and
 * inherited styling live in {@link com.nsdev.lettergenerator.internal.builder.content}.
 *
 * <p>Shared immediate text/image conversion lives in {@link
 * com.nsdev.lettergenerator.internal.content}; markup conversion lives in {@link
 * com.nsdev.lettergenerator.internal.markup}. {@link
 * com.nsdev.lettergenerator.internal.util.ArgumentChecks} reports immediate argument errors as
 * {@link java.lang.IllegalArgumentException}; the shared document validator still checks the
 * assembled model before rendering.
 */
package com.nsdev.lettergenerator.internal.builder;
