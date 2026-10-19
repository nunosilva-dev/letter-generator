/**
 * Engine-independent XML writing and character rules. {@link
 * com.nsdev.lettergenerator.internal.xml.XmlBuilder} escapes text and attributes while serializers
 * build XSL-FO or FOP configuration. Each mutable writer belongs to one composition; callers
 * balance element boundaries and finish the writer before using its result.
 *
 * <p>This package does not parse user-facing HTML or invoke a rendering engine. {@link
 * com.nsdev.lettergenerator.internal.xml.XmlCharacters} provides the XML 1.0 character predicate
 * used by input and markup validation.
 */
package com.nsdev.lettergenerator.internal.xml;
