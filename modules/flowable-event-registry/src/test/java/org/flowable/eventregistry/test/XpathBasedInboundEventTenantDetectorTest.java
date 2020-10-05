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
package org.flowable.eventregistry.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.impl.serialization.StringToXmlDocumentDeserializer;
import org.flowable.eventregistry.impl.tenantdetector.XpathBasedInboundEventTenantDetector;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class XpathBasedInboundEventTenantDetectorTest {

    private StringToXmlDocumentDeserializer deserializer = new StringToXmlDocumentDeserializer();

    @Test
    void testDetectTenantId() {
        Document xmlDocument = deserializer.deserialize("<data><name>Doe</name><tenantId>01</tenantId></data>");

        String xPath = "/data/tenantId";
        XpathBasedInboundEventTenantDetector detector = new XpathBasedInboundEventTenantDetector(xPath);
        String tenantId = detector.detectTenantId(xmlDocument);
        assertThat(tenantId).isEqualTo("01");
    }

    @Test
    void testDetectTenantIdWrongXpath() {
        Document xmlDocument = deserializer.deserialize("<data><name>Doe</name><tenantId>01</tenantId></data>");

        String xPath = "/data/wrongTenantId";
        XpathBasedInboundEventTenantDetector detector = new XpathBasedInboundEventTenantDetector(xPath);
        assertThatThrownBy(() -> detector.detectTenantId(xmlDocument)).isInstanceOf(FlowableException.class);
    }

    @Test
    void testDetectTenantIdMissingTenantIdInXml() {
        Document xmlDocument = deserializer.deserialize("<data><name>Doe</name></data>");

        String xPath = "/data/tenantId";
        XpathBasedInboundEventTenantDetector detector = new XpathBasedInboundEventTenantDetector(xPath);
        assertThatThrownBy(() -> detector.detectTenantId(xmlDocument)).isInstanceOf(FlowableException.class);
    }

}