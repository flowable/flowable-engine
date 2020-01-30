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

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class UserTaskWithEventRegistryBoundaryEventConverterTest extends AbstractConverterTest {

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
        return "usertaskeventregistry.bpmn";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("usertask");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof UserTask);
        assertEquals("usertask", flowElement.getId());
        UserTask userTask = (UserTask) flowElement;
        assertEquals("usertask", userTask.getId());
        assertEquals("kermit", userTask.getAssignee());
        
        flowElement = model.getMainProcess().getFlowElement("eventRegistryEvent");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof BoundaryEvent);
        BoundaryEvent boundaryEvent = (BoundaryEvent) flowElement;
        assertEquals("eventRegistryEvent", boundaryEvent.getId());
        assertEquals("usertask", boundaryEvent.getAttachedToRefId());
        assertEquals(2, boundaryEvent.getExtensionElements().size());
        ExtensionElement extensionElement = boundaryEvent.getExtensionElements().get("eventType").get(0);
        assertEquals("myEvent", extensionElement.getElementText());
        extensionElement = boundaryEvent.getExtensionElements().get("eventCorrelationParameter").get(0);
        assertEquals("customerId", extensionElement.getAttributeValue(null, "name"));
        assertEquals("${customerIdVar}", extensionElement.getAttributeValue(null, "value"));
    }
}
