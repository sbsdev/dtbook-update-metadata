package ch.sbs;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;

import org.junit.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;
import org.xmlunit.xpath.XPathEngine;

public class TransformerTest {

	private static final String DTBOOK_START = "<!DOCTYPE";
	private static final String DTBOOK_END = "]>";

	private static final String DTBOOK_NAMESPACE_PREFIX = "dtb";
	private static final String DTBOOK_NAMESPACE_URI = "http://www.daisy.org/z3986/2005/dtbook/";

	private static final String RESOURCES_XML_TEST_BEFORE_XML = "src/test/resources/xml/before.xml";
	private static final String RESOURCES_XML_TEST_AFTER_XML = "src/test/resources/xml/after.xml";

	private static final Map<String, String> prefix2Uri = createNamespacePrefixURI();

	private static Map<String, String> createNamespacePrefixURI() {
		Map<String, String> result = new HashMap<String, String>();
		result.put(DTBOOK_NAMESPACE_PREFIX, DTBOOK_NAMESPACE_URI);
		return result;
	}

	@Test
	public void testDoctype() throws XMLStreamException, IOException {
		String originalContent = new String(Files.readAllBytes(Paths.get(RESOURCES_XML_TEST_BEFORE_XML)),
				StandardCharsets.UTF_8);
		// grab only the DOCTYPE
		String originalDoctype = originalContent.substring(originalContent.indexOf(DTBOOK_START),
				originalContent.indexOf(DTBOOK_END));
		String updatedContent = createTestDocument(new FileInputStream(new File(RESOURCES_XML_TEST_AFTER_XML)))
				.toString();
		String updatedDoctype = updatedContent.substring(updatedContent.indexOf(DTBOOK_START),
				updatedContent.indexOf(DTBOOK_END));
		assertEquals(originalDoctype, updatedDoctype);
	}

	private ByteArrayOutputStream createTestDocument(InputStream in) throws XMLStreamException, FileNotFoundException {
		Properties props = new Properties();
		props.setProperty("dtb:sourcePublisher", "a very new Publisher");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		MetaDataTransformer.transform(in, out, props);
		return out;
	}

	@Test
	public void testXML() throws FileNotFoundException, XMLStreamException {
		assertThat(Input.fromStream(new FileInputStream(new File(RESOURCES_XML_TEST_AFTER_XML))),
				isIdenticalTo(Input.fromStream(new ByteArrayInputStream(
						createTestDocument(new FileInputStream(new File(RESOURCES_XML_TEST_BEFORE_XML)))
								.toByteArray()))));
	}

	@Test
	public void testEntities() throws FileNotFoundException, XMLStreamException {
		assertThat(createTestDocument(new FileInputStream(new File(RESOURCES_XML_TEST_AFTER_XML))).toString(),
				containsString("&charref"));
	}

	@Test
	public void testMetadata() throws XMLStreamException, IOException {
		Source source = Input
				.fromStream(new ByteArrayInputStream(
						createTestDocument(new FileInputStream(new File(RESOURCES_XML_TEST_BEFORE_XML))).toByteArray()))
				.build();
		XPathEngine xpath = new JAXPXPathEngine();
		xpath.setNamespaceContext(prefix2Uri);
		String content = xpath.evaluate("/dtb:dtbook/dtb:head/dtb:meta[@name='dtb:sourcePublisher']/@content", source);
		assert "a very new Publisher".equals(content);
	}
}