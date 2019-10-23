package ch.sbs;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.input.BOMInputStream;

public class MetaDataTransformer {
	/**
	 * Valid meta data keys.
	 */
	public static final String[] keys = { "dc:Title", "dc:Creator", "dc:Subject", "dc:Description", "dc:Publisher",
			"dc:Date", "dc:Type", "dc:Format", "dc:Identifier", "dc:Source", "dc:Language", "dc:Rights", "dtb:uid",
			"dtb:sourceEdition", "dtb:sourcePublisher", "dtb:sourceRights", "prod:series", "prod:seriesNumber",
			"prod:source" };
	/**
	 * Valid production meta data keys. If any of those keys are missing in the meta
	 * data of the input XML they will be added.
	 */
	public static final String[] productionKeys = { "prod:series", "prod:seriesNumber", "prod:source" };

	private static final String dtb = "http://www.daisy.org/z3986/2005/dtbook/";
	private static final String xml = "http://www.w3.org/XML/1998/namespace";

	private static final QName head = new QName(dtb, "head");

	private static final QName metaData = new QName(dtb, "meta");
	private static final QName metaName = new QName("name");

	private static final QName dtbook = new QName(dtb, "dtbook");
	private static final QName language_attr = new QName(xml, "lang");

	XMLEventFactory eventFactory = XMLEventFactory.newInstance();

	private static void changeMetaAttribute(XMLEventWriter writer, XMLEventFactory eventFactory, String name,
			String value) throws XMLStreamException {
		writer.add(eventFactory.createStartElement("", dtb, "meta"));
		writer.add(eventFactory.createAttribute("name", name));
		writer.add(eventFactory.createAttribute("content", value));
	}

	/**
	 * Transform a given (XML) InputStream to an (XML) OutputStream. If any
	 * properties are given in `env` then update the meta data in the XML
	 * accordingly. The rest of the XML is copied as is. Unlike a normal transformer
	 * DTD entities are not expanded.
	 * 
	 * @param in  an InputStream containing DTBook XML
	 * @param out an OutputStream containing possibly updated DTBook XML
	 * @param env a map of meta data. For any given value the key is updated in the
	 *            DTBook XML
	 * @throws XMLStreamException
	 * 
	 */
	public static void transform(InputStream in, OutputStream out, Properties env) throws XMLStreamException {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
		inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

		XMLEventReader reader = inputFactory.createXMLEventReader(new BOMInputStream(in));
		XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(out);
		XMLEventFactory eventFactory = XMLEventFactory.newInstance();

		String language = env.getProperty("dc:Language");

		Set<String> seen = new HashSet<String>();

		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();

			// update xml:lang attribute
			if (language != null && event.isStartElement() && event.asStartElement().getName().equals(dtbook)
					&& event.asStartElement().getAttributeByName(language_attr) != null
					&& event.asStartElement().getAttributeByName(language_attr).getValue() != language) {
				StartElement elem = event.asStartElement();
				writer.add(eventFactory.createStartElement(dtbook, null, null));

				Iterator<Namespace> namespaces = elem.getNamespaces();
				while (namespaces.hasNext()) {
					Namespace namespace = namespaces.next();
					writer.add(eventFactory.createNamespace(namespace.getPrefix(), namespace.getNamespaceURI()));
				}
				Iterator<Attribute> attributes = elem.getAttributes();
				while (attributes.hasNext()) {
					Attribute attribute = attributes.next();
					QName name = attribute.getName();
					writer.add(eventFactory.createAttribute(name.getPrefix(), name.getNamespaceURI(),
							name.getLocalPart(), (name.getLocalPart() == "lang") ? language : attribute.getValue()));
				}
			}
			// update the meta data elements
			else if (event.isStartElement() && event.asStartElement().getName().equals(metaData)
					&& event.asStartElement().getAttributeByName(metaName) != null) {
				String name = event.asStartElement().getAttributeByName(metaName).getValue();
				seen.add(name);

				boolean newValueGiven = false;
				for (String entry : keys) {
					if (name.equals(entry) && env.getProperty(entry) != null) {
						changeMetaAttribute(writer, eventFactory, name, env.getProperty(entry));
						newValueGiven = true;
						break;
					}
				}
				if (!newValueGiven) {
					writer.add(event);
				}
			}
			// add production meta data if it isn't there yet
			else if (event.isEndElement() && event.asEndElement().getName().equals(head)) {
				for (String key : productionKeys) {
					if (!seen.contains(key)) {
						writer.add(eventFactory.createStartElement("", dtb, "meta"));
						writer.add(eventFactory.createAttribute("name", key));
						writer.add(eventFactory.createAttribute("content", env.getProperty(key, "")));
						writer.add(eventFactory.createEndElement("", dtb, "meta"));
					}
				}
				writer.add(event);
			}
			// otherwise just copy the element
			else {
				writer.add(event);
			}
		}

		writer.flush();

	}

	/**
	 * A tiny xml transformer that will update the meta data in your DTBook xml and
	 * only that. No entity expansion, no anything! You get your original xml back
	 * with the updated meta data.
	 *
	 * Pass in the xml to be modified on stdin and get the updated xml from stdout.
	 *
	 * @param args is ignored
	 * @throws XMLStreamException
	 */
	public static void main(String[] args) throws XMLStreamException {
		transform(System.in, System.out, System.getProperties());
	}

}
