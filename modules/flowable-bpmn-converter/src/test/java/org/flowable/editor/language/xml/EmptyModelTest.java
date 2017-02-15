package org.flowable.editor.language.xml;

import static org.junit.Assert.fail;

import org.flowable.bpmn.exceptions.XMLException;
import org.junit.Test;

public class EmptyModelTest extends AbstractConverterTest {

    @Test
    public void connvertXMLToModel() throws Exception {
        try {
            readXMLFile();
            fail("Expected xml exception");
        } catch (XMLException e) {
            // exception expected
        }
    }

    @Test
    public void convertModelToXML() throws Exception {
        try {
            readXMLFile();
            fail("Expected xml exception");
        } catch (XMLException e) {
            // exception expected
        }
    }

    protected String getResource() {
        return "empty.bpmn";
    }
}
