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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import java.util.List;

/**
 * @author Zheng Ji
 */
public class ErrorsConverterTest extends AbstractConverterTest {

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
        return "errors.bpmn20.xml";
    }

    private void validateModel(BpmnModel model) {
        assertEquals(1, model.getProcesses().size());
        assertEquals(2, model.getErrors().size());

        FlowElement flowElement = model.getMainProcess().getFlowElement("sid-DC9C1AEF-C999-40B9-BAC5-6532CC6D7F89");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof StartEvent);

        StartEvent startEvent = (StartEvent) flowElement;
        List<EventDefinition> eventDefinitions = startEvent.getEventDefinitions();
        assertEquals(1, eventDefinitions.size());

        EventDefinition eventDefinition = eventDefinitions.get(0);
        assertTrue(eventDefinition instanceof ErrorEventDefinition);

        ErrorEventDefinition errorEventDefinition = (ErrorEventDefinition) eventDefinition;
        String errorCode = errorEventDefinition.getErrorCode();
        assertEquals("shareniu-b", errorCode);
    }
}
