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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.junit.Test;

public class ServiceTaskTransientVariableTest extends AbstractConverterTest {

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
        return "servicetaskstoreresulttransient.bpmn";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement1 = model.getMainProcess().getFlowElement("servicetask1");
        FlowElement flowElement2 = model.getMainProcess().getFlowElement("servicetask2");
        FlowElement flowElement3 = model.getMainProcess().getFlowElement("servicetask3");

        assertNotNull(flowElement1);
        assertNotNull(flowElement2);
        assertNotNull(flowElement3);

        assertTrue(flowElement1 instanceof ServiceTask);
        assertTrue(flowElement2 instanceof ServiceTask);
        assertTrue(flowElement3 instanceof ServiceTask);

        assertEquals("servicetask1", flowElement1.getId());
        assertEquals("servicetask2", flowElement2.getId());
        assertEquals("servicetask3", flowElement3.getId());

        ServiceTask serviceTask1 = (ServiceTask) flowElement1;
        ServiceTask serviceTask2 = (ServiceTask) flowElement2;
        ServiceTask serviceTask3 = (ServiceTask) flowElement3;

        assertTrue(serviceTask1.isStoreResultVariableAsTransient());
        assertFalse(serviceTask2.isStoreResultVariableAsTransient());
        assertFalse(serviceTask3.isStoreResultVariableAsTransient());
    }
}
