#!/bin/sh

# A tiny xml transformer that will update the meta data in your DTBook
# file and only that. No entity expansion, no anything! You get your
# original xml file back with the updated meta data.

JAR=target/update-dtbook-metadata-0.1-jar-with-dependencies.jar
XML=src/test/resources/xml/test.xml

java -DDC.TITLE="a very fancy new title" -DDC.SUBJECT="a new subject" -DPROD.SERIESNUMBER=5 -jar $JAR $XML
