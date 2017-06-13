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
package org.flowable.bpm.model.bpmn.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.BOUNDARY_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.CALL_ACTIVITY_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.CATCH_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.CONDITION_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.SERVICE_TASK_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.START_EVENT_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.SUB_PROCESS_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TASK_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_CLASS_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_CONDITION;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_DELEGATE_EXPRESSION_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_DUE_DATE_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_EXPRESSION_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_GROUPS_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_GROUPS_LIST_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_PRIORITY_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_STRING_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_USERS_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TEST_USERS_LIST_API;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.TRANSACTION_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.USER_TASK_ID;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;

import org.flowable.bpm.model.bpmn.BpmnModelBuilder;
import org.flowable.bpm.model.bpmn.BpmnModelException;
import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.GatewayDirection;
import org.flowable.bpm.model.bpmn.TransactionMethod;
import org.flowable.bpm.model.bpmn.instance.Activity;
import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.BoundaryEvent;
import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.flowable.bpm.model.bpmn.instance.BusinessRuleTask;
import org.flowable.bpm.model.bpmn.instance.CallActivity;
import org.flowable.bpm.model.bpmn.instance.CompensateEventDefinition;
import org.flowable.bpm.model.bpmn.instance.ConditionalEventDefinition;
import org.flowable.bpm.model.bpmn.instance.Definitions;
import org.flowable.bpm.model.bpmn.instance.EndEvent;
import org.flowable.bpm.model.bpmn.instance.Error;
import org.flowable.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.flowable.bpm.model.bpmn.instance.Escalation;
import org.flowable.bpm.model.bpmn.instance.EscalationEventDefinition;
import org.flowable.bpm.model.bpmn.instance.Event;
import org.flowable.bpm.model.bpmn.instance.EventDefinition;
import org.flowable.bpm.model.bpmn.instance.ExtensionElements;
import org.flowable.bpm.model.bpmn.instance.FlowNode;
import org.flowable.bpm.model.bpmn.instance.Gateway;
import org.flowable.bpm.model.bpmn.instance.InclusiveGateway;
import org.flowable.bpm.model.bpmn.instance.Message;
import org.flowable.bpm.model.bpmn.instance.MessageEventDefinition;
import org.flowable.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import org.flowable.bpm.model.bpmn.instance.Process;
import org.flowable.bpm.model.bpmn.instance.ReceiveTask;
import org.flowable.bpm.model.bpmn.instance.ScriptTask;
import org.flowable.bpm.model.bpmn.instance.SendTask;
import org.flowable.bpm.model.bpmn.instance.SequenceFlow;
import org.flowable.bpm.model.bpmn.instance.ServiceTask;
import org.flowable.bpm.model.bpmn.instance.Signal;
import org.flowable.bpm.model.bpmn.instance.SignalEventDefinition;
import org.flowable.bpm.model.bpmn.instance.StartEvent;
import org.flowable.bpm.model.bpmn.instance.SubProcess;
import org.flowable.bpm.model.bpmn.instance.Task;
import org.flowable.bpm.model.bpmn.instance.TimeCycle;
import org.flowable.bpm.model.bpmn.instance.TimeDate;
import org.flowable.bpm.model.bpmn.instance.TimeDuration;
import org.flowable.bpm.model.bpmn.instance.TimerEventDefinition;
import org.flowable.bpm.model.bpmn.instance.Transaction;
import org.flowable.bpm.model.bpmn.instance.UserTask;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableExecutionListener;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFailedJobRetryTimeCycle;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormData;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormField;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableIn;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableInputOutput;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableInputParameter;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableOut;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableOutputParameter;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableTaskListener;
import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProcessBuilderTest {

    public static final String TIMER_DATE = "2011-03-11T12:13:14Z";
    public static final String TIMER_DURATION = "P10D";
    public static final String TIMER_CYCLE = "R3/PT10H";

    public static final String FAILED_JOB_RETRY_TIME_CYCLE = "R5/PT1M";

    private BpmnModelInstance modelInstance;
    private static ModelElementType taskType;
    private static ModelElementType gatewayType;
    private static ModelElementType eventType;
    private static ModelElementType processType;

    @BeforeClass
    public static void getElementTypes() {
        Model model = BpmnModelBuilder.createEmptyModel().getModel();
        taskType = model.getType(Task.class);
        gatewayType = model.getType(Gateway.class);
        eventType = model.getType(Event.class);
        processType = model.getType(Process.class);
    }

    @After
    public void validateModel()
        throws IOException {
        if (modelInstance != null) {
            BpmnModelBuilder.validateModel(modelInstance);
        }
    }

    @Test
    public void createEmptyProcess() {
        modelInstance = BpmnModelBuilder.createProcess()
                .done();

        Definitions definitions = modelInstance.getDefinitions();
        assertThat(definitions).isNotNull();
        assertThat(definitions.getTargetNamespace()).isEqualTo(BPMN20_NS);

        Collection<ModelElementInstance> processes = modelInstance.getModelElementsByType(processType);
        assertThat(processes)
                .hasSize(1);

        Process process = (Process) processes.iterator().next();
        assertThat(process.getId()).isNotNull();
    }

    @Test
    public void getElement() {
        // Make sure this method is publicly available
        Process process = BpmnModelBuilder.createProcess().getElement();
        assertThat(process).isNotNull();
    }

    @Test
    public void createProcessWithStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithEndEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(2);
    }

    @Test
    public void createProcessWithServiceTask() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .serviceTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(2);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithSendTask() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .sendTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(2);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithUserTask() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(2);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithBusinessRuleTask() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .businessRuleTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(2);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithScriptTask() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .scriptTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(2);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithReceiveTask() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .receiveTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(2);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithManualTask() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .manualTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(2);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithParallelGateway() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .parallelGateway()
                .scriptTask()
                .endEvent()
                .moveToLastGateway()
                .userTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(3);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(2);
        assertThat(modelInstance.getModelElementsByType(gatewayType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithExclusiveGateway() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask()
                .exclusiveGateway()
                .condition("approved", "${approved}")
                .serviceTask()
                .endEvent()
                .moveToLastGateway()
                .condition("not approved", "${!approved}")
                .scriptTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(3);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(3);
        assertThat(modelInstance.getModelElementsByType(gatewayType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithInclusiveGateway() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask()
                .inclusiveGateway()
                .condition("approved", "${approved}")
                .serviceTask()
                .endEvent()
                .moveToLastGateway()
                .condition("not approved", "${!approved}")
                .scriptTask()
                .endEvent()
                .done();

        ModelElementType inclusiveGwType = modelInstance.getModel().getType(InclusiveGateway.class);

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(3);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(3);
        assertThat(modelInstance.getModelElementsByType(inclusiveGwType))
                .hasSize(1);
    }

    @Test
    public void createProcessWithForkAndJoin() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask()
                .parallelGateway()
                .serviceTask()
                .parallelGateway()
                .id("join")
                .moveToLastGateway()
                .scriptTask()
                .connectTo("join")
                .userTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(2);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(4);
        assertThat(modelInstance.getModelElementsByType(gatewayType))
                .hasSize(2);
    }

    @Test
    public void createProcessWithMultipleParallelTask() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .parallelGateway("fork")
                .userTask()
                .parallelGateway("join")
                .moveToNode("fork")
                .serviceTask()
                .connectTo("join")
                .moveToNode("fork")
                .userTask()
                .connectTo("join")
                .moveToNode("fork")
                .scriptTask()
                .connectTo("join")
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(eventType))
                .hasSize(2);
        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(4);
        assertThat(modelInstance.getModelElementsByType(gatewayType))
                .hasSize(2);
    }

    @Test
    public void extend() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask()
                .id("task1")
                .serviceTask()
                .endEvent()
                .done();

        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(2);

        UserTask userTask = modelInstance.getModelElementById("task1");
        SequenceFlow outgoingSequenceFlow = userTask.getOutgoing().iterator().next();
        FlowNode serviceTask = outgoingSequenceFlow.getTarget();
        userTask.getOutgoing().remove(outgoingSequenceFlow);
        userTask.builder()
                .scriptTask()
                .userTask()
                .connectTo(serviceTask.getId());

        assertThat(modelInstance.getModelElementsByType(taskType))
                .hasSize(4);
    }

    @Test
    public void createInvoiceProcess() {
        modelInstance = BpmnModelBuilder.createProcess()
                .executable()
                .startEvent()
                .name("Invoice received")
                .flowableFormKey("embedded:app:forms/start-form.html")
                .userTask()
                .name("Assign Approver")
                .flowableFormKey("embedded:app:forms/assign-approver.html")
                .flowableAssignee("demo")
                .userTask("approveInvoice")
                .name("Approve Invoice")
                .flowableFormKey("embedded:app:forms/approve-invoice.html")
                .flowableAssignee("${approver}")
                .exclusiveGateway()
                .name("Invoice approved?")
                .gatewayDirection(GatewayDirection.Diverging)
                .condition("yes", "${approved}")
                .userTask()
                .name("Prepare Bank Transfer")
                .flowableFormKey("embedded:app:forms/prepare-bank-transfer.html")
                .flowableCandidateGroups("accounting")
                .serviceTask()
                .name("Archive Invoice")
                .flowableClass("org.flowable.bpm.example.invoice.service.ArchiveInvoiceService")
                .endEvent()
                .name("Invoice processed")
                .moveToLastGateway()
                .condition("no", "${!approved}")
                .userTask()
                .name("Review Invoice")
                .flowableFormKey("embedded:app:forms/review-invoice.html")
                .flowableAssignee("demo")
                .exclusiveGateway()
                .name("Review successful?")
                .gatewayDirection(GatewayDirection.Diverging)
                .condition("no", "${!clarified}")
                .endEvent()
                .name("Invoice not processed")
                .moveToLastGateway()
                .condition("yes", "${clarified}")
                .connectTo("approveInvoice")
                .done();
    }

    @Test
    public void taskFlowableExtensions() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .serviceTask(TASK_ID)
                .notFlowableExclusive()
                .flowableFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
                .endEvent()
                .done();

        ServiceTask serviceTask = modelInstance.getModelElementById(TASK_ID);
        assertThat(serviceTask.isFlowableExclusive()).isFalse();

        assertFlowableFailedJobRetryTimeCycle(serviceTask);
    }

    @Test
    public void serviceTaskFlowableExtensions() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .serviceTask(TASK_ID)
                .flowableClass(TEST_CLASS_API)
                .flowableDelegateExpression(TEST_DELEGATE_EXPRESSION_API)
                .flowableExpression(TEST_EXPRESSION_API)
                .flowableResultVariable(TEST_STRING_API)
                .flowableType(TEST_STRING_API)
                .flowableFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
                .done();

        ServiceTask serviceTask = modelInstance.getModelElementById(TASK_ID);
        assertThat(serviceTask.getFlowableClass()).isEqualTo(TEST_CLASS_API);
        assertThat(serviceTask.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
        assertThat(serviceTask.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_API);
        assertThat(serviceTask.getFlowableResultVariable()).isEqualTo(TEST_STRING_API);
        assertThat(serviceTask.getFlowableType()).isEqualTo(TEST_STRING_API);

        assertFlowableFailedJobRetryTimeCycle(serviceTask);
    }

    @Test
    public void serviceTaskFlowableClass() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .serviceTask(TASK_ID)
                .flowableClass(getClass().getName())
                .done();

        ServiceTask serviceTask = modelInstance.getModelElementById(TASK_ID);
        assertThat(serviceTask.getFlowableClass()).isEqualTo(getClass().getName());
    }


    @Test
    public void sendTaskFlowableExtensions() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .sendTask(TASK_ID)
                .flowableClass(TEST_CLASS_API)
                .flowableDelegateExpression(TEST_DELEGATE_EXPRESSION_API)
                .flowableExpression(TEST_EXPRESSION_API)
                .flowableResultVariable(TEST_STRING_API)
                .flowableFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
                .endEvent()
                .done();

        SendTask sendTask = modelInstance.getModelElementById(TASK_ID);
        assertThat(sendTask.getFlowableClass()).isEqualTo(TEST_CLASS_API);
        assertThat(sendTask.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
        assertThat(sendTask.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_API);
        assertThat(sendTask.getFlowableResultVariable()).isEqualTo(TEST_STRING_API);

        assertFlowableFailedJobRetryTimeCycle(sendTask);
    }

    @Test
    public void sendTaskFlowableClass() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .sendTask(TASK_ID)
                .flowableClass(this.getClass())
                .endEvent()
                .done();

        SendTask sendTask = modelInstance.getModelElementById(TASK_ID);
        assertThat(sendTask.getFlowableClass()).isEqualTo(this.getClass().getName());
    }

    @Test
    public void userTaskFlowableExtensions() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask(TASK_ID)
                .flowableAssignee(TEST_STRING_API)
                .flowableCandidateGroups(TEST_GROUPS_API)
                .flowableCandidateUsers(TEST_USERS_LIST_API)
                .flowableDueDate(TEST_DUE_DATE_API)
                .flowableFormHandlerClass(TEST_CLASS_API)
                .flowableFormKey(TEST_STRING_API)
                .flowablePriority(TEST_PRIORITY_API)
                .flowableFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById(TASK_ID);
        assertThat(userTask.getFlowableAssignee()).isEqualTo(TEST_STRING_API);
        assertThat(userTask.getFlowableCandidateGroups()).isEqualTo(TEST_GROUPS_API);
        assertThat(userTask.getFlowableCandidateGroupsList()).containsAll(TEST_GROUPS_LIST_API);
        assertThat(userTask.getFlowableCandidateUsers()).isEqualTo(TEST_USERS_API);
        assertThat(userTask.getFlowableCandidateUsersList()).containsAll(TEST_USERS_LIST_API);
        assertThat(userTask.getFlowableDueDate()).isEqualTo(TEST_DUE_DATE_API);
        assertThat(userTask.getFlowableFormHandlerClass()).isEqualTo(TEST_CLASS_API);
        assertThat(userTask.getFlowableFormKey()).isEqualTo(TEST_STRING_API);
        assertThat(userTask.getFlowablePriority()).isEqualTo(TEST_PRIORITY_API);

        assertFlowableFailedJobRetryTimeCycle(userTask);
    }

    @Test
    public void businessRuleTaskFlowableExtensions() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .businessRuleTask(TASK_ID)
                .flowableClass(TEST_CLASS_API)
                .flowableDelegateExpression(TEST_DELEGATE_EXPRESSION_API)
                .flowableExpression(TEST_EXPRESSION_API)
                .flowableResultVariable("resultVar")
                .flowableType("type")
                .flowableFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
                .endEvent()
                .done();

        BusinessRuleTask businessRuleTask = modelInstance.getModelElementById(TASK_ID);
        assertThat(businessRuleTask.getFlowableClass()).isEqualTo(TEST_CLASS_API);
        assertThat(businessRuleTask.getFlowableDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
        assertThat(businessRuleTask.getFlowableExpression()).isEqualTo(TEST_EXPRESSION_API);
        assertThat(businessRuleTask.getFlowableResultVariable()).isEqualTo("resultVar");
        assertThat(businessRuleTask.getFlowableType()).isEqualTo("type");
        assertFlowableFailedJobRetryTimeCycle(businessRuleTask);
    }

    @Test
    public void businessRuleTaskFlowableClass() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .businessRuleTask(TASK_ID)
                .flowableClass(BpmnModelBuilder.class)
                .endEvent()
                .done();

        BusinessRuleTask businessRuleTask = modelInstance.getModelElementById(TASK_ID);
        assertThat(businessRuleTask.getFlowableClass()).isEqualTo("org.flowable.bpm.model.bpmn.BpmnModelBuilder");
    }

    @Test
    public void scriptTaskFlowableExtensions() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .scriptTask(TASK_ID)
                .flowableResultVariable(TEST_STRING_API)
                .flowableFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
                .endEvent()
                .done();

        ScriptTask scriptTask = modelInstance.getModelElementById(TASK_ID);
        assertThat(scriptTask.getFlowableResultVariable()).isEqualTo(TEST_STRING_API);

        assertFlowableFailedJobRetryTimeCycle(scriptTask);
    }

    @Test
    public void startEventFlowableExtensions() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent(START_EVENT_ID)
                .notFlowableExclusive()
                .flowableFormHandlerClass(TEST_CLASS_API)
                .flowableFormKey(TEST_STRING_API)
                .flowableInitiator(TEST_STRING_API)
                .flowableFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
                .done();

        StartEvent startEvent = modelInstance.getModelElementById(START_EVENT_ID);
        assertThat(startEvent.isFlowableExclusive()).isFalse();
        assertThat(startEvent.getFlowableFormHandlerClass()).isEqualTo(TEST_CLASS_API);
        assertThat(startEvent.getFlowableFormKey()).isEqualTo(TEST_STRING_API);
        assertThat(startEvent.getFlowableInitiator()).isEqualTo(TEST_STRING_API);

        assertFlowableFailedJobRetryTimeCycle(startEvent);
    }

    @Test
    public void errorDefinitionsForStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent("start")
                .errorEventDefinition("event")
                .error("errorCode")
                .errorEventDefinitionDone()
                .endEvent().done();

        assertErrorEventDefinition("start", "errorCode");
    }

    @Test
    public void errorDefinitionsForStartEventWithoutEventDefinitionId() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent("start")
                .errorEventDefinition()
                .error("errorCode")
                .errorEventDefinitionDone()
                .endEvent().done();

        assertErrorEventDefinition("start", "errorCode");
    }

    @Test
    public void callActivityFlowableExtension() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .callActivity(CALL_ACTIVITY_ID)
                .calledElement(TEST_STRING_API)
                .flowableIn("in-source", "in-target")
                .flowableOut("out-source", "out-target")
                .notFlowableExclusive()
                .flowableFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
                .endEvent()
                .done();

        CallActivity callActivity = modelInstance.getModelElementById(CALL_ACTIVITY_ID);
        assertThat(callActivity.getCalledElement()).isEqualTo(TEST_STRING_API);
        assertThat(callActivity.isFlowableExclusive()).isFalse();

        FlowableIn flowableIn = (FlowableIn) callActivity.getExtensionElements().getUniqueChildElementByType(FlowableIn.class);
        assertThat(flowableIn.getFlowableSource()).isEqualTo("in-source");
        assertThat(flowableIn.getFlowableTarget()).isEqualTo("in-target");

        FlowableOut flowableOut = (FlowableOut) callActivity.getExtensionElements().getUniqueChildElementByType(FlowableOut.class);
        assertThat(flowableOut.getFlowableSource()).isEqualTo("out-source");
        assertThat(flowableOut.getFlowableTarget()).isEqualTo("out-target");

        assertFlowableFailedJobRetryTimeCycle(callActivity);
    }

    @Test
    public void subProcessBuilder() {
        BpmnModelInstance modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .subProcess(SUB_PROCESS_ID)
                .embeddedSubProcess()
                .startEvent()
                .userTask()
                .endEvent()
                .subProcessDone()
                .serviceTask(SERVICE_TASK_ID)
                .endEvent()
                .done();

        SubProcess subProcess = modelInstance.getModelElementById(SUB_PROCESS_ID);
        ServiceTask serviceTask = modelInstance.getModelElementById(SERVICE_TASK_ID);
        assertThat(subProcess.isFlowableExclusive()).isTrue();
        assertThat(subProcess.getChildElementsByType(Event.class)).hasSize(2);
        assertThat(subProcess.getChildElementsByType(Task.class)).hasSize(1);
        assertThat(subProcess.getFlowElements()).hasSize(5);
        assertThat(subProcess.getSucceedingNodes().singleResult()).isEqualTo(serviceTask);
    }

    @Test
    public void subProcessBuilderDetached() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .subProcess(SUB_PROCESS_ID)
                .serviceTask(SERVICE_TASK_ID)
                .endEvent()
                .done();

        SubProcess subProcess = modelInstance.getModelElementById(SUB_PROCESS_ID);

        subProcess.builder()
                .embeddedSubProcess()
                .startEvent()
                .userTask()
                .endEvent();

        ServiceTask serviceTask = modelInstance.getModelElementById(SERVICE_TASK_ID);
        assertThat(subProcess.isFlowableExclusive()).isTrue();
        assertThat(subProcess.getChildElementsByType(Event.class)).hasSize(2);
        assertThat(subProcess.getChildElementsByType(Task.class)).hasSize(1);
        assertThat(subProcess.getFlowElements()).hasSize(5);
        assertThat(subProcess.getSucceedingNodes().singleResult()).isEqualTo(serviceTask);
    }

    @Test
    public void subProcessBuilderNested() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .subProcess(SUB_PROCESS_ID + 1)
                .embeddedSubProcess()
                .startEvent()
                .userTask()
                .subProcess(SUB_PROCESS_ID + 2)
                .notFlowableExclusive()
                .embeddedSubProcess()
                .startEvent()
                .userTask()
                .endEvent()
                .subProcessDone()
                .serviceTask(SERVICE_TASK_ID + 1)
                .endEvent()
                .subProcessDone()
                .serviceTask(SERVICE_TASK_ID + 2)
                .endEvent()
                .done();

        SubProcess subProcess = modelInstance.getModelElementById(SUB_PROCESS_ID + 1);
        ServiceTask serviceTask = modelInstance.getModelElementById(SERVICE_TASK_ID + 2);
        assertThat(subProcess.isFlowableExclusive()).isTrue();
        assertThat(subProcess.getChildElementsByType(Event.class)).hasSize(2);
        assertThat(subProcess.getChildElementsByType(Task.class)).hasSize(2);
        assertThat(subProcess.getChildElementsByType(SubProcess.class)).hasSize(1);
        assertThat(subProcess.getFlowElements()).hasSize(9);
        assertThat(subProcess.getSucceedingNodes().singleResult()).isEqualTo(serviceTask);

        SubProcess nestedSubProcess = modelInstance.getModelElementById(SUB_PROCESS_ID + 2);
        ServiceTask nestedServiceTask = modelInstance.getModelElementById(SERVICE_TASK_ID + 1);
        assertThat(nestedSubProcess.isFlowableExclusive()).isFalse();
        assertThat(nestedSubProcess.getChildElementsByType(Event.class)).hasSize(2);
        assertThat(nestedSubProcess.getChildElementsByType(Task.class)).hasSize(1);
        assertThat(nestedSubProcess.getFlowElements()).hasSize(5);
        assertThat(nestedSubProcess.getSucceedingNodes().singleResult()).isEqualTo(nestedServiceTask);
    }

    @Test
    public void subProcessBuilderWrongScope() {
        try {
            modelInstance = BpmnModelBuilder.createProcess()
                    .startEvent()
                    .subProcessDone()
                    .endEvent()
                    .done();
            fail("Exception expected");
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(BpmnModelException.class);
        }
    }

    @Test
    public void transactionBuilder() {
        BpmnModelInstance modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .transaction(TRANSACTION_ID)
                .method(TransactionMethod.Image)
                .embeddedSubProcess()
                .startEvent()
                .userTask()
                .endEvent()
                .transactionDone()
                .serviceTask(SERVICE_TASK_ID)
                .endEvent()
                .done();

        Transaction transaction = modelInstance.getModelElementById(TRANSACTION_ID);
        ServiceTask serviceTask = modelInstance.getModelElementById(SERVICE_TASK_ID);
        assertThat(transaction.isFlowableExclusive()).isTrue();
        assertThat(transaction.getMethod()).isEqualTo(TransactionMethod.Image);
        assertThat(transaction.getChildElementsByType(Event.class)).hasSize(2);
        assertThat(transaction.getChildElementsByType(Task.class)).hasSize(1);
        assertThat(transaction.getFlowElements()).hasSize(5);
        assertThat(transaction.getSucceedingNodes().singleResult()).isEqualTo(serviceTask);
    }

    @Test
    public void transactionBuilderDetached() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .transaction(TRANSACTION_ID)
                .serviceTask(SERVICE_TASK_ID)
                .endEvent()
                .done();

        Transaction transaction = modelInstance.getModelElementById(TRANSACTION_ID);

        transaction.builder()
                .embeddedSubProcess()
                .startEvent()
                .userTask()
                .endEvent();

        ServiceTask serviceTask = modelInstance.getModelElementById(SERVICE_TASK_ID);
        assertThat(transaction.isFlowableExclusive()).isTrue();
        assertThat(transaction.getChildElementsByType(Event.class)).hasSize(2);
        assertThat(transaction.getChildElementsByType(Task.class)).hasSize(1);
        assertThat(transaction.getFlowElements()).hasSize(5);
        assertThat(transaction.getSucceedingNodes().singleResult()).isEqualTo(serviceTask);
    }

    @Test
    public void scriptText() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .scriptTask("script")
                .scriptText("println \"hello, world\";")
                .endEvent()
                .done();

        ScriptTask scriptTask = modelInstance.getModelElementById("script");
        assertThat(scriptTask.getScript().getTextContent()).isEqualTo("println \"hello, world\";");
    }

    @Test
    public void eventBasedGatewayAsync() {
        try {
            modelInstance = BpmnModelBuilder.createProcess()
                    .startEvent()
                    .eventBasedGateway()
                    .flowableAsync()
                    .done();

            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ex) {
            // happy path
        }

        try {
            modelInstance = BpmnModelBuilder.createProcess()
                    .startEvent()
                    .eventBasedGateway()
                    .flowableAsync(true)
                    .endEvent()
                    .done();
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ex) {
            // happy ending :D
        }
    }

    @Test
    public void messageStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent("start").message("message")
                .done();

        assertMessageEventDefinition("start", "message");
    }

    @Test
    public void messageStartEventWithExistingMessage() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent("start").message("message")
                .subProcess().triggerByEvent()
                .embeddedSubProcess()
                .startEvent("subStart").message("message")
                .subProcessDone()
                .done();

        Message message = assertMessageEventDefinition("start", "message");
        Message subMessage = assertMessageEventDefinition("subStart", "message");

        assertThat(message).isEqualTo(subMessage);

        assertOnlyOneMessageExists("message");
    }

    @Test
    public void intermediateMessageCatchEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateCatchEvent("catch").message("message")
                .done();

        assertMessageEventDefinition("catch", "message");
    }

    @Test
    public void intermediateMessageCatchEventWithExistingMessage() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateCatchEvent("catch1").message("message")
                .intermediateCatchEvent("catch2").message("message")
                .done();

        Message message1 = assertMessageEventDefinition("catch1", "message");
        Message message2 = assertMessageEventDefinition("catch2", "message");

        assertThat(message1).isEqualTo(message2);

        assertOnlyOneMessageExists("message");
    }

    @Test
    public void messageEndEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent("end").message("message")
                .done();

        assertMessageEventDefinition("end", "message");
    }

    @Test
    public void messageEventDefinitionEndEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent("end")
                .messageEventDefinition()
                .message("message")
                .done();

        assertMessageEventDefinition("end", "message");
    }

    @Test
    public void messageEndEventWithExistingMessage() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .parallelGateway()
                .endEvent("end1").message("message")
                .moveToLastGateway()
                .endEvent("end2").message("message")
                .done();

        Message message1 = assertMessageEventDefinition("end1", "message");
        Message message2 = assertMessageEventDefinition("end2", "message");

        assertThat(message1).isEqualTo(message2);

        assertOnlyOneMessageExists("message");
    }

    @Test
    public void messageEventDefinitionEndEventWithExistingMessage() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .parallelGateway()
                .endEvent("end1")
                .messageEventDefinition()
                .message("message")
                .messageEventDefinitionDone()
                .moveToLastGateway()
                .endEvent("end2")
                .messageEventDefinition()
                .message("message")
                .done();

        Message message1 = assertMessageEventDefinition("end1", "message");
        Message message2 = assertMessageEventDefinition("end2", "message");

        assertThat(message1).isEqualTo(message2);

        assertOnlyOneMessageExists("message");
    }

    @Test
    public void intermediateMessageThrowEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw").message("message")
                .done();

        assertMessageEventDefinition("throw", "message");
    }

    @Test
    public void intermediateMessageEventDefinitionThrowEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw")
                .messageEventDefinition()
                .message("message")
                .done();

        assertMessageEventDefinition("throw", "message");
    }

    @Test
    public void intermediateMessageThrowEventWithExistingMessage() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw1").message("message")
                .intermediateThrowEvent("throw2").message("message")
                .done();

        Message message1 = assertMessageEventDefinition("throw1", "message");
        Message message2 = assertMessageEventDefinition("throw2", "message");

        assertThat(message1).isEqualTo(message2);
        assertOnlyOneMessageExists("message");
    }


    @Test
    public void intermediateMessageEventDefinitionThrowEventWithExistingMessage() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw1")
                .messageEventDefinition()
                .message("message")
                .messageEventDefinitionDone()
                .intermediateThrowEvent("throw2")
                .messageEventDefinition()
                .message("message")
                .messageEventDefinitionDone()
                .done();

        Message message1 = assertMessageEventDefinition("throw1", "message");
        Message message2 = assertMessageEventDefinition("throw2", "message");

        assertThat(message1).isEqualTo(message2);
        assertOnlyOneMessageExists("message");
    }

    @Test
    public void intermediateMessageThrowEventWithMessageDefinition() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw1")
                .messageEventDefinition()
                .id("messageEventDefinition")
                .message("message")
                .flowableType("external")
                .done();

        MessageEventDefinition event = modelInstance.getModelElementById("messageEventDefinition");
        assertThat(event.getFlowableType()).isEqualTo("external");
        assertThat(event.getMessage().getName()).isEqualTo("message");
    }

    @Test
    public void intermediateMessageThrowEventWithTaskPriority() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw1")
                .messageEventDefinition("messageEventDefinition")
                .done();
    }

    @Test
    public void endEventWithTaskPriority() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent("end")
                .messageEventDefinition("messageEventDefinition")
                .done();
    }

    @Test
    public void messageEventDefinitionWithID() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw1")
                .messageEventDefinition("messageEventDefinition")
                .done();

        MessageEventDefinition event = modelInstance.getModelElementById("messageEventDefinition");
        assertThat(event).isNotNull();

        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw2")
                .messageEventDefinition().id("messageEventDefinition1")
                .done();

        // ========================================
        // ==============end event=================
        // ========================================
        event = modelInstance.getModelElementById("messageEventDefinition1");
        assertThat(event).isNotNull();
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent("end1")
                .messageEventDefinition("messageEventDefinition")
                .done();

        event = modelInstance.getModelElementById("messageEventDefinition");
        assertThat(event).isNotNull();

        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent("end2")
                .messageEventDefinition().id("messageEventDefinition1")
                .done();

        event = modelInstance.getModelElementById("messageEventDefinition1");
        assertThat(event).isNotNull();
    }

    @Test
    public void receiveTaskMessage() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .receiveTask("receive").message("message")
                .done();

        ReceiveTask receiveTask = modelInstance.getModelElementById("receive");

        Message message = receiveTask.getMessage();
        assertThat(message).isNotNull();
        assertThat(message.getName()).isEqualTo("message");
    }

    @Test
    public void receiveTaskWithExistingMessage() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .receiveTask("receive1").message("message")
                .receiveTask("receive2").message("message")
                .done();

        ReceiveTask receiveTask1 = modelInstance.getModelElementById("receive1");
        Message message1 = receiveTask1.getMessage();

        ReceiveTask receiveTask2 = modelInstance.getModelElementById("receive2");
        Message message2 = receiveTask2.getMessage();

        assertThat(message1).isEqualTo(message2);

        assertOnlyOneMessageExists("message");
    }

    @Test
    public void sendTaskMessage() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .sendTask("send").message("message")
                .done();

        SendTask sendTask = modelInstance.getModelElementById("send");

        Message message = sendTask.getMessage();
        assertThat(message).isNotNull();
        assertThat(message.getName()).isEqualTo("message");
    }

    @Test
    public void sendTaskWithExistingMessage() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .sendTask("send1").message("message")
                .sendTask("send2").message("message")
                .done();

        SendTask sendTask1 = modelInstance.getModelElementById("send1");
        Message message1 = sendTask1.getMessage();

        SendTask sendTask2 = modelInstance.getModelElementById("send2");
        Message message2 = sendTask2.getMessage();

        assertThat(message1).isEqualTo(message2);

        assertOnlyOneMessageExists("message");
    }

    @Test
    public void signalStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent("start").signal("signal")
                .done();

        assertSignalEventDefinition("start", "signal");
    }

    @Test
    public void signalStartEventWithExistingSignal() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent("start").signal("signal")
                .subProcess().triggerByEvent()
                .embeddedSubProcess()
                .startEvent("subStart").signal("signal")
                .subProcessDone()
                .done();

        Signal signal = assertSignalEventDefinition("start", "signal");
        Signal subSignal = assertSignalEventDefinition("subStart", "signal");

        assertThat(signal).isEqualTo(subSignal);

        assertOnlyOneSignalExists("signal");
    }

    @Test
    public void intermediateSignalCatchEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateCatchEvent("catch").signal("signal")
                .done();

        assertSignalEventDefinition("catch", "signal");
    }

    @Test
    public void intermediateSignalCatchEventWithExistingSignal() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateCatchEvent("catch1").signal("signal")
                .intermediateCatchEvent("catch2").signal("signal")
                .done();

        Signal signal1 = assertSignalEventDefinition("catch1", "signal");
        Signal signal2 = assertSignalEventDefinition("catch2", "signal");

        assertThat(signal1).isEqualTo(signal2);

        assertOnlyOneSignalExists("signal");
    }

    @Test
    public void signalEndEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent("end").signal("signal")
                .done();

        assertSignalEventDefinition("end", "signal");
    }

    @Test
    public void signalEndEventWithExistingSignal() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .parallelGateway()
                .endEvent("end1").signal("signal")
                .moveToLastGateway()
                .endEvent("end2").signal("signal")
                .done();

        Signal signal1 = assertSignalEventDefinition("end1", "signal");
        Signal signal2 = assertSignalEventDefinition("end2", "signal");

        assertThat(signal1).isEqualTo(signal2);

        assertOnlyOneSignalExists("signal");
    }

    @Test
    public void intermediateSignalThrowEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw").signal("signal")
                .done();

        assertSignalEventDefinition("throw", "signal");
    }

    @Test
    public void intermediateSignalThrowEventWithExistingSignal() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw1").signal("signal")
                .intermediateThrowEvent("throw2").signal("signal")
                .done();

        Signal signal1 = assertSignalEventDefinition("throw1", "signal");
        Signal signal2 = assertSignalEventDefinition("throw2", "signal");

        assertThat(signal1).isEqualTo(signal2);

        assertOnlyOneSignalExists("signal");
    }

    @Test
    public void messageBoundaryEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent()
                .moveToActivity("task") // jump back to user task and attach a boundary event
                .boundaryEvent("boundary").message("message")
                .endEvent("boundaryEnd")
                .done();

        assertMessageEventDefinition("boundary", "message");

        UserTask userTask = modelInstance.getModelElementById("task");
        BoundaryEvent boundaryEvent = modelInstance.getModelElementById("boundary");
        EndEvent boundaryEnd = modelInstance.getModelElementById("boundaryEnd");

        // boundary event is attached to the user task
        assertThat(boundaryEvent.getAttachedTo()).isEqualTo(userTask);

        // boundary event has no incoming sequence flows
        assertThat(boundaryEvent.getIncoming()).isEmpty();

        // the next flow node is the boundary end event
        List<FlowNode> succeedingNodes = boundaryEvent.getSucceedingNodes().list();
        assertThat(succeedingNodes).containsOnly(boundaryEnd);
    }

    @Test
    public void multipleBoundaryEvents() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent()
                .moveToActivity("task") // jump back to user task and attach a boundary event
                .boundaryEvent("boundary1").message("message")
                .endEvent("boundaryEnd1")
                .moveToActivity("task") // jump back to user task and attach another boundary event
                .boundaryEvent("boundary2").signal("signal")
                .endEvent("boundaryEnd2")
                .done();

        assertMessageEventDefinition("boundary1", "message");
        assertSignalEventDefinition("boundary2", "signal");

        UserTask userTask = modelInstance.getModelElementById("task");
        BoundaryEvent boundaryEvent1 = modelInstance.getModelElementById("boundary1");
        EndEvent boundaryEnd1 = modelInstance.getModelElementById("boundaryEnd1");
        BoundaryEvent boundaryEvent2 = modelInstance.getModelElementById("boundary2");
        EndEvent boundaryEnd2 = modelInstance.getModelElementById("boundaryEnd2");

        // boundary events are attached to the user task
        assertThat(boundaryEvent1.getAttachedTo()).isEqualTo(userTask);
        assertThat(boundaryEvent2.getAttachedTo()).isEqualTo(userTask);

        // boundary events have no incoming sequence flows
        assertThat(boundaryEvent1.getIncoming()).isEmpty();
        assertThat(boundaryEvent2.getIncoming()).isEmpty();

        // the next flow node is the boundary end event
        List<FlowNode> succeedingNodes = boundaryEvent1.getSucceedingNodes().list();
        assertThat(succeedingNodes).containsOnly(boundaryEnd1);
        succeedingNodes = boundaryEvent2.getSucceedingNodes().list();
        assertThat(succeedingNodes).containsOnly(boundaryEnd2);
    }

    @Test
    public void flowableTaskListenerByClassName() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableTaskListenerClass("start", "aClass")
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById("task");
        ExtensionElements extensionElements = userTask.getExtensionElements();
        Collection<FlowableTaskListener> taskListeners = extensionElements.getChildElementsByType(FlowableTaskListener.class);
        assertThat(taskListeners).hasSize(1);

        FlowableTaskListener taskListener = taskListeners.iterator().next();
        assertThat(taskListener.getFlowableClass()).isEqualTo("aClass");
        assertThat(taskListener.getFlowableEvent()).isEqualTo("start");
    }

    @Test
    public void flowableTaskListenerByClass() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableTaskListenerClass("start", this.getClass())
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById("task");
        ExtensionElements extensionElements = userTask.getExtensionElements();
        Collection<FlowableTaskListener> taskListeners = extensionElements.getChildElementsByType(FlowableTaskListener.class);
        assertThat(taskListeners).hasSize(1);

        FlowableTaskListener taskListener = taskListeners.iterator().next();
        assertThat(taskListener.getFlowableClass()).isEqualTo(this.getClass().getName());
        assertThat(taskListener.getFlowableEvent()).isEqualTo("start");
    }

    @Test
    public void flowableTaskListenerByExpression() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableTaskListenerExpression("start", "anExpression")
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById("task");
        ExtensionElements extensionElements = userTask.getExtensionElements();
        Collection<FlowableTaskListener> taskListeners = extensionElements.getChildElementsByType(FlowableTaskListener.class);
        assertThat(taskListeners).hasSize(1);

        FlowableTaskListener taskListener = taskListeners.iterator().next();
        assertThat(taskListener.getFlowableExpression()).isEqualTo("anExpression");
        assertThat(taskListener.getFlowableEvent()).isEqualTo("start");
    }

    @Test
    public void flowableTaskListenerByDelegateExpression() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableTaskListenerDelegateExpression("start", "aDelegate")
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById("task");
        ExtensionElements extensionElements = userTask.getExtensionElements();
        Collection<FlowableTaskListener> taskListeners = extensionElements.getChildElementsByType(FlowableTaskListener.class);
        assertThat(taskListeners).hasSize(1);

        FlowableTaskListener taskListener = taskListeners.iterator().next();
        assertThat(taskListener.getFlowableDelegateExpression()).isEqualTo("aDelegate");
        assertThat(taskListener.getFlowableEvent()).isEqualTo("start");
    }

    @Test
    public void flowableExecutionListenerByClassName() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableExecutionListenerClass("start", "aClass")
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById("task");
        ExtensionElements extensionElements = userTask.getExtensionElements();
        Collection<FlowableExecutionListener> executionListeners = extensionElements.getChildElementsByType(FlowableExecutionListener.class);
        assertThat(executionListeners).hasSize(1);

        FlowableExecutionListener executionListener = executionListeners.iterator().next();
        assertThat(executionListener.getFlowableClass()).isEqualTo("aClass");
        assertThat(executionListener.getFlowableEvent()).isEqualTo("start");
    }

    @Test
    public void flowableExecutionListenerByClass() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableExecutionListenerClass("start", this.getClass())
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById("task");
        ExtensionElements extensionElements = userTask.getExtensionElements();
        Collection<FlowableExecutionListener> executionListeners = extensionElements.getChildElementsByType(FlowableExecutionListener.class);
        assertThat(executionListeners).hasSize(1);

        FlowableExecutionListener executionListener = executionListeners.iterator().next();
        assertThat(executionListener.getFlowableClass()).isEqualTo(this.getClass().getName());
        assertThat(executionListener.getFlowableEvent()).isEqualTo("start");
    }

    @Test
    public void flowableExecutionListenerByExpression() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableExecutionListenerExpression("start", "anExpression")
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById("task");
        ExtensionElements extensionElements = userTask.getExtensionElements();
        Collection<FlowableExecutionListener> executionListeners = extensionElements.getChildElementsByType(FlowableExecutionListener.class);
        assertThat(executionListeners).hasSize(1);

        FlowableExecutionListener executionListener = executionListeners.iterator().next();
        assertThat(executionListener.getFlowableExpression()).isEqualTo("anExpression");
        assertThat(executionListener.getFlowableEvent()).isEqualTo("start");
    }

    @Test
    public void flowableExecutionListenerByDelegateExpression() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableExecutionListenerDelegateExpression("start", "aDelegateExpression")
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById("task");
        ExtensionElements extensionElements = userTask.getExtensionElements();
        Collection<FlowableExecutionListener> executionListeners = extensionElements.getChildElementsByType(FlowableExecutionListener.class);
        assertThat(executionListeners).hasSize(1);

        FlowableExecutionListener executionListener = executionListeners.iterator().next();
        assertThat(executionListener.getFlowableDelegateExpression()).isEqualTo("aDelegateExpression");
        assertThat(executionListener.getFlowableEvent()).isEqualTo("start");
    }

    @Test
    public void multiInstanceLoopCharacteristicsSequential() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .multiInstance()
                .sequential()
                .cardinality("card")
                .completionCondition("compl")
                .flowableCollection("coll")
                .flowableElementVariable("element")
                .multiInstanceDone()
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById("task");
        Collection<MultiInstanceLoopCharacteristics> miCharacteristics =
                userTask.getChildElementsByType(MultiInstanceLoopCharacteristics.class);

        assertThat(miCharacteristics).hasSize(1);

        MultiInstanceLoopCharacteristics miCharacteristic = miCharacteristics.iterator().next();
        assertThat(miCharacteristic.isSequential()).isTrue();
        assertThat(miCharacteristic.getLoopCardinality().getTextContent()).isEqualTo("card");
        assertThat(miCharacteristic.getCompletionCondition().getTextContent()).isEqualTo("compl");
        assertThat(miCharacteristic.getFlowableCollection()).isEqualTo("coll");
        assertThat(miCharacteristic.getFlowableElementVariable()).isEqualTo("element");

    }

    @Test
    public void multiInstanceLoopCharacteristicsParallel() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .multiInstance()
                .parallel()
                .multiInstanceDone()
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById("task");
        Collection<MultiInstanceLoopCharacteristics> miCharacteristics =
                userTask.getChildElementsByType(MultiInstanceLoopCharacteristics.class);

        assertThat(miCharacteristics).hasSize(1);

        MultiInstanceLoopCharacteristics miCharacteristic = miCharacteristics.iterator().next();
        assertThat(miCharacteristic.isSequential()).isFalse();
    }

    @Test
    public void taskWithFlowableInputOutput() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableInputParameter("foo", "bar")
                .flowableInputParameter("yoo", "hoo")
                .flowableOutputParameter("one", "two")
                .flowableOutputParameter("three", "four")
                .endEvent()
                .done();

        UserTask task = modelInstance.getModelElementById("task");
        assertFlowableInputOutputParameter(task);
    }

    @Test
    public void taskWithFlowableInputOutputWithExistingExtensionElements() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableExecutionListenerExpression("end", "${true}")
                .flowableInputParameter("foo", "bar")
                .flowableInputParameter("yoo", "hoo")
                .flowableOutputParameter("one", "two")
                .flowableOutputParameter("three", "four")
                .endEvent()
                .done();

        UserTask task = modelInstance.getModelElementById("task");
        assertFlowableInputOutputParameter(task);
    }

    @Test
    public void taskWithFlowableInputOutputWithExistingFlowableInputOutput() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .flowableInputParameter("foo", "bar")
                .flowableOutputParameter("one", "two")
                .endEvent()
                .done();

        UserTask task = modelInstance.getModelElementById("task");

        task.builder()
                .flowableInputParameter("yoo", "hoo")
                .flowableOutputParameter("three", "four");

        assertFlowableInputOutputParameter(task);
    }

    @Test
    public void subProcessWithFlowableInputOutput() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .subProcess("subProcess")
                .flowableInputParameter("foo", "bar")
                .flowableInputParameter("yoo", "hoo")
                .flowableOutputParameter("one", "two")
                .flowableOutputParameter("three", "four")
                .embeddedSubProcess()
                .startEvent()
                .endEvent()
                .subProcessDone()
                .endEvent()
                .done();

        SubProcess subProcess = modelInstance.getModelElementById("subProcess");
        assertFlowableInputOutputParameter(subProcess);
    }

    @Test
    public void subProcessWithFlowableInputOutputWithExistingExtensionElements() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .subProcess("subProcess")
                .flowableExecutionListenerExpression("end", "${true}")
                .flowableInputParameter("foo", "bar")
                .flowableInputParameter("yoo", "hoo")
                .flowableOutputParameter("one", "two")
                .flowableOutputParameter("three", "four")
                .embeddedSubProcess()
                .startEvent()
                .endEvent()
                .subProcessDone()
                .endEvent()
                .done();

        SubProcess subProcess = modelInstance.getModelElementById("subProcess");
        assertFlowableInputOutputParameter(subProcess);
    }

    @Test
    public void subProcessWithFlowableInputOutputWithExistingFlowableInputOutput() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .subProcess("subProcess")
                .flowableInputParameter("foo", "bar")
                .flowableOutputParameter("one", "two")
                .embeddedSubProcess()
                .startEvent()
                .endEvent()
                .subProcessDone()
                .endEvent()
                .done();

        SubProcess subProcess = modelInstance.getModelElementById("subProcess");

        subProcess.builder()
                .flowableInputParameter("yoo", "hoo")
                .flowableOutputParameter("three", "four");

        assertFlowableInputOutputParameter(subProcess);
    }

    @Test
    public void timerStartEventWithDate() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent("start").timerWithDate(TIMER_DATE)
                .done();

        assertTimerWithDate("start", TIMER_DATE);
    }

    @Test
    public void timerStartEventWithDuration() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent("start").timerWithDuration(TIMER_DURATION)
                .done();

        assertTimerWithDuration("start", TIMER_DURATION);
    }

    @Test
    public void timerStartEventWithCycle() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent("start").timerWithCycle(TIMER_CYCLE)
                .done();

        assertTimerWithCycle("start", TIMER_CYCLE);
    }

    @Test
    public void intermediateTimerCatchEventWithDate() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateCatchEvent("catch").timerWithDate(TIMER_DATE)
                .done();

        assertTimerWithDate("catch", TIMER_DATE);
    }

    @Test
    public void intermediateTimerCatchEventWithDuration() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateCatchEvent("catch").timerWithDuration(TIMER_DURATION)
                .done();

        assertTimerWithDuration("catch", TIMER_DURATION);
    }

    @Test
    public void intermediateTimerCatchEventWithCycle() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateCatchEvent("catch").timerWithCycle(TIMER_CYCLE)
                .done();

        assertTimerWithCycle("catch", TIMER_CYCLE);
    }

    @Test
    public void timerBoundaryEventWithDate() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent()
                .moveToActivity("task")
                .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
                .done();

        assertTimerWithDate("boundary", TIMER_DATE);
    }

    @Test
    public void timerBoundaryEventWithDuration() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent()
                .moveToActivity("task")
                .boundaryEvent("boundary").timerWithDuration(TIMER_DURATION)
                .done();

        assertTimerWithDuration("boundary", TIMER_DURATION);
    }

    @Test
    public void timerBoundaryEventWithCycle() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent()
                .moveToActivity("task")
                .boundaryEvent("boundary").timerWithCycle(TIMER_CYCLE)
                .done();

        assertTimerWithCycle("boundary", TIMER_CYCLE);
    }

    @Test
    public void notCancelingBoundaryEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask()
                .boundaryEvent("boundary").cancelActivity(false)
                .done();

        BoundaryEvent boundaryEvent = modelInstance.getModelElementById("boundary");
        assertThat(boundaryEvent.cancelActivity()).isFalse();
    }

    @Test
    public void catchAllErrorBoundaryEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent()
                .moveToActivity("task")
                .boundaryEvent("boundary").error()
                .endEvent("boundaryEnd")
                .done();

        ErrorEventDefinition errorEventDefinition = assertAndGetSingleEventDefinition("boundary", ErrorEventDefinition.class);
        assertThat(errorEventDefinition.getError()).isNull();
    }

    @Test
    public void errorBoundaryEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent()
                .moveToActivity("task")
                .boundaryEvent("boundary").error("myErrorCode")
                .endEvent("boundaryEnd")
                .done();

        assertErrorEventDefinition("boundary", "myErrorCode");

        UserTask userTask = modelInstance.getModelElementById("task");
        BoundaryEvent boundaryEvent = modelInstance.getModelElementById("boundary");
        EndEvent boundaryEnd = modelInstance.getModelElementById("boundaryEnd");

        // boundary event is attached to the user task
        assertThat(boundaryEvent.getAttachedTo()).isEqualTo(userTask);

        // boundary event has no incoming sequence flows
        assertThat(boundaryEvent.getIncoming()).isEmpty();

        // the next flow node is the boundary end event
        List<FlowNode> succeedingNodes = boundaryEvent.getSucceedingNodes().list();
        assertThat(succeedingNodes).containsOnly(boundaryEnd);
    }

    @Test
    public void errorDefinitionForBoundaryEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent()
                .moveToActivity("task")
                .boundaryEvent("boundary")
                .errorEventDefinition("event")
                .error("errorCode")
                .errorEventDefinitionDone()
                .endEvent("boundaryEnd")
                .done();

        assertErrorEventDefinition("boundary", "errorCode");
    }

    @Test
    public void errorDefinitionForBoundaryEventWithoutEventDefinitionId() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent()
                .moveToActivity("task")
                .boundaryEvent("boundary")
                .errorEventDefinition()
                .error("errorCode")
                .errorEventDefinitionDone()
                .endEvent("boundaryEnd")
                .done();

        assertErrorEventDefinition("boundary", "errorCode");
    }

    @Test
    public void errorEndEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent("end").error("myErrorCode")
                .done();

        assertErrorEventDefinition("end", "myErrorCode");
    }

    @Test
    public void errorEndEventWithExistingError() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent("end").error("myErrorCode")
                .moveToActivity("task")
                .boundaryEvent("boundary").error("myErrorCode")
                .endEvent("boundaryEnd")
                .done();

        Error boundaryError = assertErrorEventDefinition("boundary", "myErrorCode");
        Error endError = assertErrorEventDefinition("end", "myErrorCode");

        assertThat(boundaryError).isEqualTo(endError);

        assertOnlyOneErrorExists("myErrorCode");
    }

    @Test
    public void errorStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent()
                .subProcess()
                .triggerByEvent()
                .embeddedSubProcess()
                .startEvent("subProcessStart")
                .error("myErrorCode")
                .endEvent()
                .done();

        assertErrorEventDefinition("subProcessStart", "myErrorCode");
    }

    @Test
    public void catchAllErrorStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent()
                .subProcess()
                .triggerByEvent()
                .embeddedSubProcess()
                .startEvent("subProcessStart")
                .error()
                .endEvent()
                .done();

        ErrorEventDefinition errorEventDefinition = assertAndGetSingleEventDefinition("subProcessStart", ErrorEventDefinition.class);
        assertThat(errorEventDefinition.getError()).isNull();
    }

    @Test
    public void catchAllEscalationBoundaryEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent()
                .moveToActivity("task")
                .boundaryEvent("boundary").escalation()
                .endEvent("boundaryEnd")
                .done();

        EscalationEventDefinition escalationEventDefinition = assertAndGetSingleEventDefinition("boundary", EscalationEventDefinition.class);
        assertThat(escalationEventDefinition.getEscalation()).isNull();
    }

    @Test
    public void escalationBoundaryEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .subProcess("subProcess")
                .endEvent()
                .moveToActivity("subProcess")
                .boundaryEvent("boundary").escalation("myEscalationCode")
                .endEvent("boundaryEnd")
                .done();

        assertEscalationEventDefinition("boundary", "myEscalationCode");

        SubProcess subProcess = modelInstance.getModelElementById("subProcess");
        BoundaryEvent boundaryEvent = modelInstance.getModelElementById("boundary");
        EndEvent boundaryEnd = modelInstance.getModelElementById("boundaryEnd");

        // boundary event is attached to the sub process
        assertThat(boundaryEvent.getAttachedTo()).isEqualTo(subProcess);

        // boundary event has no incoming sequence flows
        assertThat(boundaryEvent.getIncoming()).isEmpty();

        // the next flow node is the boundary end event
        List<FlowNode> succeedingNodes = boundaryEvent.getSucceedingNodes().list();
        assertThat(succeedingNodes).containsOnly(boundaryEnd);
    }

    @Test
    public void escalationEndEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent("end").escalation("myEscalationCode")
                .done();

        assertEscalationEventDefinition("end", "myEscalationCode");
    }

    @Test
    public void escalationStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent()
                .subProcess()
                .triggerByEvent()
                .embeddedSubProcess()
                .startEvent("subProcessStart")
                .escalation("myEscalationCode")
                .endEvent()
                .done();

        assertEscalationEventDefinition("subProcessStart", "myEscalationCode");
    }

    @Test
    public void catchAllEscalationStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent()
                .subProcess()
                .triggerByEvent()
                .embeddedSubProcess()
                .startEvent("subProcessStart")
                .escalation()
                .endEvent()
                .done();

        EscalationEventDefinition escalationEventDefinition = assertAndGetSingleEventDefinition("subProcessStart", EscalationEventDefinition.class);
        assertThat(escalationEventDefinition.getEscalation()).isNull();
    }

    @Test
    public void intermediateEscalationThrowEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateThrowEvent("throw").escalation("myEscalationCode")
                .endEvent()
                .done();

        assertEscalationEventDefinition("throw", "myEscalationCode");
    }

    @Test
    public void escalationEndEventWithExistingEscalation() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("task")
                .endEvent("end").escalation("myEscalationCode")
                .moveToActivity("task")
                .boundaryEvent("boundary").escalation("myEscalationCode")
                .endEvent("boundaryEnd")
                .done();

        Escalation boundaryEscalation = assertEscalationEventDefinition("boundary", "myEscalationCode");
        Escalation endEscalation = assertEscalationEventDefinition("end", "myEscalationCode");

        assertThat(boundaryEscalation).isEqualTo(endEscalation);

        assertOnlyOneEscalationExists("myEscalationCode");

    }

    @Test
    public void compensationStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent()
                .subProcess()
                .triggerByEvent()
                .embeddedSubProcess()
                .startEvent("subProcessStart")
                .compensation()
                .endEvent()
                .done();

        assertCompensationEventDefinition("subProcessStart");
    }

    @Test
    public void interruptingStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent()
                .subProcess()
                .triggerByEvent()
                .embeddedSubProcess()
                .startEvent("subProcessStart")
                .interrupting(true)
                .error()
                .endEvent()
                .done();

        StartEvent startEvent = modelInstance.getModelElementById("subProcessStart");
        assertThat(startEvent).isNotNull();
        assertThat(startEvent.isInterrupting()).isTrue();
    }

    @Test
    public void nonInterruptingStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .endEvent()
                .subProcess()
                .triggerByEvent()
                .embeddedSubProcess()
                .startEvent("subProcessStart")
                .interrupting(false)
                .error()
                .endEvent()
                .done();

        StartEvent startEvent = modelInstance.getModelElementById("subProcessStart");
        assertThat(startEvent).isNotNull();
        assertThat(startEvent.isInterrupting()).isFalse();
    }

    @Test
    public void userTaskFlowableFormField() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask(TASK_ID)
                .flowableFormField()
                .flowableId("myFormField_1")
                .flowableType("string")
                .flowableFormFieldDone()
                .flowableFormField()
                .flowableId("myFormField_2")
                .flowableType("integer")
                .flowableFormFieldDone()
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById(TASK_ID);
        assertFlowableFormField(userTask);
    }

    @Test
    public void userTaskFlowableFormFieldWithExistingFlowableFormData() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask(TASK_ID)
                .flowableFormField()
                .flowableId("myFormField_1")
                .flowableType("string")
                .flowableFormFieldDone()
                .endEvent()
                .done();

        UserTask userTask = modelInstance.getModelElementById(TASK_ID);

        userTask.builder()
                .flowableFormField()
                .flowableId("myFormField_2")
                .flowableType("integer")
                .flowableFormFieldDone();

        assertFlowableFormField(userTask);
    }

    @Test
    public void startEventFlowableFormField() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent(START_EVENT_ID)
                .flowableFormField()
                .flowableId("myFormField_1")
                .flowableType("string")
                .flowableFormFieldDone()
                .flowableFormField()
                .flowableId("myFormField_2")
                .flowableType("integer")
                .flowableFormFieldDone()
                .endEvent()
                .done();

        StartEvent startEvent = modelInstance.getModelElementById(START_EVENT_ID);
        assertFlowableFormField(startEvent);
    }

    @Test
    public void compensateEventDefinitionCatchStartEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent("start")
                .compensateEventDefinition()
                .waitForCompletion(false)
                .compensateEventDefinitionDone()
                .userTask("userTask")
                .endEvent("end")
                .done();

        CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("start", CompensateEventDefinition.class);
        Activity activity = eventDefinition.getActivity();
        assertThat(activity).isNull();
        assertThat(eventDefinition.isWaitForCompletion()).isFalse();
    }


    @Test
    public void compensateEventDefinitionCatchBoundaryEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("userTask")
                .boundaryEvent("catch")
                .compensateEventDefinition()
                .waitForCompletion(false)
                .compensateEventDefinitionDone()
                .endEvent("end")
                .done();

        CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("catch", CompensateEventDefinition.class);
        Activity activity = eventDefinition.getActivity();
        assertThat(activity).isNull();
        assertThat(eventDefinition.isWaitForCompletion()).isFalse();
    }

    @Test
    public void compensateEventDefinitionCatchBoundaryEventWithId() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("userTask")
                .boundaryEvent("catch")
                .compensateEventDefinition("foo")
                .waitForCompletion(false)
                .compensateEventDefinitionDone()
                .endEvent("end")
                .done();

        CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("catch", CompensateEventDefinition.class);
        assertThat(eventDefinition.getId()).isEqualTo("foo");
    }

    @Test
    public void compensateEventDefinitionThrowEndEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("userTask")
                .endEvent("end")
                .compensateEventDefinition()
                .activityRef("userTask")
                .waitForCompletion(true)
                .compensateEventDefinitionDone()
                .done();

        CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("end", CompensateEventDefinition.class);
        Activity activity = eventDefinition.getActivity();
        assertThat(activity).isEqualTo(modelInstance.getModelElementById("userTask"));
        assertThat(eventDefinition.isWaitForCompletion()).isTrue();
    }

    @Test
    public void compensateEventDefinitionThrowIntermediateEvent() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("userTask")
                .intermediateThrowEvent("throw")
                .compensateEventDefinition()
                .activityRef("userTask")
                .waitForCompletion(true)
                .compensateEventDefinitionDone()
                .endEvent("end")
                .done();

        CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("throw", CompensateEventDefinition.class);
        Activity activity = eventDefinition.getActivity();
        assertThat(activity).isEqualTo(modelInstance.getModelElementById("userTask"));
        assertThat(eventDefinition.isWaitForCompletion()).isTrue();
    }

    @Test
    public void compensateEventDefinitionThrowIntermediateEventWithId() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("userTask")
                .intermediateCatchEvent("throw")
                .compensateEventDefinition("foo")
                .activityRef("userTask")
                .waitForCompletion(true)
                .compensateEventDefinitionDone()
                .endEvent("end")
                .done();

        CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("throw", CompensateEventDefinition.class);
        assertThat(eventDefinition.getId()).isEqualTo("foo");
    }

    @Test
    public void compensateEventDefinitionReferencesNonExistingActivity() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("userTask")
                .endEvent("end")
                .done();

        UserTask userTask = modelInstance.getModelElementById("userTask");
        UserTaskBuilder userTaskBuilder = userTask.builder();

        try {
            userTaskBuilder
                    .boundaryEvent()
                    .compensateEventDefinition()
                    .activityRef("nonExistingTask")
                    .done();
            fail("should fail");
        }
        catch (BpmnModelException e) {
            assertThat(e).hasMessageContaining("Activity with id 'nonExistingTask' does not exist");
        }
    }

    @Test
    public void compensateEventDefinitionReferencesActivityInDifferentScope() {
        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("userTask")
                .subProcess()
                .embeddedSubProcess()
                .startEvent()
                .userTask("subProcessTask")
                .endEvent()
                .subProcessDone()
                .endEvent("end")
                .done();

        UserTask userTask = modelInstance.getModelElementById("userTask");
        UserTaskBuilder userTaskBuilder = userTask.builder();

        try {
            userTaskBuilder
                    .boundaryEvent("boundary")
                    .compensateEventDefinition()
                    .activityRef("subProcessTask")
                    .done();
            fail("should fail");
        }
        catch (BpmnModelException e) {
            assertThat(e).hasMessageContaining("Activity with id 'subProcessTask' must be in the same scope as 'boundary'");
        }
    }

    @Test
    public void intermediateConditionalEventDefinition() {

        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateCatchEvent(CATCH_ID)
                .conditionalEventDefinition(CONDITION_ID)
                .condition(TEST_CONDITION)
                .conditionalEventDefinitionDone()
                .endEvent()
                .done();

        ConditionalEventDefinition eventDefinition = assertAndGetSingleEventDefinition(CATCH_ID, ConditionalEventDefinition.class);
        assertThat(eventDefinition.getId()).isEqualTo(CONDITION_ID);
        assertThat(eventDefinition.getCondition().getTextContent()).isEqualTo(TEST_CONDITION);
    }

    @Test
    public void intermediateConditionalEventDefinitionShortCut() {

        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .intermediateCatchEvent(CATCH_ID)
                .condition(TEST_CONDITION)
                .endEvent()
                .done();

        ConditionalEventDefinition eventDefinition = assertAndGetSingleEventDefinition(CATCH_ID, ConditionalEventDefinition.class);
        assertThat(eventDefinition.getCondition().getTextContent()).isEqualTo(TEST_CONDITION);
    }

    @Test
    public void boundaryConditionalEventDefinition() {

        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask(USER_TASK_ID)
                .endEvent()
                .moveToActivity(USER_TASK_ID)
                .boundaryEvent(BOUNDARY_ID)
                .conditionalEventDefinition(CONDITION_ID)
                .condition(TEST_CONDITION)
                .conditionalEventDefinitionDone()
                .endEvent()
                .done();

        ConditionalEventDefinition eventDefinition = assertAndGetSingleEventDefinition(BOUNDARY_ID, ConditionalEventDefinition.class);
        assertThat(eventDefinition.getId()).isEqualTo(CONDITION_ID);
        assertThat(eventDefinition.getCondition().getTextContent()).isEqualTo(TEST_CONDITION);
    }

    @Test
    public void eventSubProcessConditionalStartEvent() {

        modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask()
                .endEvent()
                .subProcess()
                .triggerByEvent()
                .embeddedSubProcess()
                .startEvent(START_EVENT_ID)
                .conditionalEventDefinition(CONDITION_ID)
                .condition(TEST_CONDITION)
                .conditionalEventDefinitionDone()
                .endEvent()
                .done();

        ConditionalEventDefinition eventDefinition = assertAndGetSingleEventDefinition(START_EVENT_ID, ConditionalEventDefinition.class);
        assertThat(eventDefinition.getId()).isEqualTo(CONDITION_ID);
        assertThat(eventDefinition.getCondition().getTextContent()).isEqualTo(TEST_CONDITION);
    }

    protected Message assertMessageEventDefinition(String elementId, String messageName) {
        MessageEventDefinition messageEventDefinition = assertAndGetSingleEventDefinition(elementId, MessageEventDefinition.class);
        Message message = messageEventDefinition.getMessage();
        assertThat(message).isNotNull();
        assertThat(message.getName()).isEqualTo(messageName);

        return message;
    }

    protected void assertOnlyOneMessageExists(String messageName) {
        Collection<Message> messages = modelInstance.getModelElementsByType(Message.class);
        assertThat(messages).extracting("name").containsOnlyOnce(messageName);
    }

    protected Signal assertSignalEventDefinition(String elementId, String signalName) {
        SignalEventDefinition signalEventDefinition = assertAndGetSingleEventDefinition(elementId, SignalEventDefinition.class);
        Signal signal = signalEventDefinition.getSignal();
        assertThat(signal).isNotNull();
        assertThat(signal.getName()).isEqualTo(signalName);

        return signal;
    }

    protected void assertOnlyOneSignalExists(String signalName) {
        Collection<Signal> signals = modelInstance.getModelElementsByType(Signal.class);
        assertThat(signals).extracting("name").containsOnlyOnce(signalName);
    }

    protected Error assertErrorEventDefinition(String elementId, String errorCode) {
        ErrorEventDefinition errorEventDefinition = assertAndGetSingleEventDefinition(elementId, ErrorEventDefinition.class);
        Error error = errorEventDefinition.getError();
        assertThat(error).isNotNull();
        assertThat(error.getErrorCode()).isEqualTo(errorCode);

        return error;
    }

    protected void assertOnlyOneErrorExists(String errorCode) {
        Collection<Error> errors = modelInstance.getModelElementsByType(Error.class);
        assertThat(errors).extracting("errorCode").containsOnlyOnce(errorCode);
    }

    protected Escalation assertEscalationEventDefinition(String elementId, String escalationCode) {
        EscalationEventDefinition escalationEventDefinition = assertAndGetSingleEventDefinition(elementId, EscalationEventDefinition.class);
        Escalation escalation = escalationEventDefinition.getEscalation();
        assertThat(escalation).isNotNull();
        assertThat(escalation.getEscalationCode()).isEqualTo(escalationCode);

        return escalation;
    }

    protected void assertOnlyOneEscalationExists(String escalationCode) {
        Collection<Escalation> escalations = modelInstance.getModelElementsByType(Escalation.class);
        assertThat(escalations).extracting("escalationCode").containsOnlyOnce(escalationCode);
    }

    protected void assertCompensationEventDefinition(String elementId) {
        assertAndGetSingleEventDefinition(elementId, CompensateEventDefinition.class);
    }

    protected void assertFlowableInputOutputParameter(BaseElement element) {
        FlowableInputOutput flowableInputOutput = element.getExtensionElements().getElementsQuery().filterByType(FlowableInputOutput.class).singleResult();
        assertThat(flowableInputOutput).isNotNull();

        List<FlowableInputParameter> flowableInputParameters = new ArrayList<>(flowableInputOutput.getFlowableInputParameters());
        assertThat(flowableInputParameters).hasSize(2);

        FlowableInputParameter flowableInputParameter = flowableInputParameters.get(0);
        assertThat(flowableInputParameter.getFlowableName()).isEqualTo("foo");
        assertThat(flowableInputParameter.getTextContent()).isEqualTo("bar");

        flowableInputParameter = flowableInputParameters.get(1);
        assertThat(flowableInputParameter.getFlowableName()).isEqualTo("yoo");
        assertThat(flowableInputParameter.getTextContent()).isEqualTo("hoo");

        List<FlowableOutputParameter> flowableOutputParameters = new ArrayList<>(flowableInputOutput.getFlowableOutputParameters());
        assertThat(flowableOutputParameters).hasSize(2);

        FlowableOutputParameter flowableOutputParameter = flowableOutputParameters.get(0);
        assertThat(flowableOutputParameter.getFlowableName()).isEqualTo("one");
        assertThat(flowableOutputParameter.getTextContent()).isEqualTo("two");

        flowableOutputParameter = flowableOutputParameters.get(1);
        assertThat(flowableOutputParameter.getFlowableName()).isEqualTo("three");
        assertThat(flowableOutputParameter.getTextContent()).isEqualTo("four");
    }

    protected void assertTimerWithDate(String elementId, String timerDate) {
        TimerEventDefinition timerEventDefinition = assertAndGetSingleEventDefinition(elementId, TimerEventDefinition.class);
        TimeDate timeDate = timerEventDefinition.getTimeDate();
        assertThat(timeDate).isNotNull();
        assertThat(timeDate.getTextContent()).isEqualTo(timerDate);
    }

    protected void assertTimerWithDuration(String elementId, String timerDuration) {
        TimerEventDefinition timerEventDefinition = assertAndGetSingleEventDefinition(elementId, TimerEventDefinition.class);
        TimeDuration timeDuration = timerEventDefinition.getTimeDuration();
        assertThat(timeDuration).isNotNull();
        assertThat(timeDuration.getTextContent()).isEqualTo(timerDuration);
    }

    protected void assertTimerWithCycle(String elementId, String timerCycle) {
        TimerEventDefinition timerEventDefinition = assertAndGetSingleEventDefinition(elementId, TimerEventDefinition.class);
        TimeCycle timeCycle = timerEventDefinition.getTimeCycle();
        assertThat(timeCycle).isNotNull();
        assertThat(timeCycle.getTextContent()).isEqualTo(timerCycle);
    }

    @SuppressWarnings("unchecked")
    protected <T extends EventDefinition> T assertAndGetSingleEventDefinition(String elementId, Class<T> eventDefinitionType) {
        BpmnModelElementInstance element = modelInstance.getModelElementById(elementId);
        assertThat(element).isNotNull();
        Collection<EventDefinition> eventDefinitions = element.getChildElementsByType(EventDefinition.class);
        assertThat(eventDefinitions).hasSize(1);

        EventDefinition eventDefinition = eventDefinitions.iterator().next();
        assertThat(eventDefinition)
                .isNotNull()
                .isInstanceOf(eventDefinitionType);
        return (T) eventDefinition;
    }

    protected void assertFlowableFormField(BaseElement element) {
        assertThat(element.getExtensionElements()).isNotNull();

        FlowableFormData flowableFormData = element.getExtensionElements().getElementsQuery().filterByType(FlowableFormData.class).singleResult();
        assertThat(flowableFormData).isNotNull();

        List<FlowableFormField> flowableFormFields = new ArrayList<>(flowableFormData.getFlowableFormFields());
        assertThat(flowableFormFields).hasSize(2);

        FlowableFormField flowableFormField = flowableFormFields.get(0);
        assertThat(flowableFormField.getFlowableId()).isEqualTo("myFormField_1");
        assertThat(flowableFormField.getFlowableType()).isEqualTo("string");

        flowableFormField = flowableFormFields.get(1);
        assertThat(flowableFormField.getFlowableId()).isEqualTo("myFormField_2");
        assertThat(flowableFormField.getFlowableType()).isEqualTo("integer");
    }

    protected void assertFlowableFailedJobRetryTimeCycle(BaseElement element) {
        assertThat(element.getExtensionElements()).isNotNull();

        FlowableFailedJobRetryTimeCycle flowableFailedJobRetryTimeCycle =
                element.getExtensionElements().getElementsQuery().filterByType(FlowableFailedJobRetryTimeCycle.class).singleResult();
        assertThat(flowableFailedJobRetryTimeCycle).isNotNull();
        assertThat(flowableFailedJobRetryTimeCycle.getTextContent()).isEqualTo(FAILED_JOB_RETRY_TIME_CYCLE);
    }
}
