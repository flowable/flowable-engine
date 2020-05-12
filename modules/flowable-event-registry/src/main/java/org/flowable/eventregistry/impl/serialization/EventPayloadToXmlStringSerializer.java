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
package org.flowable.eventregistry.impl.serialization;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.OutboundEventSerializer;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simple {@link EventInstance} serialization that maps all {@link EventPayloadInstance}'s
 * to an XML Document which gets transformed to a String.
 *
 * @author Joram Barrez
 */
public class EventPayloadToXmlStringSerializer implements OutboundEventSerializer {

    @Override
    public String serialize(EventInstance eventInstance) {
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement(eventInstance.getEventKey());
            doc.appendChild(rootElement);

            if (!eventInstance.getPayloadInstances().isEmpty()) {
                for (EventPayloadInstance payloadInstance : eventInstance.getPayloadInstances()) {
                    Element element = doc.createElement(payloadInstance.getDefinitionName());
                    element.setTextContent(payloadInstance.getValue().toString());
                    rootElement.appendChild(element);
                }
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();

        } catch (Exception e) {
            throw new FlowableException("Could not serialize eventInstance to xml string", e);
        }
    }

}
