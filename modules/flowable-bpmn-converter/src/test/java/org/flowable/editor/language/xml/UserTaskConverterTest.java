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

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class UserTaskConverterTest extends AbstractConverterTest {

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
        return "usertaskmodel.bpmn";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("usertask");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof UserTask);
        assertEquals("usertask", flowElement.getId());
        UserTask userTask = (UserTask) flowElement;
        assertEquals("usertask", userTask.getId());
        assertEquals("User task", userTask.getName());
        assertEquals("Test Category", userTask.getCategory());
        assertEquals("testKey", userTask.getFormKey());
        assertEquals("40", userTask.getPriority());
        assertEquals("2012-11-01", userTask.getDueDate());

        assertEquals("customCalendarName", userTask.getBusinessCalendarName());

        assertEquals("kermit", userTask.getAssignee());
        assertEquals(2, userTask.getCandidateUsers().size());
        assertTrue(userTask.getCandidateUsers().contains("kermit"));
        assertTrue(userTask.getCandidateUsers().contains("fozzie"));
        assertEquals(2, userTask.getCandidateGroups().size());
        assertTrue(userTask.getCandidateGroups().contains("management"));
        assertTrue(userTask.getCandidateGroups().contains("sales"));

        assertEquals(1, userTask.getCustomUserIdentityLinks().size());
        assertEquals(2, userTask.getCustomGroupIdentityLinks().size());
        assertTrue(userTask.getCustomUserIdentityLinks().get("businessAdministrator").contains("kermit"));
        assertTrue(userTask.getCustomGroupIdentityLinks().get("manager").contains("management"));
        assertTrue(userTask.getCustomGroupIdentityLinks().get("businessAdministrator").contains("management"));

        List<FormProperty> formProperties = userTask.getFormProperties();
        assertEquals(3, formProperties.size());
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
        formProperty = formProperties.get(2);
        assertEquals("formId3", formProperty.getId());
        assertEquals("enumName", formProperty.getName());
        assertEquals("enum", formProperty.getType());
        assertTrue(StringUtils.isEmpty(formProperty.getVariable()));
        assertTrue(StringUtils.isEmpty(formProperty.getExpression()));
        assertEquals(2, formProperty.getFormValues().size());

        List<FlowableListener> listeners = userTask.getTaskListeners();
        assertEquals(3, listeners.size());
        FlowableListener listener = listeners.get(0);
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_CLASS, listener.getImplementationType());
        assertEquals("org.test.TestClass", listener.getImplementation());
        assertEquals("create", listener.getEvent());
        assertEquals("before-commit", listener.getOnTransaction());
        assertEquals("org.test.TestResolverClass", listener.getCustomPropertiesResolverImplementation());
        listener = listeners.get(1);
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION, listener.getImplementationType());
        assertEquals("${someExpression}", listener.getImplementation());
        assertEquals("assignment", listener.getEvent());
        assertEquals("committed", listener.getOnTransaction());
        assertEquals("${testResolverExpression}", listener.getCustomPropertiesResolverImplementation());
        listener = listeners.get(2);
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, listener.getImplementationType());
        assertEquals("${someDelegateExpression}", listener.getImplementation());
        assertEquals("complete", listener.getEvent());
        assertEquals("rolled-back", listener.getOnTransaction());
        assertEquals("${delegateResolverExpression}", listener.getCustomPropertiesResolverImplementation());

        List<FlowableListener> executionListeners = userTask.getExecutionListeners();
        assertEquals(1, executionListeners.size());
        FlowableListener executionListener = executionListeners.get(0);
        assertEquals("end", executionListener.getEvent());
        assertEquals("before-commit", executionListener.getOnTransaction());
        assertEquals("org.test.TestResolverClass", executionListener.getCustomPropertiesResolverImplementation());

    }
}
