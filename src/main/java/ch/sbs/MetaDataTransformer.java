package ch.sbs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.input.BOMInputStream;

public class MetaDataTransformer {
	private static final Map<String, String> keys;
	static {
		Map<String, String> tmp = new HashMap<String, String>();
		tmp.put("dc:Title", "TITLE");
		tmp.put("dc:Creator", "CREATOR");
		tmp.put("dc:Subject", "SUBJECT");
		tmp.put("dc:Description", "DESCRIPTION");
		tmp.put("dc:Publisher", "PUBLISHER");
		tmp.put("dc:Date", "DATE");
		tmp.put("dc:Type", "TYPE");
		tmp.put("dc:Format", "FORMAT");
		tmp.put("dc:Identifier", "IDENTIFIER");
		tmp.put("dc:Source", "SOURCE");
		tmp.put("dc:Language", "LANGUAGE");
		tmp.put("dc:Rights", "RIGHTS");
		tmp.put("dtb:uid", "UID");
		tmp.put("dtb:sourceEdition", "SOURCEEDITION");
		tmp.put("dtb:sourcePublisher", "SOURCEPUBLISHER");
		tmp.put("dtb:sourceRights", "SOURCERIGHTS");
		tmp.put("prod:series", "PRODSERIES");
		tmp.put("prod:seriesNumber", "PRODSERIESNUMBER");
		tmp.put("prod:source", "PRODSOURCE");
		keys = Collections.unmodifiableMap(tmp);
	}

	private static final String dtb = "http://www.daisy.org/z3986/2005/dtbook/";

	private static final QName metaData = new QName(dtb, "meta");
	private static final QName metaName = new QName("name");

	XMLEventFactory eventFactory = XMLEventFactory.newInstance();

	private static void changeAttribute(XMLEventWriter writer, XMLEventFactory eventFactory, String name, String value)
			throws XMLStreamException {
		writer.add(eventFactory.createStartElement("", dtb, "meta"));
		writer.add(eventFactory.createAttribute("name", name));
		writer.add(eventFactory.createAttribute("content", value));

	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: Specify XML File Name");
			System.exit(1);
		}
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);

		try {
			XMLEventReader reader = inputFactory.createXMLEventReader(new BOMInputStream(new FileInputStream(args[0])));
			XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(System.out);
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();

			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();

				if (event.isStartElement() && event.asStartElement().getName().equals(metaData)
						&& event.asStartElement().getAttributeByName(metaName) != null) {
					String name = event.asStartElement().getAttributeByName(metaName).getValue();
					boolean found = false;
					for (Map.Entry<String, String> entry : keys.entrySet()) {
						if (name.equalsIgnoreCase(entry.getKey()) && System.getProperty(entry.getValue()) != null) {
							changeAttribute(writer, eventFactory, name, System.getProperty(entry.getValue()));
							found = true;
							break;
						}
					}
					if (!found) {
						writer.add(event);
					}
				} else {
					writer.add(event);
				}
			}

			writer.flush();

		} catch (

		FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

}
