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
package org.flowable.bpm.model.bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.BUSINESS_RULE_TASK;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.CALL_ACTIVITY_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.END_EVENT_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.PROCESS_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.SCRIPT_TASK_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.SEND_TASK_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.SEQUENCE_FLOW_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.SERVICE_TASK_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.START_EVENT_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_CLASS_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_CLASS_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_DELEGATE_EXPRESSION_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_DELEGATE_EXPRESSION_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_DUE_DATE_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_DUE_DATE_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_EXECUTION_EVENT_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_EXECUTION_EVENT_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_EXPRESSION_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_EXPRESSION_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_GROUPS_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_GROUPS_LIST_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_GROUPS_LIST_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_GROUPS_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_PRIORITY_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_PRIORITY_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_STRING_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_STRING_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_TASK_EVENT_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_TASK_EVENT_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_TYPE_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_TYPE_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_USERS_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_USERS_LIST_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_USERS_LIST_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_USERS_XML;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.USER_TASK_ID;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.ACTIVITI_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.flowable.bpm.model.bpmn.instance.BusinessRuleTask;
import org.flowable.bpm.model.bpmn.instance.CallActivity;
import org.flowable.bpm.model.bpmn.instance.EndEvent;
import org.flowable.bpm.model.bpmn.instance.Expression;
import org.flowable.bpm.model.bpmn.instance.MessageEventDefinition;
import org.flowable.bpm.model.bpmn.instance.ParallelGateway;
import org.flowable.bpm.model.bpmn.instance.Process;
import org.flowable.bpm.model.bpmn.instance.ScriptTask;
import org.flowable.bpm.model.bpmn.instance.SendTask;
import org.flowable.bpm.model.bpmn.instance.SequenceFlow;
import org.flowable.bpm.model.bpmn.instance.ServiceTask;
import org.flowable.bpm.model.bpmn.instance.StartEvent;
import org.flowable.bpm.model.bpmn.instance.UserTask;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableConnector;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableConnectorId;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableConstraint;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableEntry;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableExecutionListener;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFailedJobRetryTimeCycle;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableField;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormData;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormField;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormProperty;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableIn;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableInputOutput;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableInputParameter;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableList;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableMap;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableOut;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableOutputParameter;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowablePotentialStarter;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableProperties;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableProperty;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableScript;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableTaskListener;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@RunWith(Parameterized.class)
public class FlowableExtensionsTest {

    private Process process;
    private StartEvent startEvent;
    private SequenceFlow sequenceFlow;
    private UserTask userTask;
    private ServiceTask serviceTask;
    private SendTask sendTask;
    private ScriptTask scriptTask;
    private CallActivity callActivity;
    private BusinessRuleTask businessRuleTask;
    private EndEvent endEvent;
    private MessageEventDefinition messageEventDefinition;
    private ParallelGateway parallelGateway;
    private String namespace;
    private BpmnModelInstance originalModelInstance;
    private BpmnModelInstance modelInstance;

