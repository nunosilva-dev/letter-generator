/**
 * Content blocks shared by body sections, fixed elements, and visual signatures. The sealed {@link
 * com.nsdev.lettergenerator.model.block.Block} hierarchy supplies flowing text, headings, lists,
 * images, rich tables, style/keep groups, semantic letter parts, bounded canvases, independent
 * columns, decorated boxes and document-local component references. Explicit page breaks are
 * permitted only in body sections. JSON identifies variants with the {@code type} property.
 *
 * <p>Collections are snapshotted into unmodifiable lists, preserving nulls for generation-time
 * validation except for documented optional-list defaults. Images protect their byte arrays with
 * defensive copies. Constructing a block does not validate renderability.
 */
package com.nsdev.lettergenerator.model.block;
