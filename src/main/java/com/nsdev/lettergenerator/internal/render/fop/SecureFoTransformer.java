package com.nsdev.lettergenerator.internal.render.fop;

import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/** Parses only generated FO; external entities, DTDs, and stylesheets are prohibited. */
final class SecureFoTransformer {
    /** SAX feature rejecting any DOCTYPE declaration. */
    private static final String DISALLOW_DOCTYPE =
            "http://apache.org/xml/features/disallow-doctype-decl";

    /** SAX feature disabling external general entities. */
    private static final String EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";

    /** SAX feature disabling external parameter entities. */
    private static final String EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";

    /** SAX feature disabling external DTD loading. */
    private static final String LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    /** Prevents instantiation of the secure identity-transform boundary. */
    private SecureFoTransformer() {}

    /**
     * Streams generated XSL-FO into the FOP SAX handler using a hardened identity transform.
     *
     * <p>DTD declarations, external entities and external stylesheets are prohibited. This is an
     * engine boundary for XML produced by {@link XmlBuilder}; it is not the user-facing HTML
     * parser.
     *
     * @param xml complete generated FO document
     * @param handler destination supplied by the current FOP rendering operation
     * @throws SAXException if the secure XML reader or its required features cannot be created
     * @throws ParserConfigurationException if a secure SAX parser cannot be configured
     * @throws TransformerException if transform configuration, XML parsing, or delivery to the
     *     handler fails, including rejection of prohibited XML
     */
    static void transform(String xml, ContentHandler handler)
            throws SAXException, ParserConfigurationException, TransformerException {
        XMLReader reader = secureReader();
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        factory.newTransformer()
                .transform(
                        new SAXSource(reader, new InputSource(new StringReader(xml))),
                        new SAXResult(handler));
    }

    /**
     * Creates a namespace-aware parser that rejects DOCTYPE declarations and all external entity
     * resolution.
     *
     * @return namespace-aware XML reader with external resolution disabled
     * @throws ParserConfigurationException if the parser factory cannot satisfy secure settings
     * @throws SAXException if a required feature or XML reader is unavailable
     */
    private static XMLReader secureReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature(DISALLOW_DOCTYPE, true);
        factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
        factory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
        factory.setFeature(LOAD_EXTERNAL_DTD, false);
        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setEntityResolver(
                (publicId, systemId) -> {
                    throw new SAXException("External entities are disabled");
                });
        return reader;
    }
}
