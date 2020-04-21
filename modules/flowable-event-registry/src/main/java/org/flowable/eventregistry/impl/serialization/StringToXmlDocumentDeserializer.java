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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.w3c.dom.Document;

/**
 * @author Joram Barrez
 */
public class StringToXmlDocumentDeserializer implements InboundEventDeserializer<Document> {

    @Override
    public Document deserialize(String rawEvent) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            try (InputStream inputStream = new ByteArrayInputStream(rawEvent.getBytes(StandardCharsets.UTF_8))) {
                return documentBuilder.parse(inputStream);
            }
        } catch (Exception e) {
            throw new FlowableException("Could not deserialize event to xml", e);
        }
    }

}
