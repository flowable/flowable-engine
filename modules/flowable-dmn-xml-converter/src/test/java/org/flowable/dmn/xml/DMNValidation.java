package org.flowable.dmn.xml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class DMNValidation {

    protected static final String DMN11_resource = "collectionsRegression.dmn";
    protected static final String DMN11_invalid_resource = "collectionsRegression_invalid.dmn";
    protected static final String DMN12_resource = "chapter11multiDiagram.dmn";
    protected static final String DMN12_invalid_resource = "chapter11multiDiagram_invalid.dmn";
    protected static final String DMN13_resource = "dmn13_chapter11multiDiagram.dmn";
    protected static final String DMN13_invalid_resource = "dmn13_chapter11multiDiagram_invalid.dmn";

    @Test
    public void validateDMN11() throws Exception {
        validateResource(DMN11_resource);
    }

    @Test
    public void validateDMN11invalid() {
        Assertions.assertThrows(SAXException.class, () -> validateResource(DMN11_invalid_resource));
    }

    @Test
    public void validateDMN12() throws Exception {
        validateResource(DMN12_resource);
    }

    @Test
    public void validateDMN12invalid() {
        Assertions.assertThrows(SAXException.class, () -> validateResource(DMN12_invalid_resource));
    }

    @Test
    public void validateDMN13() throws Exception {
        validateResource(DMN13_resource);
    }

    @Test
    public void validateDMN13invalid() {
        Assertions.assertThrows(SAXException.class, () -> validateResource(DMN13_invalid_resource));
    }

    protected void validateResource(String resource) throws Exception {
        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream(resource);
        XMLInputFactory xif = XMLInputFactory.newInstance();
        InputStreamReader in = new InputStreamReader(xmlStream, StandardCharsets.UTF_8);
        XMLStreamReader xtr = xif.createXMLStreamReader(in);
        new DmnXMLConverter().validateModel(xtr);
    }
}
