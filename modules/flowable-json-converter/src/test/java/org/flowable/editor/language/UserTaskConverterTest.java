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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class UserTaskConverterTest extends AbstractConverterTest {

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
        return "test.usertaskmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("usertask", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof UserTask);
        assertEquals("usertask", flowElement.getId());
        UserTask userTask = (UserTask) flowElement;
        assertEquals("usertask", userTask.getId());
        assertEquals("User task", userTask.getName());
        assertEquals("testKey", userTask.getFormKey());
        assertEquals("40", userTask.getPriority());
        assertEquals("2012-11-01", userTask.getDueDate());
        assertEquals("defaultCategory", userTask.getCategory());
        assertEquals("${skipExpression}", userTask.getSkipExpression());

        assertEquals("kermit", userTask.getAssignee());
        assertEquals(2, userTask.getCandidateUsers().size());
        assertTrue(userTask.getCandidateUsers().contains("kermit"));
        assertTrue(userTask.getCandidateUsers().contains("fozzie"));
        assertEquals(2, userTask.getCandidateGroups().size());
        assertTrue(userTask.getCandidateGroups().contains("management"));
        assertTrue(userTask.getCandidateGroups().contains("sales"));

        List<FormProperty> formProperties = userTask.getFormProperties();
        assertEquals(2, formProperties.size());
        FormProperty formProperty = formProperties.get(0);
        assertEquals("formId", formProperty.getId());
        assertEquals("formName", formProperty.getName());
        assertEquals("string", formProperty.getType());
        assertEquals("variable", formProperty.getVariable());
        assertEquals("${expression}", formProperty.getExpression());
        formProperty = formProperties.get(1);
        assertEquals("formId2", formProperty.getId());
        assertEquals("anotherName", formProperty.getName());
        assertEquals("long", formProperty.getType());
        assertTrue(StringUtils.isEmpty(formProperty.getVariable()));
        assertTrue(StringUtils.isEmpty(formProperty.getExpression()));

        List<FlowableListener> listeners = userTask.getTaskListeners();
        assertEquals(3, listeners.size());
        FlowableListener listener = listeners.get(0);
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_CLASS, listener.getImplementationType());
        assertEquals("org.test.TestClass", listener.getImplementation());
        assertEquals("create", listener.getEvent());
        assertEquals(2, listener.getFieldExtensions().size());
        assertEquals("testField", listener.getFieldExtensions().get(0).getFieldName());
        assertEquals("test", listener.getFieldExtensions().get(0).getStringValue());
        listener = listeners.get(1);
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION, listener.getImplementationType());
        assertEquals("${someExpression}", listener.getImplementation());
        assertEquals("assignment", listener.getEvent());
        listener = listeners.get(2);
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, listener.getImplementationType());
        assertEquals("${someDelegateExpression}", listener.getImplementation());
        assertEquals("complete", listener.getEvent());

        flowElement = model.getMainProcess().getFlowElement("start", true);
        assertTrue(flowElement instanceof StartEvent);

        StartEvent startEvent = (StartEvent) flowElement;
        assertEquals(1, startEvent.getOutgoingFlows().size());

        flowElement = model.getMainProcess().getFlowElement("flow1", true);
        assertTrue(flowElement instanceof SequenceFlow);

        SequenceFlow flow = (SequenceFlow) flowElement;
        assertEquals("flow1", flow.getId());
        assertNotNull(flow.getSourceRef());
        assertNotNull(flow.getTargetRef());
    }
}
