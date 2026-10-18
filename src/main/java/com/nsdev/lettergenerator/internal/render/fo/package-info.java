/**
 * Serializes the immutable letter model into XSL Formatting Objects (FO) and an image lookup.
 *
 * <p>{@link com.nsdev.lettergenerator.internal.render.fo.FoComposer} assembles page masters, static
 * regions and ordered body flow. Package-private writers divide page geometry, blocks, tables,
 * resolved styles and explicit page anchors. Text remains native FO text and is escaped through the
 * structured XML writer; this package does not create PDFBox objects or run the layout engine.
 * Dimensions retain explicit millimeter or point units until FOP consumes the document.
 *
 * @see com.nsdev.lettergenerator.internal.render.fop.FopRenderer
 * @see com.nsdev.lettergenerator.internal.xml.XmlBuilder
 */
package com.nsdev.lettergenerator.internal.render.fo;
