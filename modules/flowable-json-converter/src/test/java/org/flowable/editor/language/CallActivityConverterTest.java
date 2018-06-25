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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CallActivityConverterTest extends AbstractConverterTest {

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
        return "test.callactivitymodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("callactivity", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof CallActivity);
        CallActivity callActivity = (CallActivity) flowElement;
        assertEquals("callactivity", callActivity.getId());
        assertEquals("Call activity", callActivity.getName());

        assertEquals("processId", callActivity.getCalledElement());
        assertEquals("id", callActivity.getCalledElementType());
        assertTrue(callActivity.isInheritVariables());
        assertTrue(callActivity.isSameDeployment());
        assertTrue(callActivity.isInheritBusinessKey());
        assertTrue(callActivity.isUseLocalScopeForOutParameters());

        List<IOParameter> parameters = callActivity.getInParameters();
        assertEquals(2, parameters.size());
        IOParameter parameter = parameters.get(0);
        assertEquals("test", parameter.getSource());
        assertEquals("test", parameter.getTarget());
        parameter = parameters.get(1);
        assertEquals("${test}", parameter.getSourceExpression());
        assertEquals("test", parameter.getTarget());

        parameters = callActivity.getOutParameters();
        assertEquals(1, parameters.size());
        parameter = parameters.get(0);
        assertEquals("test", parameter.getSource());
        assertEquals("test", parameter.getTarget());
    }
}
