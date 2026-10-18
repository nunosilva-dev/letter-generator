/**
 * Structured generation and restricted consumption of internal XML. {@link
 * com.nsdev.lettergenerator.internal.xml.XmlBuilder} escapes text and attributes while serializers
 * build XSL-FO or FOP configuration. Each mutable writer belongs to one composition; callers
 * balance element boundaries and finish the writer before using its result.
 *
 * <p>{@link com.nsdev.lettergenerator.internal.xml.SecureFoTransformer} sends generated FO to the
 * rendering engine through a namespace-aware SAX identity transform with DTDs, external entities,
 * and external stylesheets disabled. This package does not parse user-facing HTML. {@link
 * com.nsdev.lettergenerator.internal.xml.XmlCharacters} provides the XML 1.0 character predicate
 * used by input and markup validation.
 */
package com.nsdev.lettergenerator.internal.xml;
