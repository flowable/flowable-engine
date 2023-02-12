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
package org.flowable.dmn.xml;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnExtensionAttribute;
import org.junit.jupiter.api.Test;

public class DmnDefinitionExtensionAttributesConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        DmnDefinition definition = readXMLFile();
        validateModel(definition);
    }

    @Test
    public void convertModelToXML() throws Exception {
        DmnDefinition bpmnModel = readXMLFile();
        DmnDefinition parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "customExtensionAttributes.dmn";
    }

    private void validateModel(DmnDefinition model) {

        Map<String, Map<String, String>> namespaceToAttributeValue = new HashMap<>();

        Collection<List<DmnExtensionAttribute>> values = model.getAttributes().values();
        for (List<DmnExtensionAttribute> attributes : model.getAttributes().values()) {
            for (DmnExtensionAttribute attribute : attributes) {
                namespaceToAttributeValue.computeIfAbsent(attribute.getNamespace(), key -> new HashMap<>()).put(attribute.getName(), attribute.getValue());
            }
        }

        assertThat(namespaceToAttributeValue.get("http://flowable.org/dmn")).hasSize(1);
        assertThat(namespaceToAttributeValue.get("http://flowable.org/dmn").get("custom")).isEqualTo("Hello");

        assertThat(namespaceToAttributeValue.get("http://flowable.org/test")).hasSize(2);
        assertThat(namespaceToAttributeValue.get("http://flowable.org/test").get("custom")).isEqualTo("Testing");
        assertThat(namespaceToAttributeValue.get("http://flowable.org/test").get("customTwo")).isEqualTo("123");

    }
}
