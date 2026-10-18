package com.nsdev.lettergenerator.internal.xml;

import com.nsdev.lettergenerator.exception.PdfGenerationException;
import java.io.StringWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Per-document structured XML writer used for FO and engine configuration.
 *
 * <p>Calls form a balanced start/attribute/text/end sequence. Attribute values and document text
 * are escaped by StAX. Namespace-aware instances declare their prefix on the first element. This
 * mutable writer belongs to one composition call and wraps serialization failures as PDF-generation
 * failures.
 */
public final class XmlBuilder {
    /** Character buffer owned by this XML composition. */
    private final StringWriter output = new StringWriter();

    /** StAX serializer that escapes element text and attribute values. */
    private final XMLStreamWriter writer;

    /** Prefix applied to elements when a namespace URI is configured. */
    private final String prefix;

    /** Namespace URI shared by all elements, or the empty string for unnamespaced output. */
    private final String namespace;

    /**
     * Whether the first element has been emitted, preventing duplicate root namespace declarations.
     */
    private boolean rootWritten;

    /**
     * Creates an unnamespaced writer for internal FOP configuration XML.
     *
     * @throws PdfGenerationException if the StAX writer cannot be created
     */
    public XmlBuilder() {
        this("", "");
    }

    /**
     * Creates a writer that qualifies all elements with one namespace.
     *
     * @param prefix XML prefix; empty for unnamespaced output
     * @param namespace namespace URI; empty for unnamespaced output
     * @throws PdfGenerationException if the StAX writer cannot be created
     */
    public XmlBuilder(String prefix, String namespace) {
        this.prefix = prefix;
        this.namespace = namespace;
        try {
            writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
        } catch (XMLStreamException exception) {
            throw failure(exception);
        }
    }

    /**
     * Opens an element under the current parent and declares the namespace on the first element.
     *
     * @param name trusted XML local name supplied by the serializer
     * @return this writer
     * @throws PdfGenerationException if the start element or namespace cannot be serialized
     */
    public XmlBuilder start(String name) {
        write(
                () -> {
                    if (namespace.isEmpty()) {
                        writer.writeStartElement(name);
                    } else {
                        writer.writeStartElement(prefix, name, namespace);
                        if (!rootWritten) {
                            writer.writeNamespace(prefix, namespace);
                        }
                    }
                    rootWritten = true;
                });
        return this;
    }

    /**
     * Writes an escaped attribute on the current open start tag.
     *
     * @param name trusted attribute name
     * @param value non-null value converted using {@code toString()}
     * @return this writer
     * @throws PdfGenerationException if the attribute cannot be serialized
     * @throws NullPointerException if value is null
     */
    public XmlBuilder attribute(String name, Object value) {
        write(() -> writer.writeAttribute(name, value.toString()));
        return this;
    }

    /**
     * Appends escaped character data; callers never pre-escape user content.
     *
     * @param text character data for the current element
     * @return this writer
     * @throws PdfGenerationException if character data cannot be serialized
     */
    public XmlBuilder text(String text) {
        write(() -> writer.writeCharacters(text));
        return this;
    }

    /**
     * Closes the most recently opened element.
     *
     * @return this writer
     * @throws PdfGenerationException if the current element cannot be closed
     */
    public XmlBuilder end() {
        write(writer::writeEndElement);
        return this;
    }

    /**
     * Closes the underlying writer after the caller has balanced all element boundaries.
     *
     * @return complete XML accumulated by this instance
     * @throws PdfGenerationException if XML serialization fails
     */
    public String finish() {
        write(writer::close);
        return output.toString();
    }

    /**
     * Runs one StAX operation and preserves its checked failure as a PDF-generation exception.
     *
     * @param action StAX operation to run against the current writer
     * @throws PdfGenerationException if the callback reports an XML serialization error
     */
    private void write(XmlAction action) {
        try {
            action.write();
        } catch (XMLStreamException exception) {
            throw failure(exception);
        }
    }

    /**
     * Creates the common engine-boundary error without discarding the StAX cause.
     *
     * @param cause original checked StAX failure
     * @return generation exception retaining the cause
     */
    private static PdfGenerationException failure(XMLStreamException cause) {
        return new PdfGenerationException("Unable to serialize internal XML", cause);
    }

    /** Checked StAX callback used to centralize exception translation. */
    @FunctionalInterface
    private interface XmlAction {
        /**
         * Performs one XML operation.
         *
         * @throws XMLStreamException if the underlying StAX writer rejects the operation
         */
        void write() throws XMLStreamException;
    }
}
