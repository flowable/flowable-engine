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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.junit.Test;

public class DecisionTaskConverterTest extends AbstractConverterTest {

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
        return "test.decisiontaskmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("decisiontask", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof ServiceTask);
        ServiceTask serviceTask = (ServiceTask) flowElement;
        assertEquals("decisiontask", serviceTask.getId());
        assertEquals("decision task", serviceTask.getName());

        assertThatFieldExtension(serviceTask, "fallbackToDefaultTenant", "true");
        assertThatFieldExtension(serviceTask, "decisionTaskThrowErrorOnNoHits", "true");

    }

    protected void assertThatFieldExtension(ServiceTask serviceTask, String fieldName, Object fieldValue) {
        assertEquals(serviceTask.getFieldExtensions().stream().
                filter(field -> field.getFieldName().equals(fieldName)).
                findFirst().
                orElseThrow(AssertionError::new).
                getStringValue(), fieldValue);
    }
}
