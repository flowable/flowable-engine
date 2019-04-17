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
package org.flowable.editor.language;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.junit.Test;

import java.util.List;

/**
 * @author Zheng Ji
 */
public class ErrorConverterTest extends AbstractConverterTest {

    @Test
    public void convertJsonToModel() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
    }

    @Test
    public void doubleConversionValidation() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel);
    }

    @Override
    protected String getResource() {
        return "test.errormodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("sid-CF7571C4-F093-45C5-ADD6-8F357ACF584C");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof StartEvent);

        StartEvent startEvent= (StartEvent) flowElement;
        List<EventDefinition> eventDefinitions = startEvent.getEventDefinitions();
        assertEquals(1,eventDefinitions.size());

        EventDefinition eventDefinition = eventDefinitions.get(0);
        assertTrue(eventDefinition instanceof ErrorEventDefinition);

        ErrorEventDefinition errorEventDefinition= (ErrorEventDefinition) eventDefinition;
        String errorCode = errorEventDefinition.getErrorCode();
        assertEquals("myerror1",errorCode);


    }
}
