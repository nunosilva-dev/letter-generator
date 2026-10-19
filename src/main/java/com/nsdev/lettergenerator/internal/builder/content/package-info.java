/**
 * Deferred content resolution for the public fluent letter builder. The sealed {@link
 * com.nsdev.lettergenerator.internal.builder.content.ContentPart} contract explicitly permits four
 * package-private records, each responsible for one input kind. {@link
 * com.nsdev.lettergenerator.internal.builder.content.ContentParts} creates these parts without
 * exposing their concrete types to the builder package.
 *
 * <p>Resolution uses final region dimensions in millimeters, named image assets and inherited
 * styles to produce immutable model blocks. Shared style adaptation preserves explicit block
 * overrides. Public types here exist for communication between library packages and are not
 * supported application APIs.
 */
package com.nsdev.lettergenerator.internal.builder.content;