    @Parameters(name = "Namespace: {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
                {FLOWABLE_NS, BpmnModelBuilder.readModelFromStream(FlowableExtensionsTest.class.getResourceAsStream("FlowableExtensionsTest.xml"))},
                // for compatibility reasons we check the old namespace, too
                {ACTIVITI_NS, BpmnModelBuilder.readModelFromStream(FlowableExtensionsTest.class.getResourceAsStream("FlowableExtensionsCompatibilityTest.xml"))}
        });
    }

    public FlowableExtensionsTest(String namespace, BpmnModelInstance modelInstance) {
        this.namespace = namespace;
        this.originalModelInstance = modelInstance;
    }

    @Before
    public void setUp() {
        modelInstance = originalModelInstance.clone();
        process = modelInstance.getModelElementById(PROCESS_ID);
        startEvent = modelInstance.getModelElementById(START_EVENT_ID);
        sequenceFlow = modelInstance.getModelElementById(SEQUENCE_FLOW_ID);
        userTask = modelInstance.getModelElementById(USER_TASK_ID);
        serviceTask = modelInstance.getModelElementById(SERVICE_TASK_ID);
        sendTask = modelInstance.getModelElementById(SEND_TASK_ID);
        scriptTask = modelInstance.getModelElementById(SCRIPT_TASK_ID);
        callActivity = modelInstance.getModelElementById(CALL_ACTIVITY_ID);
        businessRuleTask = modelInstance.getModelElementById(BUSINESS_RULE_TASK);
        endEvent = modelInstance.getModelElementById(END_EVENT_ID);
        messageEventDefinition = (MessageEventDefinition) endEvent.getEventDefinitions().iterator().next();
        parallelGateway = modelInstance.getModelElementById("parallelGateway");
    }

    @Test
    public void assignee() {
        assertThat(userTask.getFlowableAssignee()).isEqualTo(TEST_STRING_XML);
        userTask.setFlowableAssignee(TEST_STRING_API);
        assertThat(userTask.getFlowableAssignee()).isEqualTo(TEST_STRING_API);
    }

    @Test
    public void async() {
        assertThat(startEvent.isFlowableAsync()).isTrue();
        assertThat(endEvent.isFlowableAsync()).isTrue();
        assertThat(userTask.isFlowableAsync()).isTrue();
        assertThat(parallelGateway.isFlowableAsync()).isTrue();

        startEvent.setFlowableAsync(false);
        endEvent.setFlowableAsync(false);
        userTask.setFlowableAsync(false);
        parallelGateway.setFlowableAsync(false);

        assertThat(startEvent.isFlowableAsync()).isFalse();
        assertThat(endEvent.isFlowableAsync()).isFalse();
        assertThat(userTask.isFlowableAsync()).isFalse();
        assertThat(parallelGateway.isFlowableAsync()).isFalse();
    }

    @Test
    public void candidateGroups() {
        assertThat(userTask.getFlowableCandidateGroups()).isEqualTo(TEST_GROUPS_XML);
        assertThat(userTask.getFlowableCandidateGroupsList()).containsAll(TEST_GROUPS_LIST_XML);
        userTask.setFlowableCandidateGroups(TEST_GROUPS_API);
        assertThat(userTask.getFlowableCandidateGroups()).isEqualTo(TEST_GROUPS_API);
        assertThat(userTask.getFlowableCandidateGroupsList()).containsAll(TEST_GROUPS_LIST_API);
        userTask.setFlowableCandidateGroupsList(TEST_GROUPS_LIST_XML);
        assertThat(userTask.getFlowableCandidateGroups()).isEqualTo(TEST_GROUPS_XML);
        assertThat(userTask.getFlowableCandidateGroupsList()).containsAll(TEST_GROUPS_LIST_XML);
    }

    @Test
    public void candidateStarterGroups() {
        assertThat(process.getFlowableCandidateStarterGroups()).isEqualTo(TEST_GROUPS_XML);
        assertThat(process.getFlowableCandidateStarterGroupsList()).containsAll(TEST_GROUPS_LIST_XML);
        process.setFlowableCandidateStarterGroups(TEST_GROUPS_API);
        assertThat(process.getFlowableCandidateStarterGroups()).isEqualTo(TEST_GROUPS_API);
        assertThat(process.getFlowableCandidateStarterGroupsList()).containsAll(TEST_GROUPS_LIST_API);
        process.setFlowableCandidateStarterGroupsList(TEST_GROUPS_LIST_XML);
        assertThat(process.getFlowableCandidateStarterGroups()).isEqualTo(TEST_GROUPS_XML);
        assertThat(process.getFlowableCandidateStarterGroupsList()).containsAll(TEST_GROUPS_LIST_XML);
    }

    @Test
    public void candidateStarterUsers() {
        assertThat(process.getFlowableCandidateStarterUsers()).isEqualTo(TEST_USERS_XML);
        assertThat(process.getFlowableCandidateStarterUsersList()).containsAll(TEST_USERS_LIST_XML);
        process.setFlowableCandidateStarterUsers(TEST_USERS_API);
        assertThat(process.getFlowableCandidateStarterUsers()).isEqualTo(TEST_USERS_API);
        assertThat(process.getFlowableCandidateStarterUsersList()).containsAll(TEST_USERS_LIST_API);
        process.setFlowableCandidateStarterUsersList(TEST_USERS_LIST_XML);
        assertThat(process.getFlowableCandidateStarterUsers()).isEqualTo(TEST_USERS_XML);
        assertThat(process.getFlowableCandidateStarterUsersList()).containsAll(TEST_USERS_LIST_XML);
    }

    @Test
    public void candidateUsers() {
        assertThat(userTask.getFlowableCandidateUsers()).isEqualTo(TEST_USERS_XML);
        assertThat(userTask.getFlowableCandidateUsersList()).containsAll(TEST_USERS_LIST_XML);
        userTask.setFlowableCandidateUsers(TEST_USERS_API);
        assertThat(userTask.getFlowableCandidateUsers()).isEqualTo(TEST_USERS_API);
        assertThat(userTask.getFlowableCandidateUsersList()).containsAll(TEST_USERS_LIST_API);
        userTask.setFlowableCandidateUsersList(TEST_USERS_LIST_XML);
        assertThat(userTask.getFlowableCandidateUsers()).isEqualTo(TEST_USERS_XML);
        assertThat(userTask.getFlowableCandidateUsersList()).containsAll(TEST_USERS_LIST_XML);
    }

    @Test
    public void doClass() {
        assertThat(serviceTask.getFlowableClass()).isEqualTo(TEST_CLASS_XML);
        assertThat(messageEventDefinition.getFlowableClass()).isEqualTo(TEST_CLASS_XML);

        serviceTask.setFlowableClass(TEST_CLASS_API);
        messageEventDefinition.setFlowableClass(TEST_CLASS_API);

        assertThat(serviceTask.getFlowableClass()).isEqualTo(TEST_CLASS_API);
        assertThat(messageEventDefinition.getFlowableClass()).isEqualTo(TEST_CLASS_API);
    }

    @Test
    public void delegateExpression() {
        assertThat(serviceTask.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_XML);
        assertThat(messageEventDefinition.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_XML);

        serviceTask.setFlowableDelegateExpression(TEST_DELEGATE_EXPRESSION_API);
        messageEventDefinition.setFlowableDelegateExpression(TEST_DELEGATE_EXPRESSION_API);

        assertThat(serviceTask.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
        assertThat(messageEventDefinition.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
    }

    @Test
    public void dueDate() {
        assertThat(userTask.getFlowableDueDate()).isEqualTo(TEST_DUE_DATE_XML);
        userTask.setFlowableDueDate(TEST_DUE_DATE_API);
        assertThat(userTask.getFlowableDueDate()).isEqualTo(TEST_DUE_DATE_API);
    }

    @Test
    public void exclusive() {
        assertThat(startEvent.isFlowableExclusive()).isTrue();
        assertThat(userTask.isFlowableExclusive()).isFalse();
        userTask.setFlowableExclusive(true);
        assertThat(userTask.isFlowableExclusive()).isTrue();
        assertThat(parallelGateway.isFlowableExclusive()).isTrue();
        parallelGateway.setFlowableExclusive(false);
        assertThat(parallelGateway.isFlowableExclusive()).isFalse();

        assertThat(callActivity.isFlowableExclusive()).isFalse();
        callActivity.setFlowableExclusive(true);
        assertThat(callActivity.isFlowableExclusive()).isTrue();
    }

    @Test
    public void expression() {
        assertThat(serviceTask.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_XML);
        assertThat(messageEventDefinition.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_XML);
        serviceTask.setFlowableExpression(TEST_EXPRESSION_API);
        messageEventDefinition.setFlowableExpression(TEST_EXPRESSION_API);
        assertThat(serviceTask.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_API);
        assertThat(messageEventDefinition.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_API);
    }

    @Test
    public void formHandlerClass() {
        assertThat(startEvent.getFlowableFormHandlerClass()).isEqualTo(TEST_CLASS_XML);
        assertThat(userTask.getFlowableFormHandlerClass()).isEqualTo(TEST_CLASS_XML);
        startEvent.setFlowableFormHandlerClass(TEST_CLASS_API);
        userTask.setFlowableFormHandlerClass(TEST_CLASS_API);
        assertThat(startEvent.getFlowableFormHandlerClass()).isEqualTo(TEST_CLASS_API);
        assertThat(userTask.getFlowableFormHandlerClass()).isEqualTo(TEST_CLASS_API);
    }

    @Test
    public void formKey() {
        assertThat(startEvent.getFlowableFormKey()).isEqualTo(TEST_STRING_XML);
        assertThat(userTask.getFlowableFormKey()).isEqualTo(TEST_STRING_XML);
        startEvent.setFlowableFormKey(TEST_STRING_API);
        userTask.setFlowableFormKey(TEST_STRING_API);
        assertThat(startEvent.getFlowableFormKey()).isEqualTo(TEST_STRING_API);
        assertThat(userTask.getFlowableFormKey()).isEqualTo(TEST_STRING_API);
    }

    @Test
    public void initiator() {
        assertThat(startEvent.getFlowableInitiator()).isEqualTo(TEST_STRING_XML);
        startEvent.setFlowableInitiator(TEST_STRING_API);
        assertThat(startEvent.getFlowableInitiator()).isEqualTo(TEST_STRING_API);
    }

    @Test
    public void priority() {
        assertThat(userTask.getFlowablePriority()).isEqualTo(TEST_PRIORITY_XML);
        userTask.setFlowablePriority(TEST_PRIORITY_API);
        assertThat(userTask.getFlowablePriority()).isEqualTo(TEST_PRIORITY_API);
    }

    @Test
    public void resultVariable() {
        assertThat(serviceTask.getFlowableResultVariable()).isEqualTo(TEST_STRING_XML);
        assertThat(messageEventDefinition.getFlowableResultVariable()).isEqualTo(TEST_STRING_XML);
        serviceTask.setFlowableResultVariable(TEST_STRING_API);
        messageEventDefinition.setFlowableResultVariable(TEST_STRING_API);
        assertThat(serviceTask.getFlowableResultVariable()).isEqualTo(TEST_STRING_API);
        assertThat(messageEventDefinition.getFlowableResultVariable()).isEqualTo(TEST_STRING_API);
    }

    @Test
    public void type() {
        assertThat(serviceTask.getFlowableType()).isEqualTo(TEST_TYPE_XML);
        assertThat(messageEventDefinition.getFlowableType()).isEqualTo(TEST_STRING_XML);
        serviceTask.setFlowableType(TEST_TYPE_API);
        messageEventDefinition.setFlowableType(TEST_STRING_API);
        assertThat(serviceTask.getFlowableType()).isEqualTo(TEST_TYPE_API);
        assertThat(messageEventDefinition.getFlowableType()).isEqualTo(TEST_STRING_API);
    }

    @Test
    public void executionListenerExtension() {
        FlowableExecutionListener processListener =
                process.getExtensionElements().getElementsQuery().filterByType(FlowableExecutionListener.class).singleResult();
        FlowableExecutionListener startEventListener =
                startEvent.getExtensionElements().getElementsQuery().filterByType(FlowableExecutionListener.class).singleResult();
        FlowableExecutionListener serviceTaskListener =
                serviceTask.getExtensionElements().getElementsQuery().filterByType(FlowableExecutionListener.class).singleResult();
        assertThat(processListener.getFlowableClass()).isEqualTo(TEST_CLASS_XML);
        assertThat(processListener.getFlowableEvent()).isEqualTo(TEST_EXECUTION_EVENT_XML);
        assertThat(startEventListener.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_XML);
        assertThat(startEventListener.getFlowableEvent()).isEqualTo(TEST_EXECUTION_EVENT_XML);
        assertThat(serviceTaskListener.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_XML);
        assertThat(serviceTaskListener.getFlowableEvent()).isEqualTo(TEST_EXECUTION_EVENT_XML);
        processListener.setFlowableClass(TEST_CLASS_API);
        processListener.setFlowableEvent(TEST_EXECUTION_EVENT_API);
        startEventListener.setFlowableExpression(TEST_EXPRESSION_API);
        startEventListener.setFlowableEvent(TEST_EXECUTION_EVENT_API);
        serviceTaskListener.setFlowableDelegateExpression(TEST_DELEGATE_EXPRESSION_API);
        serviceTaskListener.setFlowableEvent(TEST_EXECUTION_EVENT_API);
        assertThat(processListener.getFlowableClass()).isEqualTo(TEST_CLASS_API);
        assertThat(processListener.getFlowableEvent()).isEqualTo(TEST_EXECUTION_EVENT_API);
        assertThat(startEventListener.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_API);
        assertThat(startEventListener.getFlowableEvent()).isEqualTo(TEST_EXECUTION_EVENT_API);
        assertThat(serviceTaskListener.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
        assertThat(serviceTaskListener.getFlowableEvent()).isEqualTo(TEST_EXECUTION_EVENT_API);
    }

    @Test
    public void flowableScriptExecutionListener() {
        FlowableExecutionListener sequenceFlowListener =
                sequenceFlow.getExtensionElements().getElementsQuery().filterByType(FlowableExecutionListener.class).singleResult();

        FlowableScript script = sequenceFlowListener.getFlowableScript();
        assertThat(script.getFlowableResource()).isNull();
        assertThat(script.getTextContent()).isEqualTo("println 'Hello World'");

        FlowableScript newScript = modelInstance.newInstance(FlowableScript.class);
        newScript.setFlowableResource("test.groovy");
        sequenceFlowListener.setFlowableScript(newScript);

        script = sequenceFlowListener.getFlowableScript();
        assertThat(script.getFlowableResource()).isEqualTo("test.groovy");
        assertThat(script.getTextContent()).isEmpty();
    }

    @Test
    public void failedJobRetryTimeCycleExtension() {
        FlowableFailedJobRetryTimeCycle timeCycle =
                sendTask.getExtensionElements().getElementsQuery().filterByType(FlowableFailedJobRetryTimeCycle.class).singleResult();
        assertThat(timeCycle.getTextContent()).isEqualTo(TEST_STRING_XML);
        timeCycle.setTextContent(TEST_STRING_API);
        assertThat(timeCycle.getTextContent()).isEqualTo(TEST_STRING_API);
    }

    @Test
    public void fieldExtension() {
        FlowableField field = sendTask.getExtensionElements().getElementsQuery().filterByType(FlowableField.class).singleResult();
        assertThat(field.getFlowableName()).isEqualTo(TEST_STRING_XML);
        assertThat(field.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_XML);
        assertThat(field.getFlowableStringValue()).isEqualTo(TEST_STRING_XML);
        assertThat(field.getFlowableExpressionChild().getTextContent()).isEqualTo(TEST_EXPRESSION_XML);
        assertThat(field.getFlowableString().getTextContent()).isEqualTo(TEST_STRING_XML);
        field.setFlowableName(TEST_STRING_API);
        field.setFlowableExpression(TEST_EXPRESSION_API);
        field.setFlowableStringValue(TEST_STRING_API);
        field.getFlowableExpressionChild().setTextContent(TEST_EXPRESSION_API);
        field.getFlowableString().setTextContent(TEST_STRING_API);
        assertThat(field.getFlowableName()).isEqualTo(TEST_STRING_API);
        assertThat(field.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_API);
        assertThat(field.getFlowableStringValue()).isEqualTo(TEST_STRING_API);
        assertThat(field.getFlowableExpressionChild().getTextContent()).isEqualTo(TEST_EXPRESSION_API);
        assertThat(field.getFlowableString().getTextContent()).isEqualTo(TEST_STRING_API);
    }

    @Test
    public void formData() {
        FlowableFormData formData = userTask.getExtensionElements().getElementsQuery().filterByType(FlowableFormData.class).singleResult();
        FlowableFormField formField = formData.getFlowableFormFields().iterator().next();
        assertThat(formField.getFlowableId()).isEqualTo(TEST_STRING_XML);
        assertThat(formField.getFlowableType()).isEqualTo(TEST_STRING_XML);
        formField.setFlowableId(TEST_STRING_API);
        formField.setFlowableType(TEST_STRING_API);
        assertThat(formField.getFlowableId()).isEqualTo(TEST_STRING_API);
        assertThat(formField.getFlowableType()).isEqualTo(TEST_STRING_API);

        FlowableProperty property = formField.getFlowableProperties().getFlowableProperties().iterator().next();
        assertThat(property.getFlowableId()).isEqualTo(TEST_STRING_XML);
        assertThat(property.getFlowableValue()).isEqualTo(TEST_STRING_XML);
        property.setFlowableId(TEST_STRING_API);
        property.setFlowableValue(TEST_STRING_API);
        assertThat(property.getFlowableId()).isEqualTo(TEST_STRING_API);
        assertThat(property.getFlowableValue()).isEqualTo(TEST_STRING_API);

        FlowableConstraint constraint = formField.getFlowableValidation().getFlowableConstraints().iterator().next();
        assertThat(constraint.getFlowableName()).isEqualTo(TEST_STRING_XML);
        constraint.setFlowableName(TEST_STRING_API);
        assertThat(constraint.getFlowableName()).isEqualTo(TEST_STRING_API);

        FlowableValue value = formField.getFlowableValues().iterator().next();
        assertThat(value.getFlowableId()).isEqualTo(TEST_STRING_XML);
        assertThat(value.getFlowableName()).isEqualTo(TEST_STRING_XML);
        value.setFlowableId(TEST_STRING_API);
        value.setFlowableName(TEST_STRING_API);
        assertThat(value.getFlowableId()).isEqualTo(TEST_STRING_API);
        assertThat(value.getFlowableName()).isEqualTo(TEST_STRING_API);
    }

    @Test
    public void formProperty() {
        FlowableFormProperty formProperty = startEvent.getExtensionElements().getElementsQuery().filterByType(FlowableFormProperty.class).singleResult();
        assertThat(formProperty.getFlowableId()).isEqualTo(TEST_STRING_XML);
        assertThat(formProperty.getFlowableName()).isEqualTo(TEST_STRING_XML);
        assertThat(formProperty.getFlowableType()).isEqualTo(TEST_STRING_XML);
        assertThat(formProperty.isFlowableRequired()).isFalse();
        assertThat(formProperty.isFlowableReadable()).isTrue();
        assertThat(formProperty.isFlowableWriteable()).isTrue();
        assertThat(formProperty.getFlowableVariable()).isEqualTo(TEST_STRING_XML);
        assertThat(formProperty.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_XML);
        formProperty.setFlowableId(TEST_STRING_API);
        formProperty.setFlowableName(TEST_STRING_API);
        formProperty.setFlowableType(TEST_STRING_API);
        formProperty.setFlowableRequired(true);
        formProperty.setFlowableReadable(false);
        formProperty.setFlowableWriteable(false);
        formProperty.setFlowableVariable(TEST_STRING_API);
        formProperty.setFlowableExpression(TEST_EXPRESSION_API);
        assertThat(formProperty.getFlowableId()).isEqualTo(TEST_STRING_API);
        assertThat(formProperty.getFlowableName()).isEqualTo(TEST_STRING_API);
        assertThat(formProperty.getFlowableType()).isEqualTo(TEST_STRING_API);
        assertThat(formProperty.isFlowableRequired()).isTrue();
        assertThat(formProperty.isFlowableReadable()).isFalse();
        assertThat(formProperty.isFlowableWriteable()).isFalse();
        assertThat(formProperty.getFlowableVariable()).isEqualTo(TEST_STRING_API);
        assertThat(formProperty.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_API);
    }

    @Test
    public void inExtension() {
        FlowableIn in = callActivity.getExtensionElements().getElementsQuery().filterByType(FlowableIn.class).singleResult();
        assertThat(in.getFlowableSource()).isEqualTo(TEST_STRING_XML);
        assertThat(in.getFlowableSourceExpression()).isEqualTo(TEST_EXPRESSION_XML);
        assertThat(in.getFlowableTarget()).isEqualTo(TEST_STRING_XML);
        in.setFlowableSource(TEST_STRING_API);
        in.setFlowableSourceExpression(TEST_EXPRESSION_API);
        in.setFlowableTarget(TEST_STRING_API);
        assertThat(in.getFlowableSource()).isEqualTo(TEST_STRING_API);
        assertThat(in.getFlowableSourceExpression()).isEqualTo(TEST_EXPRESSION_API);
        assertThat(in.getFlowableTarget()).isEqualTo(TEST_STRING_API);
    }

    @Test
    public void outExtension() {
        FlowableOut out = callActivity.getExtensionElements().getElementsQuery().filterByType(FlowableOut.class).singleResult();
        assertThat(out.getFlowableSource()).isEqualTo(TEST_STRING_XML);
        assertThat(out.getFlowableSourceExpression()).isEqualTo(TEST_EXPRESSION_XML);
        assertThat(out.getFlowableTarget()).isEqualTo(TEST_STRING_XML);
        out.setFlowableSource(TEST_STRING_API);
        out.setFlowableSourceExpression(TEST_EXPRESSION_API);
        out.setFlowableTarget(TEST_STRING_API);
        assertThat(out.getFlowableSource()).isEqualTo(TEST_STRING_API);
        assertThat(out.getFlowableSourceExpression()).isEqualTo(TEST_EXPRESSION_API);
        assertThat(out.getFlowableTarget()).isEqualTo(TEST_STRING_API);
    }

    @Test
    public void potentialStarter() {
        FlowablePotentialStarter potentialStarter =
                startEvent.getExtensionElements().getElementsQuery().filterByType(FlowablePotentialStarter.class).singleResult();
        Expression expression = potentialStarter.getResourceAssignmentExpression().getExpression();
        assertThat(expression.getTextContent()).isEqualTo(TEST_GROUPS_XML);
        expression.setTextContent(TEST_GROUPS_API);
        assertThat(expression.getTextContent()).isEqualTo(TEST_GROUPS_API);
    }

    @Test
    public void taskListener() {
        FlowableTaskListener taskListener = userTask.getExtensionElements().getElementsQuery().filterByType(FlowableTaskListener.class).list().get(0);
        assertThat(taskListener.getFlowableEvent()).isEqualTo(TEST_TASK_EVENT_XML);
        assertThat(taskListener.getFlowableClass()).isEqualTo(TEST_CLASS_XML);
        assertThat(taskListener.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_XML);
        assertThat(taskListener.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_XML);
        taskListener.setFlowableEvent(TEST_TASK_EVENT_API);
        taskListener.setFlowableClass(TEST_CLASS_API);
        taskListener.setFlowableExpression(TEST_EXPRESSION_API);
        taskListener.setFlowableDelegateExpression(TEST_DELEGATE_EXPRESSION_API);
        assertThat(taskListener.getFlowableEvent()).isEqualTo(TEST_TASK_EVENT_API);
        assertThat(taskListener.getFlowableClass()).isEqualTo(TEST_CLASS_API);
        assertThat(taskListener.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_API);
        assertThat(taskListener.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);

        FlowableField field = taskListener.getFlowableFields().iterator().next();
        assertThat(field.getFlowableName()).isEqualTo(TEST_STRING_XML);
        assertThat(field.getFlowableString().getTextContent()).isEqualTo(TEST_STRING_XML);
    }

    @Test
    public void flowableScriptTaskListener() {
        FlowableTaskListener taskListener = userTask.getExtensionElements().getElementsQuery().filterByType(FlowableTaskListener.class).list().get(1);

        FlowableScript script = taskListener.getFlowableScript();
        assertThat(script.getFlowableResource()).isEqualTo("test.groovy");
        assertThat(script.getTextContent()).isEmpty();

        FlowableScript newScript = modelInstance.newInstance(FlowableScript.class);
        newScript.setTextContent("println 'Hello World'");
        taskListener.setFlowableScript(newScript);

        script = taskListener.getFlowableScript();
        assertThat(script.getFlowableResource()).isNull();
        assertThat(script.getTextContent()).isEqualTo("println 'Hello World'");
    }

    @Test
    public void flowableModelerProperties() {
        FlowableProperties flowableProperties = endEvent.getExtensionElements().getElementsQuery().filterByType(FlowableProperties.class).singleResult();
        assertThat(flowableProperties).isNotNull();
        assertThat(flowableProperties.getFlowableProperties()).hasSize(2);

        for (FlowableProperty flowableProperty : flowableProperties.getFlowableProperties()) {
            assertThat(flowableProperty.getFlowableId()).isNull();
            assertThat(flowableProperty.getFlowableName()).startsWith("name");
            assertThat(flowableProperty.getFlowableValue()).startsWith("value");
        }
    }

    @Test
    public void getNonExistingFlowableCandidateUsers() {
        userTask.removeAttributeNs(namespace, "candidateUsers");
        assertThat(userTask.getFlowableCandidateUsers()).isNull();
        assertThat(userTask.getFlowableCandidateUsersList()).isEmpty();
    }

    @Test
    public void setNullFlowableCandidateUsers() {
        assertThat(userTask.getFlowableCandidateUsers()).isNotEmpty();
        assertThat(userTask.getFlowableCandidateUsersList()).isNotEmpty();
        userTask.setFlowableCandidateUsers(null);
        assertThat(userTask.getFlowableCandidateUsers()).isNull();
        assertThat(userTask.getFlowableCandidateUsersList()).isEmpty();
    }

    @Test
    public void emptyFlowableCandidateUsers() {
        assertThat(userTask.getFlowableCandidateUsers()).isNotEmpty();
        assertThat(userTask.getFlowableCandidateUsersList()).isNotEmpty();
        userTask.setFlowableCandidateUsers("");
        assertThat(userTask.getFlowableCandidateUsers()).isNull();
        assertThat(userTask.getFlowableCandidateUsersList()).isEmpty();
    }

    @Test
    public void setNullFlowableCandidateUsersList() {
        assertThat(userTask.getFlowableCandidateUsers()).isNotEmpty();
        assertThat(userTask.getFlowableCandidateUsersList()).isNotEmpty();
        userTask.setFlowableCandidateUsersList(null);
        assertThat(userTask.getFlowableCandidateUsers()).isNull();
        assertThat(userTask.getFlowableCandidateUsersList()).isEmpty();
    }

    @Test
    public void emptyFlowableCandidateUsersList() {
        assertThat(userTask.getFlowableCandidateUsers()).isNotEmpty();
        assertThat(userTask.getFlowableCandidateUsersList()).isNotEmpty();
        userTask.setFlowableCandidateUsersList(Collections.<String>emptyList());
        assertThat(userTask.getFlowableCandidateUsers()).isNull();
        assertThat(userTask.getFlowableCandidateUsersList()).isEmpty();
    }

    @Test
    public void flowableConnector() {
        FlowableConnector flowableConnector = serviceTask.getExtensionElements().getElementsQuery().filterByType(FlowableConnector.class).singleResult();
        assertThat(flowableConnector).isNotNull();

        FlowableConnectorId flowableConnectorId = flowableConnector.getFlowableConnectorId();
        assertThat(flowableConnectorId).isNotNull();
        assertThat(flowableConnectorId.getTextContent()).isEqualTo("soap-http-connector");

        FlowableInputOutput flowableInputOutput = flowableConnector.getFlowableInputOutput();

        Collection<FlowableInputParameter> inputParameters = flowableInputOutput.getFlowableInputParameters();
        assertThat(inputParameters).hasSize(1);

        FlowableInputParameter inputParameter = inputParameters.iterator().next();
        assertThat(inputParameter.getFlowableName()).isEqualTo("endpointUrl");
        assertThat(inputParameter.getTextContent()).isEqualTo("http://example.com/webservice");

        Collection<FlowableOutputParameter> outputParameters = flowableInputOutput.getFlowableOutputParameters();
        assertThat(outputParameters).hasSize(1);

        FlowableOutputParameter outputParameter = outputParameters.iterator().next();
        assertThat(outputParameter.getFlowableName()).isEqualTo("result");
        assertThat(outputParameter.getTextContent()).isEqualTo("output");
    }

    @Test
    public void flowableInputOutput() {
        FlowableInputOutput flowableInputOutput = serviceTask.getExtensionElements().getElementsQuery().filterByType(FlowableInputOutput.class).singleResult();
        assertThat(flowableInputOutput).isNotNull();
        assertThat(flowableInputOutput.getFlowableInputParameters()).hasSize(6);
        assertThat(flowableInputOutput.getFlowableOutputParameters()).hasSize(1);
    }

    @Test
    public void flowableInputParameter() {
        // find existing
        FlowableInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeConstant");

        // modify existing
        inputParameter.setFlowableName("hello");
        inputParameter.setTextContent("world");
        inputParameter = findInputParameterByName(serviceTask, "hello");
        assertThat(inputParameter.getTextContent()).isEqualTo("world");

        // add new one
        inputParameter = modelInstance.newInstance(FlowableInputParameter.class);
        inputParameter.setFlowableName("abc");
        inputParameter.setTextContent("def");
        serviceTask.getExtensionElements().getElementsQuery().filterByType(FlowableInputOutput.class).singleResult()
                .addChildElement(inputParameter);

        // search for new one
        inputParameter = findInputParameterByName(serviceTask, "abc");
        assertThat(inputParameter.getFlowableName()).isEqualTo("abc");
        assertThat(inputParameter.getTextContent()).isEqualTo("def");
    }

    @Test
    public void flowableNullInputParameter() {
        FlowableInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeNull");
        assertThat(inputParameter.getFlowableName()).isEqualTo("shouldBeNull");
        assertThat(inputParameter.getTextContent()).isEmpty();
    }

    @Test
    public void flowableConstantInputParameter() {
        FlowableInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeConstant");
        assertThat(inputParameter.getFlowableName()).isEqualTo("shouldBeConstant");
        assertThat(inputParameter.getTextContent()).isEqualTo("foo");
    }

    @Test
    public void flowableExpressionInputParameter() {
        FlowableInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeExpression");
        assertThat(inputParameter.getFlowableName()).isEqualTo("shouldBeExpression");
        assertThat(inputParameter.getTextContent()).isEqualTo("${1 + 1}");
    }

    @Test
    public void flowableListInputParameter() {
        FlowableInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeList");
        assertThat(inputParameter.getFlowableName()).isEqualTo("shouldBeList");
        assertThat(inputParameter.getTextContent()).isNotEmpty();
        assertThat(inputParameter.getUniqueChildElementByNameNs(FLOWABLE_NS, "list")).isNotNull();

        FlowableList list = inputParameter.getValue();
        assertThat(list.getValues()).hasSize(3);
        for (BpmnModelElementInstance values : list.getValues()) {
            assertThat(values.getTextContent()).isIn("a", "b", "c");
        }

        list = modelInstance.newInstance(FlowableList.class);
        for (int i = 0; i < 4; i++) {
            FlowableValue value = modelInstance.newInstance(FlowableValue.class);
            value.setTextContent("test");
            list.getValues().add(value);
        }
        Collection<FlowableValue> testValues = Arrays.asList(modelInstance.newInstance(FlowableValue.class), modelInstance.newInstance(FlowableValue.class));
        list.getValues().addAll(testValues);
        inputParameter.setValue(list);

        list = inputParameter.getValue();
        assertThat(list.getValues()).hasSize(6);
        list.getValues().removeAll(testValues);
        ArrayList<BpmnModelElementInstance> flowableValues = new ArrayList<>(list.getValues());
        assertThat(flowableValues).hasSize(4);
        for (BpmnModelElementInstance value : flowableValues) {
            assertThat(value.getTextContent()).isEqualTo("test");
        }

        list.getValues().remove(flowableValues.get(1));
        assertThat(list.getValues()).hasSize(3);

        list.getValues().removeAll(Arrays.asList(flowableValues.get(0), flowableValues.get(3)));
        assertThat(list.getValues()).hasSize(1);

        list.getValues().clear();
        assertThat(list.getValues()).isEmpty();

        // test standard list interactions
        Collection<BpmnModelElementInstance> elements = list.getValues();

        FlowableValue value = modelInstance.newInstance(FlowableValue.class);
        elements.add(value);

        List<FlowableValue> newValues = new ArrayList<>();
        newValues.add(modelInstance.newInstance(FlowableValue.class));
        newValues.add(modelInstance.newInstance(FlowableValue.class));
        elements.addAll(newValues);
        assertThat(elements).hasSize(3);

        assertThat(elements).doesNotContain(modelInstance.newInstance(FlowableValue.class));
        assertThat(elements.containsAll(Collections.singletonList(modelInstance.newInstance(FlowableValue.class)))).isFalse();

        assertThat(elements.remove(modelInstance.newInstance(FlowableValue.class))).isFalse();
        assertThat(elements).hasSize(3);

        assertThat(elements.remove(value)).isTrue();
        assertThat(elements).hasSize(2);

        assertThat(elements.removeAll(newValues)).isTrue();
        assertThat(elements).isEmpty();

        elements.add(modelInstance.newInstance(FlowableValue.class));
        elements.clear();
        assertThat(elements).isEmpty();

        inputParameter.removeValue();
        assertThat(inputParameter.getValue()).isNull();

    }

    @Test
    public void flowableMapInputParameter() {
        FlowableInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeMap");
        assertThat(inputParameter.getFlowableName()).isEqualTo("shouldBeMap");
        assertThat(inputParameter.getTextContent()).isNotEmpty();
        assertThat(inputParameter.getUniqueChildElementByNameNs(FLOWABLE_NS, "map")).isNotNull();

        FlowableMap map = inputParameter.getValue();
        assertThat(map.getFlowableEntries()).hasSize(2);

        map = modelInstance.newInstance(FlowableMap.class);
        FlowableEntry entry = modelInstance.newInstance(FlowableEntry.class);
        entry.setTextContent("value");
        map.getFlowableEntries().add(entry);

        inputParameter.setValue(map);
        map = inputParameter.getValue();
        assertThat(map.getFlowableEntries()).hasSize(1);
        entry = map.getFlowableEntries().iterator().next();
        assertThat(entry.getTextContent()).isEqualTo("value");

        Collection<FlowableEntry> entries = map.getFlowableEntries();
        entries.add(modelInstance.newInstance(FlowableEntry.class));
        assertThat(entries).hasSize(2);

        inputParameter.removeValue();
        assertThat(inputParameter.getValue()).isNull();
    }

    @Test
    public void flowableScriptInputParameter() {
        FlowableInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeScript");
        assertThat(inputParameter.getFlowableName()).isEqualTo("shouldBeScript");
        assertThat(inputParameter.getTextContent()).isNotEmpty();
        assertThat(inputParameter.getUniqueChildElementByNameNs(FLOWABLE_NS, "script")).isNotNull();
        assertThat(inputParameter.getUniqueChildElementByType(FlowableScript.class)).isNotNull();

        FlowableScript script = inputParameter.getValue();
        assertThat(script.getFlowableResource()).isNull();
        assertThat(script.getTextContent()).isEqualTo("1 + 1");

        script = modelInstance.newInstance(FlowableScript.class);
        script.setFlowableResource("script.py");

        inputParameter.setValue(script);

        script = inputParameter.getValue();
        assertThat(script.getFlowableResource()).isEqualTo("script.py");
        assertThat(script.getTextContent()).isEmpty();

        inputParameter.removeValue();
        assertThat(inputParameter.getValue()).isNull();
    }

    @Test
    public void flowableNestedOutputParameter() {
        FlowableOutputParameter flowableOutputParameter = serviceTask.getExtensionElements().getElementsQuery().filterByType(FlowableInputOutput.class)
                .singleResult().getFlowableOutputParameters().iterator().next();

        assertThat(flowableOutputParameter).isNotNull();
        assertThat(flowableOutputParameter.getFlowableName()).isEqualTo("nested");
        FlowableList list = flowableOutputParameter.getValue();
        assertThat(list).isNotNull();
        assertThat(list.getValues()).hasSize(2);
        Iterator<BpmnModelElementInstance> iterator = list.getValues().iterator();

        // nested list
        FlowableList nestedList = (FlowableList) iterator.next().getUniqueChildElementByType(FlowableList.class);
        assertThat(nestedList).isNotNull();
        assertThat(nestedList.getValues()).hasSize(2);
        for (BpmnModelElementInstance value : nestedList.getValues()) {
            assertThat(value.getTextContent()).isEqualTo("list");
        }

        // nested map
        FlowableMap nestedMap = (FlowableMap) iterator.next().getUniqueChildElementByType(FlowableMap.class);
        assertThat(nestedMap).isNotNull();
        assertThat(nestedMap.getFlowableEntries()).hasSize(2);
        Iterator<FlowableEntry> mapIterator = nestedMap.getFlowableEntries().iterator();

        // nested list in nested map
        FlowableEntry nestedListEntry = mapIterator.next();
        assertThat(nestedListEntry).isNotNull();
        FlowableList nestedNestedList = nestedListEntry.getValue();
        for (BpmnModelElementInstance value : nestedNestedList.getValues()) {
            assertThat(value.getTextContent()).isEqualTo("map");
        }

        // nested map in nested map
        FlowableEntry nestedMapEntry = mapIterator.next();
        assertThat(nestedMapEntry).isNotNull();
        FlowableMap nestedNestedMap = nestedMapEntry.getValue();
        FlowableEntry entry = nestedNestedMap.getFlowableEntries().iterator().next();
        assertThat(entry.getTextContent()).isEqualTo("nested");
    }

    protected FlowableInputParameter findInputParameterByName(BaseElement baseElement, String name) {
        Collection<FlowableInputParameter> flowableInputParameters = baseElement.getExtensionElements().getElementsQuery()
                .filterByType(FlowableInputOutput.class).singleResult().getFlowableInputParameters();
        for (FlowableInputParameter flowableInputParameter : flowableInputParameters) {
            if (flowableInputParameter.getFlowableName().equals(name)) {
                return flowableInputParameter;
            }
        }
        throw new BpmnModelException("Unable to find flowable:inputParameter with name '" + name + "' for element with id '" + baseElement.getId() + '\'');
    }

    @After
    public void validateModel() {
        BpmnModelBuilder.validateModel(modelInstance);
    }
}
