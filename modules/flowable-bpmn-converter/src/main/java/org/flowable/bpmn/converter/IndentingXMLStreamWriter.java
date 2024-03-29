/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.bpmn.converter;

import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author Tijs Rademakers
 */
public class IndentingXMLStreamWriter extends DelegatingXMLStreamWriter {

    private static final Object SEEN_NOTHING = new Object();
    private static final Object SEEN_ELEMENT = new Object();
    private static final Object SEEN_DATA = new Object();

    private Object state = SEEN_NOTHING;
    private Stack<Object> stateStack = new Stack<>();

    private String indentStep = "  ";
    private int depth;

    public IndentingXMLStreamWriter(XMLStreamWriter writer) {
        super(writer);
    }

    public void setIndentStep(String s) {
        this.indentStep = s;
    }

    private void onStartElement() throws XMLStreamException {
        stateStack.push(SEEN_ELEMENT);
        state = SEEN_NOTHING;
        if (depth > 0) {
            super.writeCharacters("\n");
        }
        doIndent();
        depth++;
    }

    private void onEndElement() throws XMLStreamException {
        depth--;
        if (state == SEEN_ELEMENT) {
            super.writeCharacters("\n");
            doIndent();
        }
        state = stateStack.pop();
    }

    private void onEmptyElement() throws XMLStreamException {
        state = SEEN_ELEMENT;
        if (depth > 0) {
            super.writeCharacters("\n");
        }
        doIndent();
    }

    /**
     * Print indentation for the current level.
     * 
     * @exception org.xml.sax.SAXException
     *                If there is an error writing the indentation characters, or if a filter further down the chain raises an exception.
     */
    private void doIndent() throws XMLStreamException {
        if (depth > 0) {
            for (int i = 0; i < depth; i++) {
                super.writeCharacters(indentStep);
            }
        }
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        super.writeStartDocument();
        super.writeCharacters("\n");
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        super.writeStartDocument(version);
        super.writeCharacters("\n");
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        super.writeStartDocument(encoding, version);
        super.writeCharacters("\n");
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        onStartElement();
        super.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        onStartElement();
        super.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        onStartElement();
        super.writeStartElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        onEmptyElement();
        super.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        onEmptyElement();
        super.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        onEmptyElement();
        super.writeEmptyElement(localName);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        onEndElement();
        super.writeEndElement();
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        state = SEEN_DATA;
        super.writeCharacters(text);
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        state = SEEN_DATA;
        super.writeCharacters(text, start, len);
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        state = SEEN_DATA;
        super.writeCData(data);
    }

}
