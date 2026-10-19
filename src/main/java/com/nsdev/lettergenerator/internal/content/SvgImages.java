package com.nsdev.lettergenerator.internal.content;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Secure entry point for a bounded, static SVG subset with local fragment references only. */
public final class SvgImages {
    /** Maximum supplied SVG encoding length, before any XML parsing or native image processing. */
    private static final int MAX_BYTES = 2_000_000;

    /**
     * Intrinsic viewport aspect-ratio dimensions expressed in CSS pixels.
     *
     * @param width positive finite width
     * @param height positive finite height
     */
    public record Dimensions(double width, double height) {}

    /** Prevents construction of this stateless SVG helper. */
    private SvgImages() {}

    /**
     * Parses SVG with all DTD/entity/external access disabled and validates the complete static
     * allowlist before any rendering engine receives the bytes. Stylesheets, executable content,
     * animation, filters, foreign namespaces, external references and cyclic references are
     * rejected.
     *
     * @param data encoded SVG bytes, read synchronously without retaining them
     * @return positive intrinsic dimensions resolved from absolute sizes and/or viewBox
     * @throws IllegalArgumentException if XML, content, references or geometry are unsupported
     */
    public static Dimensions inspect(byte[] data) {
        return SvgGeometry.dimensions(read(data).getDocumentElement());
    }

    /**
     * Produces private native-rendering bytes with an explicit intrinsic viewport. Batik otherwise
     * selects a default viewport for viewBox-only or partially dimensioned sources, changing the
     * painted size inside the declared image box. The caller's original bytes remain unchanged.
     *
     * @param data owned source bytes, validated again before native processing
     * @return independently encoded static SVG with absolute CSS-pixel viewport dimensions
     * @throws IllegalArgumentException for invalid input or serialization failure
     */
    public static byte[] renderingBytes(byte[] data) {
        Document document = read(data);
        var root = document.getDocumentElement();
        Dimensions size = SvgGeometry.dimensions(root);
        root.setAttribute("width", size.width() + "px");
        root.setAttribute("height", size.height() + "px");
        try {
            TransformerFactory factory = TransformerFactory.newDefaultInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            var output = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(document), new StreamResult(output));
            return output.toByteArray();
        } catch (TransformerException exception) {
            throw new IllegalArgumentException(
                    "Unable to encode the SVG rendering viewport", exception);
        }
    }

    /**
     * Parses and validates a fresh owned tree with no external resource resolution.
     *
     * @param data bounded encoded static SVG
     * @return validated tree owned by this call
     * @throws IllegalArgumentException for unsafe XML or unsupported static content
     */
    private static Document read(byte[] data) {
        if (data == null || data.length == 0 || data.length > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "SVG bytes must be nonempty and at most 2000000 bytes");
        }
        try (var input = new ByteArrayInputStream(data)) {
            var factory = DocumentBuilderFactory.newDefaultInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            builder.setEntityResolver(
                    (publicId, systemId) -> {
                        throw new SAXException("SVG external entities are forbidden");
                    });
            Document document = builder.parse(input);
            SvgSanitizer.validate(document);
            SvgGeometry.dimensions(document.getDocumentElement());
            return document;
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new IllegalArgumentException("Invalid or unsafe SVG", exception);
        }
    }
}
