package com.nsdev.lettergenerator.internal.xml;

import com.nsdev.lettergenerator.exception.PdfGenerationException;
import java.io.StringWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/** Structured XML output: attributes and text are escaped by the JDK XML writer. */
public final class XmlBuilder {
    private final StringWriter output = new StringWriter();
    private final XMLStreamWriter writer;
    private final String prefix;
    private final String namespace;
    private boolean rootWritten;

    public XmlBuilder() {
        this("", "");
    }

    public XmlBuilder(String prefix, String namespace) {
        this.prefix = prefix;
        this.namespace = namespace;
        try {
            writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
        } catch (XMLStreamException exception) {
            throw failure(exception);
        }
    }

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

    public XmlBuilder attribute(String name, Object value) {
        write(() -> writer.writeAttribute(name, value.toString()));
        return this;
    }

    public XmlBuilder text(String text) {
        write(() -> writer.writeCharacters(text));
        return this;
    }

    public XmlBuilder end() {
        write(writer::writeEndElement);
        return this;
    }

    public String finish() {
        write(writer::close);
        return output.toString();
    }

    private void write(XmlAction action) {
        try {
            action.write();
        } catch (XMLStreamException exception) {
            throw failure(exception);
        }
    }

    private static PdfGenerationException failure(XMLStreamException cause) {
        return new PdfGenerationException("Unable to serialize internal XML", cause);
    }

    @FunctionalInterface
    private interface XmlAction {
        void write() throws XMLStreamException;
    }
}
