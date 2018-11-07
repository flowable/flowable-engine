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
package org.flowable.editor.language.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class CustomNamespaceAttributeConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "customnamespaceattributemodel.bpmn";
    }

    private void validateModel(BpmnModel model) {
        Process process = model.getMainProcess();
        assertNotNull(process.getAttributes());
        assertEquals(1, process.getAttributes().size());
        List<ExtensionAttribute> attributes = process.getAttributes().get("version");
        assertNotNull(attributes);
        assertEquals(1, attributes.size());
        ExtensionAttribute attribute = attributes.get(0);
        // custom:version = "9"
        assertNotNull(attribute);
        assertEquals("http://custom.org/bpmn", attribute.getNamespace());
        assertEquals("custom", attribute.getNamespacePrefix());
        assertEquals("version", attribute.getName());
        assertEquals("9", attribute.getValue());

        FlowElement flowElement = model.getMainProcess().getFlowElement("usertask");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof UserTask);
        assertEquals("usertask", flowElement.getId());
        UserTask userTask = (UserTask) flowElement;
        assertEquals("usertask", userTask.getId());
        assertEquals("User Task", userTask.getName());

        Map<String, List<ExtensionAttribute>> attributesMap = userTask.getAttributes();
        assertNotNull(attributesMap);
        assertEquals(2, attributesMap.size());

        attributes = attributesMap.get("id");
        assertNotNull(attributes);
        assertEquals(1, attributes.size());
        ExtensionAttribute a = attributes.get(0);
        assertNotNull(a);
        assertEquals("id", a.getName());
        assertEquals("test", a.getValue());
        assertEquals("custom2", a.getNamespacePrefix());
        assertEquals("http://custom2.org/bpmn", a.getNamespace());

        attributes = attributesMap.get("attr");
        assertNotNull(attributes);
        assertEquals(1, attributes.size());
        a = attributes.get(0);
        assertNotNull(a);
        assertEquals("attr", a.getName());
        assertEquals("attrValue", a.getValue());
        assertEquals("custom2", a.getNamespacePrefix());
        assertEquals("http://custom2.org/bpmn", a.getNamespace());
    }
}
