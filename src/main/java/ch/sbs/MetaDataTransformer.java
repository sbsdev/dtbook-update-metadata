package ch.sbs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.input.BOMInputStream;

public class MetaDataTransformer {
    static final String dtb = "http://www.daisy.org/z3986/2005/dtbook/";
    static final String brl = "http://www.daisy.org/z3986/2009/braille/";

    static final QName metaData;

    static {
	metaData = new QName(dtb, "meta");
    }

    XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    public MetaDataTransformer() {
    }

    public static void main(String[] args) {
	if (args.length < 1) {
	    System.out.println("Usage: Specify XML File Name");
	    System.exit(1);
	}

	MetaDataTransformer transformer = new MetaDataTransformer();

	try {
	    transformer.transform(new BOMInputStream(new FileInputStream(args[0])), System.out);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void transform(InputStream input, OutputStream output)
	throws XMLStreamException {

	transform(XMLInputFactory.newInstance().createXMLEventReader(input),
		  XMLOutputFactory.newInstance().createXMLEventWriter(output));
    }

    public void transform(Reader input, Writer output)
	throws XMLStreamException {

	transform(XMLInputFactory.newInstance().createXMLEventReader(input),
		  XMLOutputFactory.newInstance().createXMLEventWriter(output));
    }

    public void transform(XMLEventReader reader, XMLEventWriter writer)	throws XMLStreamException {

	boolean insideMetaData = false;

	while (reader.hasNext()) {

	    XMLEvent event = reader.nextEvent();

	    if (event.isStartElement()) {
		if (event.asStartElement().getName().equals(metaData)) {
		    insideMetaData = true;
		}
		writer.add(event);
	    } else if (event.isEndElement()) {
		if (event.asEndElement().getName().equals(metaData)) {
		    insideMetaData = false;
		}
		writer.add(event);
	    } else if (event.isCharacters()) {
		if (insideMetaData) {
		    writer.add(eventFactory.createCharacters("foo"));
		} else {
		    writer.add(event);
		}
	    } else {
		writer.add(event);
	    }
	}
	writer.flush();
    }
}
