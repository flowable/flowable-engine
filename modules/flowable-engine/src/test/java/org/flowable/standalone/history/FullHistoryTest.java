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

package org.flowable.standalone.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricFormProperty;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricVariableUpdate;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.api.runtime.DummySerializable;
import org.flowable.engine.test.history.SerializableVariable;
import org.flowable.standalone.jpa.FieldAccessJPAEntity;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.flowable.variable.service.impl.types.EntityManagerSession;
import org.flowable.variable.service.impl.types.EntityManagerSessionFactory;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Christian Lipphardt (camunda)
 */
public class FullHistoryTest extends ResourceFlowableTestCase {

    public FullHistoryTest() {
        super("org/flowable/standalone/history/fullhistory.flowable.cfg.xml");
    }

    @Test
    @Deployment
    public void testVariableUpdates() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("number", "one");
        variables.put("character", "a");
        variables.put("bytes", ":-(".getBytes());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("receiveTask", variables);
        runtimeService.setVariable(processInstance.getId(), "number", "two");
        runtimeService.setVariable(processInstance.getId(), "bytes", ":-)".getBytes());

        // Start-task should be added to history
        HistoricActivityInstance historicStartEvent = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId())
                .activityId("theStart").singleResult();
        assertThat(historicStartEvent).isNotNull();
        assertActivityInstancesAreSame(historicStartEvent,
                runtimeService.createActivityInstanceQuery().activityInstanceId(historicStartEvent.getId()).singleResult());

        HistoricActivityInstance waitStateActivity = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId())
                .activityId("waitState").singleResult();
        assertActivityInstancesAreSame(waitStateActivity,
                runtimeService.createActivityInstanceQuery().activityInstanceId(waitStateActivity.getId()).singleResult());
        assertThat(waitStateActivity).isNotNull();

        HistoricActivityInstance serviceTaskActivity = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId())
                .activityId("serviceTask").singleResult();
        assertActivityInstancesAreSame(serviceTaskActivity,
                runtimeService.createActivityInstanceQuery().activityInstanceId(serviceTaskActivity.getId()).singleResult());
        assertThat(serviceTaskActivity).isNotNull();

        List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().orderByVariableName().asc().orderByVariableRevision().asc().list();

        assertThat(historicDetails).hasSize(10);

        HistoricVariableUpdate historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(0);
        assertThat(historicVariableUpdate.getVariableName()).isEqualTo("bytes");
        assertThat(new String((byte[]) historicVariableUpdate.getValue())).isEqualTo(":-(");
        assertThat(historicVariableUpdate.getRevision()).isZero();

        // Flowable 6: we don't store the start event activityId anymore!
        // assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(historicStartEvent.getId());
        assertThat(historicVariableUpdate.getActivityInstanceId()).isNull();

        // Variable is updated when process was in waitstate
        historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(1);
        assertThat(historicVariableUpdate.getVariableName()).isEqualTo("bytes");
        assertThat(new String((byte[]) historicVariableUpdate.getValue())).isEqualTo(":-)");
        assertThat(historicVariableUpdate.getRevision()).isEqualTo(1);

        // assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(waitStateActivity.getId());
        assertThat(historicVariableUpdate.getActivityInstanceId()).isNull();

        historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(2);
        assertThat(historicVariableUpdate.getVariableName()).isEqualTo("character");
        assertThat(historicVariableUpdate.getValue()).isEqualTo("a");
        assertThat(historicVariableUpdate.getRevision()).isZero();

        // assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(historicStartEvent.getId());
        assertThat(historicVariableUpdate.getActivityInstanceId()).isNull();

        historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(3);
        assertThat(historicVariableUpdate.getVariableName()).isEqualTo("number");
        assertThat(historicVariableUpdate.getValue()).isEqualTo("one");
        assertThat(historicVariableUpdate.getRevision()).isZero();

        // assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(historicStartEvent.getId());
        assertThat(historicVariableUpdate.getActivityInstanceId()).isNull();

        // Variable is updated when process was in waitstate
        historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(4);
        assertThat(historicVariableUpdate.getVariableName()).isEqualTo("number");
        assertThat(historicVariableUpdate.getValue()).isEqualTo("two");
        assertThat(historicVariableUpdate.getRevision()).isEqualTo(1);

        // assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(waitStateActivity.getId());
        assertThat(historicVariableUpdate.getActivityInstanceId()).isNull();

        // Variable set from process-start execution listener
        historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(5);
        assertThat(historicVariableUpdate.getVariableName()).isEqualTo("zVar1");
        assertThat(historicVariableUpdate.getValue()).isEqualTo("Event: start");
        assertThat(historicVariableUpdate.getRevision()).isZero();
        assertThat(historicVariableUpdate.getActivityInstanceId()).isNull();

        // Variable set from transition take execution listener
        historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(6);
        assertThat(historicVariableUpdate.getVariableName()).isEqualTo("zVar2");
        assertThat(historicVariableUpdate.getValue()).isEqualTo("Event: take");
        assertThat(historicVariableUpdate.getRevision()).isZero();
        assertThat(historicVariableUpdate.getActivityInstanceId()).isNull();

        // Variable set from activity start execution listener on the servicetask
        historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(7);
        assertThat(historicVariableUpdate.getVariableName()).isEqualTo("zVar3");
        assertThat(historicVariableUpdate.getValue()).isEqualTo("Event: start");
        assertThat(historicVariableUpdate.getRevision()).isZero();
        assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(serviceTaskActivity.getId());

        // Variable set from activity end execution listener on the servicetask
        historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(8);
        assertThat(historicVariableUpdate.getVariableName()).isEqualTo("zVar4");
        assertThat(historicVariableUpdate.getValue()).isEqualTo("Event: end");
        assertThat(historicVariableUpdate.getRevision()).isZero();
        assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(serviceTaskActivity.getId());

        // Variable set from service-task
        historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(9);
        assertThat(historicVariableUpdate.getVariableName()).isEqualTo("zzz");
        assertThat(historicVariableUpdate.getValue()).isEqualTo(123456789L);
        assertThat(historicVariableUpdate.getRevision()).isZero();
        assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(serviceTaskActivity.getId());

        // trigger receive task
        runtimeService.trigger(runtimeService.createExecutionQuery().activityId("waitState").singleResult().getId());
        assertProcessEnded(processInstance.getId());

        // check for historic process variables set
        HistoricVariableInstanceQuery historicProcessVariableQuery = historyService.createHistoricVariableInstanceQuery().orderByVariableName().asc();

        assertThat(historicProcessVariableQuery.count()).isEqualTo(8);

        List<HistoricVariableInstance> historicVariables = historicProcessVariableQuery.list();

        // Variable status when process is finished
        HistoricVariableInstance historicVariable = historicVariables.get(0);
        assertThat(historicVariable.getVariableName()).isEqualTo("bytes");
        assertThat(new String((byte[]) historicVariable.getValue())).isEqualTo(":-)");
        assertThat(historicVariable.getCreateTime()).isNotNull();
        assertThat(historicVariable.getLastUpdatedTime()).isNotNull();

        historicVariable = historicVariables.get(1);
        assertThat(historicVariable.getVariableName()).isEqualTo("character");
        assertThat(historicVariable.getValue()).isEqualTo("a");
        assertThat(historicVariable.getCreateTime()).isNotNull();
        assertThat(historicVariable.getLastUpdatedTime()).isNotNull();

        historicVariable = historicVariables.get(2);
        assertThat(historicVariable.getVariableName()).isEqualTo("number");
        assertThat(historicVariable.getValue()).isEqualTo("two");
        assertThat(historicVariable.getCreateTime()).isNotNull();
        assertThat(historicVariable.getLastUpdatedTime()).isNotSameAs(historicVariable.getCreateTime());

        historicVariable = historicVariables.get(3);
        assertThat(historicVariable.getVariableName()).isEqualTo("zVar1");
        assertThat(historicVariable.getValue()).isEqualTo("Event: start");
        assertThat(historicVariable.getCreateTime()).isNotNull();
        assertThat(historicVariable.getLastUpdatedTime()).isNotNull();

        historicVariable = historicVariables.get(4);
        assertThat(historicVariable.getVariableName()).isEqualTo("zVar2");
        assertThat(historicVariable.getValue()).isEqualTo("Event: take");
        assertThat(historicVariable.getCreateTime()).isNotNull();
        assertThat(historicVariable.getLastUpdatedTime()).isNotNull();

        historicVariable = historicVariables.get(5);
        assertThat(historicVariable.getVariableName()).isEqualTo("zVar3");
        assertThat(historicVariable.getValue()).isEqualTo("Event: start");
        assertThat(historicVariable.getCreateTime()).isNotNull();
        assertThat(historicVariable.getLastUpdatedTime()).isNotNull();

        historicVariable = historicVariables.get(6);
        assertThat(historicVariable.getVariableName()).isEqualTo("zVar4");
        assertThat(historicVariable.getValue()).isEqualTo("Event: end");
        assertThat(historicVariable.getCreateTime()).isNotNull();
        assertThat(historicVariable.getLastUpdatedTime()).isNotNull();

        historicVariable = historicVariables.get(7);
        assertThat(historicVariable.getVariableName()).isEqualTo("zzz");
        assertThat(historicVariable.getValue()).isEqualTo(123456789L);
        assertThat(historicVariable.getCreateTime()).isNotNull();
        assertThat(historicVariable.getLastUpdatedTime()).isNotNull();

        historicVariable = historyService.createHistoricVariableInstanceQuery().variableValueLike("number", "tw%").singleResult();
        assertThat(historicVariable).isNotNull();
        assertThat(historicVariable.getVariableName()).isEqualTo("number");
        assertThat(historicVariable.getValue()).isEqualTo("two");

        historicVariable = historyService.createHistoricVariableInstanceQuery().variableValueLikeIgnoreCase("number", "TW%").singleResult();
        assertThat(historicVariable).isNotNull();
        assertThat(historicVariable.getVariableName()).isEqualTo("number");
        assertThat(historicVariable.getValue()).isEqualTo("two");

        historicVariable = historyService.createHistoricVariableInstanceQuery().variableValueLikeIgnoreCase("number", "TW2%").singleResult();
        assertThat(historicVariable).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricVariableInstanceQueryTaskVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("variable", "setFromProcess");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(1);

        org.flowable.task.api.Task activeTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(activeTask).isNotNull();
        taskService.setVariableLocal(activeTask.getId(), "variable", "setFromTask");

        // Check if additional variable is available in history, task-local
        assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);
        assertThat(historyService.createHistoricVariableInstanceQuery().taskId(activeTask.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricVariableInstanceQuery().taskId(activeTask.getId()).singleResult().getValue()).isEqualTo("setFromTask");
        assertThat(historyService.createHistoricVariableInstanceQuery().taskId(activeTask.getId()).singleResult().getTaskId()).isEqualTo(activeTask.getId());
        assertThat(historyService.createHistoricVariableInstanceQuery().excludeTaskVariables().count()).isEqualTo(1);

        // Test null task-id
        assertThatThrownBy(() -> historyService.createHistoricVariableInstanceQuery().taskId(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");

        // Test invalid usage of taskId together with excludeTaskVariables
        assertThatThrownBy(() -> historyService.createHistoricVariableInstanceQuery().taskId("123").excludeTaskVariables().singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Cannot use taskId together with excludeTaskVariables");

        assertThatThrownBy(() -> historyService.createHistoricVariableInstanceQuery().excludeTaskVariables().taskId("123").singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Cannot use taskId together with excludeTaskVariables");
    }

    @Test
    @Deployment(resources = "org/flowable/standalone/history/FullHistoryTest.testVariableUpdates.bpmn20.xml")
    public void testHistoricVariableInstanceQuery() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("process", "one");
        runtimeService.startProcessInstanceByKey("receiveTask", variables);
        runtimeService.trigger(runtimeService.createExecutionQuery().activityId("waitState").singleResult().getId());

        assertThat(historyService.createHistoricVariableInstanceQuery().variableName("process").count()).isEqualTo(1);
        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("process", "one").count()).isEqualTo(1);

        Map<String, Object> variables2 = new HashMap<>();
        variables2.put("process", "two");
        runtimeService.startProcessInstanceByKey("receiveTask", variables2);
        runtimeService.trigger(runtimeService.createExecutionQuery().activityId("waitState").singleResult().getId());

        assertThat(historyService.createHistoricVariableInstanceQuery().variableName("process").count()).isEqualTo(2);
        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("process", "one").count()).isEqualTo(1);
        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("process", "two").count()).isEqualTo(1);

        HistoricVariableInstance historicProcessVariable = historyService.createHistoricVariableInstanceQuery().variableValueEquals("process", "one")
                .singleResult();
        assertThat(historicProcessVariable.getVariableName()).isEqualTo("process");
        assertThat(historicProcessVariable.getValue()).isEqualTo("one");

        Map<String, Object> variables3 = new HashMap<>();
        variables3.put("long", 1000L);
        variables3.put("double", 25.43d);
        runtimeService.startProcessInstanceByKey("receiveTask", variables3);
        runtimeService.trigger(runtimeService.createExecutionQuery().activityId("waitState").singleResult().getId());

        assertThat(historyService.createHistoricVariableInstanceQuery().variableName("long").count()).isEqualTo(1);
        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("long", 1000L).count()).isEqualTo(1);
        assertThat(historyService.createHistoricVariableInstanceQuery().variableName("double").count()).isEqualTo(1);
        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("double", 25.43d).count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testHistoricVariableUpdatesAllTypes() throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss SSS");
        Map<String, Object> variables = new HashMap<>();
        variables.put("aVariable", "initial value");

        Date startedDate = sdf.parse("01/01/2001 01:23:45 000");

        // In the javaDelegate, the current time is manipulated
        Date updatedDate = sdf.parse("01/01/2001 01:23:46 000");

        processEngineConfiguration.getClock().setCurrentTime(startedDate);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricVariableUpdateProcess", variables);

        List<HistoricDetail> details = historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId())
                .orderByVariableName().asc().orderByTime().asc().list();

        // 8 variable updates should be present, one performed when starting
        // process
        // the other 7 are set in VariableSetter serviceTask
        assertThat(details).hasSize(9);

        // Since we order by varName, first entry should be aVariable update
        // from startTask
        HistoricVariableUpdate startVarUpdate = (HistoricVariableUpdate) details.get(0);
        assertThat(startVarUpdate.getVariableName()).isEqualTo("aVariable");
        assertThat(startVarUpdate.getValue()).isEqualTo("initial value");
        assertThat(startVarUpdate.getRevision()).isZero();
        assertThat(startVarUpdate.getProcessInstanceId()).isEqualTo(processInstance.getId());
        // Date should the one set when starting
        assertThat(startVarUpdate.getTime()).isEqualTo(startedDate);

        HistoricVariableUpdate updatedStringVariable = (HistoricVariableUpdate) details.get(1);
        assertThat(updatedStringVariable.getVariableName()).isEqualTo("aVariable");
        assertThat(updatedStringVariable.getValue()).isEqualTo("updated value");
        assertThat(updatedStringVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
        // Date should be the updated date
        assertThat(updatedStringVariable.getTime()).isEqualTo(updatedDate);

        HistoricVariableUpdate intVariable = (HistoricVariableUpdate) details.get(2);
        assertThat(intVariable.getVariableName()).isEqualTo("bVariable");
        assertThat(intVariable.getValue()).isEqualTo(123);
        assertThat(intVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(intVariable.getTime()).isEqualTo(updatedDate);

        HistoricVariableUpdate longVariable = (HistoricVariableUpdate) details.get(3);
        assertThat(longVariable.getVariableName()).isEqualTo("cVariable");
        assertThat(longVariable.getValue()).isEqualTo(12345L);
        assertThat(longVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(longVariable.getTime()).isEqualTo(updatedDate);

        HistoricVariableUpdate doubleVariable = (HistoricVariableUpdate) details.get(4);
        assertThat(doubleVariable.getVariableName()).isEqualTo("dVariable");
        assertThat(doubleVariable.getValue()).isEqualTo(1234.567);
        assertThat(doubleVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(doubleVariable.getTime()).isEqualTo(updatedDate);

        HistoricVariableUpdate shortVariable = (HistoricVariableUpdate) details.get(5);
        assertThat(shortVariable.getVariableName()).isEqualTo("eVariable");
        assertThat(shortVariable.getValue()).isEqualTo((short) 12);
        assertThat(shortVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(shortVariable.getTime()).isEqualTo(updatedDate);

        HistoricVariableUpdate dateVariable = (HistoricVariableUpdate) details.get(6);
        assertThat(dateVariable.getVariableName()).isEqualTo("fVariable");
        assertThat(dateVariable.getValue()).isEqualTo(sdf.parse("01/01/2001 01:23:45 678"));
        assertThat(dateVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dateVariable.getTime()).isEqualTo(updatedDate);

        HistoricVariableUpdate serializableVariable = (HistoricVariableUpdate) details.get(7);
        assertThat(serializableVariable.getVariableName()).isEqualTo("gVariable");
        assertThat(serializableVariable.getValue()).isEqualTo(new SerializableVariable("hello hello"));
        assertThat(serializableVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(serializableVariable.getTime()).isEqualTo(updatedDate);

        HistoricVariableUpdate byteArrayVariable = (HistoricVariableUpdate) details.get(8);
        assertThat(byteArrayVariable.getVariableName()).isEqualTo("hVariable");
        assertThat(new String((byte[]) byteArrayVariable.getValue())).isEqualTo(";-)");
        assertThat(byteArrayVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(byteArrayVariable.getTime()).isEqualTo(updatedDate);

        // end process instance
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());

        // check for historic process variables set
        HistoricVariableInstanceQuery historicProcessVariableQuery = historyService.createHistoricVariableInstanceQuery().orderByVariableName().asc();

        assertThat(historicProcessVariableQuery.count()).isEqualTo(8);

        List<HistoricVariableInstance> historicVariables = historicProcessVariableQuery.list();

        // Variable status when process is finished
        HistoricVariableInstance historicVariable = historicVariables.get(0);
        assertThat(historicVariable.getVariableName()).isEqualTo("aVariable");
        assertThat(historicVariable.getValue()).isEqualTo("updated value");
        assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

        historicVariable = historicVariables.get(1);
        assertThat(historicVariable.getVariableName()).isEqualTo("bVariable");
        assertThat(historicVariable.getValue()).isEqualTo(123);
        assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

        historicVariable = historicVariables.get(2);
        assertThat(historicVariable.getVariableName()).isEqualTo("cVariable");
        assertThat(historicVariable.getValue()).isEqualTo(12345L);
        assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

        historicVariable = historicVariables.get(3);
        assertThat(historicVariable.getVariableName()).isEqualTo("dVariable");
        assertThat(historicVariable.getValue()).isEqualTo(1234.567);
        assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

        historicVariable = historicVariables.get(4);
        assertThat(historicVariable.getVariableName()).isEqualTo("eVariable");
        assertThat(historicVariable.getValue()).isEqualTo((short) 12);
        assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

        historicVariable = historicVariables.get(5);
        assertThat(historicVariable.getVariableName()).isEqualTo("fVariable");
        assertThat(historicVariable.getValue()).isEqualTo(sdf.parse("01/01/2001 01:23:45 678"));
        assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

        historicVariable = historicVariables.get(6);
        assertThat(historicVariable.getVariableName()).isEqualTo("gVariable");
        assertThat(historicVariable.getValue()).isEqualTo(new SerializableVariable("hello hello"));
        assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

        historicVariable = historicVariables.get(7);
        assertThat(historicVariable.getVariableName()).isEqualTo("hVariable");
        assertThat(new String((byte[]) historicVariable.getValue())).as(";-)").isEqualTo(";-)");
        assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
    }

    @Test
    @Deployment
    public void testHistoricFormProperties() throws Exception {
        Date startedDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss SSS").parse("01/01/2001 01:23:46 000");

        processEngineConfiguration.getClock().setCurrentTime(startedDate);

        Map<String, String> formProperties = new HashMap<>();
        formProperties.put("formProp1", "Activiti rocks");
        formProperties.put("formProp2", "12345");

        ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("historicFormPropertiesProcess").singleResult();

        ProcessInstance processInstance = formService.submitStartFormData(procDef.getId(), formProperties);

        // Submit form-properties on the created task
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Out execution only has a single activity waiting, the task
        List<String> activityIds = runtimeService.getActiveActivityIds(task.getExecutionId());
        assertThat(activityIds).hasSize(1);

        String taskActivityId = activityIds.get(0);

        // Submit form properties
        formProperties = new HashMap<>();
        formProperties.put("formProp3", "Activiti still rocks!!!");
        formProperties.put("formProp4", "54321");
        formService.submitTaskFormData(task.getId(), formProperties);

        // 4 historic form properties should be created. 2 when process started,
        // 2 when task completed
        List<HistoricDetail> props = historyService.createHistoricDetailQuery().formProperties().processInstanceId(processInstance.getId())
                .orderByFormPropertyId().asc().list();

        HistoricFormProperty historicProperty1 = (HistoricFormProperty) props.get(0);
        assertThat(historicProperty1.getPropertyId()).isEqualTo("formProp1");
        assertThat(historicProperty1.getPropertyValue()).isEqualTo("Activiti rocks");
        assertThat(historicProperty1.getTime()).isEqualTo(startedDate);
        assertThat(historicProperty1.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(historicProperty1.getTaskId()).isNull();

        assertThat(historicProperty1.getActivityInstanceId()).isNotNull();
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                .activityInstanceId(historicProperty1.getActivityInstanceId()).singleResult();
        assertThat(historicActivityInstance).isNotNull();
        assertThat(historicActivityInstance.getActivityId()).isEqualTo("start");

        HistoricFormProperty historicProperty2 = (HistoricFormProperty) props.get(1);
        assertThat(historicProperty2.getPropertyId()).isEqualTo("formProp2");
        assertThat(historicProperty2.getPropertyValue()).isEqualTo("12345");
        assertThat(historicProperty2.getTime()).isEqualTo(startedDate);
        assertThat(historicProperty2.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(historicProperty2.getTaskId()).isNull();

        assertThat(historicProperty2.getActivityInstanceId()).isNotNull();
        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityInstanceId(historicProperty2.getActivityInstanceId())
                .singleResult();
        assertThat(historicActivityInstance).isNotNull();
        assertThat(historicActivityInstance.getActivityId()).isEqualTo("start");

        HistoricFormProperty historicProperty3 = (HistoricFormProperty) props.get(2);
        assertThat(historicProperty3.getPropertyId()).isEqualTo("formProp3");
        assertThat(historicProperty3.getPropertyValue()).isEqualTo("Activiti still rocks!!!");
        assertThat(historicProperty3.getTime()).isEqualTo(startedDate);
        assertThat(historicProperty3.getProcessInstanceId()).isEqualTo(processInstance.getId());
        String activityInstanceId = historicProperty3.getActivityInstanceId();
        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityInstanceId(activityInstanceId).singleResult();
        assertThat(historicActivityInstance).isNotNull();
        assertThat(historicActivityInstance.getActivityId()).isEqualTo(taskActivityId);
        assertThat(historicProperty3.getTaskId()).isNotNull();

        HistoricFormProperty historicProperty4 = (HistoricFormProperty) props.get(3);
        assertThat(historicProperty4.getPropertyId()).isEqualTo("formProp4");
        assertThat(historicProperty4.getPropertyValue()).isEqualTo("54321");
        assertThat(historicProperty4.getTime()).isEqualTo(startedDate);
        assertThat(historicProperty4.getProcessInstanceId()).isEqualTo(processInstance.getId());
        activityInstanceId = historicProperty4.getActivityInstanceId();
        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityInstanceId(activityInstanceId).singleResult();
        assertThat(historicActivityInstance).isNotNull();
        assertThat(historicActivityInstance.getActivityId()).isEqualTo(taskActivityId);
        assertThat(historicProperty4.getTaskId()).isNotNull();

        assertThat(props).hasSize(4);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricVariableQuery() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "activiti rocks!");
        variables.put("longVar", 12345L);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        // Query on activity-instance, activity instance null will return all
        // vars set when starting process
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().activityInstanceId(null).count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().activityInstanceId("unexisting").count()).isZero();

        // Query on process-instance
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId("unexisting").count()).isZero();

        // Query both process-instance and activity-instance
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().activityInstanceId(null).processInstanceId(processInstance.getId()).count())
                .isEqualTo(2);

        // end process instance
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());

        assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);

        // Query on process-instance
        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId("unexisting").count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricVariableQueryExcludeTaskRelatedDetails() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "activiti rocks!");
        variables.put("longVar", 12345L);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        // Set a local task-variable
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.setVariableLocal(task.getId(), "taskVar", "It is I, le Variable");

        // Query on process-instance
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        // Query on process-instance, excluding task-details
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).excludeTaskDetails().count())
                .isEqualTo(2);

        // Check task-id precedence on excluding task-details
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).excludeTaskDetails()
                .taskId(task.getId()).count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricFormPropertiesQuery() throws Exception {
        Map<String, String> formProperties = new HashMap<>();
        formProperties.put("stringVar", "activiti rocks!");
        formProperties.put("longVar", "12345");

        ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult();
        ProcessInstance processInstance = formService.submitStartFormData(procDef.getId(), formProperties);

        // Query on activity-instance, activity instance null will return all
        // vars set when starting process
        assertThat(historyService.createHistoricDetailQuery().formProperties().activityInstanceId(null).count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().formProperties().activityInstanceId("unexisting").count()).isZero();

        // Query on process-instance
        assertThat(historyService.createHistoricDetailQuery().formProperties().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().formProperties().processInstanceId("unexisting").count()).isZero();

        // Complete the task by submitting the task properties
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        formProperties = new HashMap<>();
        formProperties.put("taskVar", "task form property");
        formService.submitTaskFormData(task.getId(), formProperties);

        assertThat(historyService.createHistoricDetailQuery().formProperties().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        assertThat(historyService.createHistoricDetailQuery().formProperties().processInstanceId("unexisting").count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricVariableQuerySorting() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "activiti rocks!");
        variables.put("longVar", 12345L);

        runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByProcessInstanceId().asc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByTime().asc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableName().asc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableRevision().asc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableType().asc().count()).isEqualTo(2);

        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByProcessInstanceId().desc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByTime().desc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableName().desc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableRevision().desc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableType().desc().count()).isEqualTo(2);

        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByProcessInstanceId().asc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByTime().asc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableName().asc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableRevision().asc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableType().asc().list()).hasSize(2);

        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByProcessInstanceId().desc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByTime().desc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableName().desc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableRevision().desc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableType().desc().list()).hasSize(2);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricFormPropertySorting() throws Exception {

        Map<String, String> formProperties = new HashMap<>();
        formProperties.put("stringVar", "activiti rocks!");
        formProperties.put("longVar", "12345");

        ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult();
        formService.submitStartFormData(procDef.getId(), formProperties);

        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByProcessInstanceId().asc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByTime().asc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByFormPropertyId().asc().count()).isEqualTo(2);

        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByProcessInstanceId().desc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByTime().desc().count()).isEqualTo(2);
        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByFormPropertyId().desc().count()).isEqualTo(2);

        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByProcessInstanceId().asc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByTime().asc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByFormPropertyId().asc().list()).hasSize(2);

        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByProcessInstanceId().desc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByTime().desc().list()).hasSize(2);
        assertThat(historyService.createHistoricDetailQuery().formProperties().orderByFormPropertyId().desc().list()).hasSize(2);
    }

    @Test
    @Deployment
    public void testHistoricDetailQueryMixed() throws Exception {

        Map<String, String> formProperties = new HashMap<>();
        formProperties.put("formProp1", "activiti rocks!");
        formProperties.put("formProp2", "12345");

        ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("historicDetailMixed").singleResult();
        ProcessInstance processInstance = formService.submitStartFormData(procDef.getId(), formProperties);

        List<HistoricDetail> details = historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).orderByVariableName().asc().list();

        assertThat(details).hasSize(4);

        assertThat(details.get(0)).isInstanceOf(HistoricFormProperty.class);
        HistoricFormProperty formProp1 = (HistoricFormProperty) details.get(0);
        assertThat(formProp1.getPropertyId()).isEqualTo("formProp1");
        assertThat(formProp1.getPropertyValue()).isEqualTo("activiti rocks!");

        assertThat(details.get(1)).isInstanceOf(HistoricFormProperty.class);
        HistoricFormProperty formProp2 = (HistoricFormProperty) details.get(1);
        assertThat(formProp2.getPropertyId()).isEqualTo("formProp2");
        assertThat(formProp2.getPropertyValue()).isEqualTo("12345");

        assertThat(details.get(2)).isInstanceOf(HistoricVariableUpdate.class);
        HistoricVariableUpdate varUpdate1 = (HistoricVariableUpdate) details.get(2);
        assertThat(varUpdate1.getVariableName()).isEqualTo("variable1");
        assertThat(varUpdate1.getValue()).isEqualTo("activiti rocks!");

        // This variable should be of type LONG since this is defined in the
        // process-definition
        assertThat(details.get(3)).isInstanceOf(HistoricVariableUpdate.class);
        HistoricVariableUpdate varUpdate2 = (HistoricVariableUpdate) details.get(3);
        assertThat(varUpdate2.getVariableName()).isEqualTo("variable2");
        assertThat(varUpdate2.getValue()).isEqualTo(12345L);
    }

    @Test
    public void testHistoricDetailQueryInvalidSorting() throws Exception {

        assertThatThrownBy(() -> historyService.createHistoricDetailQuery().asc().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> historyService.createHistoricDetailQuery().desc().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> historyService.createHistoricDetailQuery().orderByProcessInstanceId().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> historyService.createHistoricDetailQuery().orderByTime().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> historyService.createHistoricDetailQuery().orderByVariableName().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> historyService.createHistoricDetailQuery().orderByVariableRevision().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> historyService.createHistoricDetailQuery().orderByVariableType().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    @Deployment
    public void testHistoricTaskInstanceVariableUpdates() {
        String processInstanceId = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest").getId();

        String taskId = taskService.createTaskQuery().singleResult().getId();

        runtimeService.setVariable(processInstanceId, "deadline", "yesterday");

        taskService.setVariableLocal(taskId, "bucket", "23c");
        taskService.setVariableLocal(taskId, "mop", "37i");

        taskService.complete(taskId);

        assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(1);

        List<HistoricDetail> historicTaskVariableUpdates = historyService.createHistoricDetailQuery().taskId(taskId).variableUpdates().orderByVariableName()
                .asc().list();

        assertThat(historicTaskVariableUpdates).hasSize(2);

        historyService.deleteHistoricTaskInstance(taskId);

        // Check if the variable updates have been removed as well
        historicTaskVariableUpdates = historyService.createHistoricDetailQuery().taskId(taskId).variableUpdates().orderByVariableName().asc().list();

        assertThat(historicTaskVariableUpdates).isEmpty();

        managementService.executeCommand(commandContext -> {
            processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(taskId);
            return null;
        });
    }

    // ACT-592
    @Test
    @Deployment
    public void testSetVariableOnProcessInstanceWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerVariablesProcess");
        runtimeService.setVariable(processInstance.getId(), "myVar", 123456L);
        assertThat(runtimeService.getVariable(processInstance.getId(), "myVar")).isEqualTo(123456L);
    }

    @Test
    @Deployment
    public void testDeleteHistoricProcessInstance() {
        // Start process-instance with some variables set
        Map<String, Object> vars = new HashMap<>();
        vars.put("processVar", 123L);
        vars.put("anotherProcessVar", new DummySerializable());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest", vars);
        assertThat(processInstance).isNotNull();

        // Set 2 task properties
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.setVariableLocal(task.getId(), "taskVar", 45678);
        taskService.setVariableLocal(task.getId(), "anotherTaskVar", "value");

        // Finish the task, this end the process-instance
        taskService.complete(task.getId());

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);
        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);
        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        // Delete the historic process-instance
        historyService.deleteHistoricProcessInstance(processInstance.getId());

        // Verify no traces are left in the history tables
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        assertThatThrownBy(() -> historyService.deleteHistoricProcessInstance("unexisting"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("No historic process instance found with id: unexisting");
    }

    @Test
    @Deployment
    public void testDeleteRunningHistoricProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");
        assertThat(processInstance).isNotNull();

        // Delete the historic process-instance, which is still running
        assertThatThrownBy(() -> historyService.deleteHistoricProcessInstance(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageStartingWith("Process instance is still running, cannot delete historic process instance");
    }

    /**
     * Test created to validate ACT-621 fix.
     */
    @Test
    @Deployment
    public void testHistoricFormPropertiesOnReEnteringActivity() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("comeBack", Boolean.TRUE);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricFormPropertiesProcess", variables);
        assertThat(processInstance).isNotNull();

        // Submit form on task
        Map<String, String> data = new HashMap<>();
        data.put("formProp1", "Property value");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        formService.submitTaskFormData(task.getId(), data);

        // Historic property should be available
        List<HistoricDetail> details = historyService.createHistoricDetailQuery().formProperties().processInstanceId(processInstance.getId()).list();
        assertThat(details).hasSize(1);

        // org.flowable.task.service.Task should be active in the same activity as the previous one
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        formService.submitTaskFormData(task.getId(), data);

        details = historyService.createHistoricDetailQuery().formProperties().processInstanceId(processInstance.getId()).list();
        assertThat(details).hasSize(2);

        // Should have 2 different historic activity instance ID's, with the
        // same activityId
        assertThat(details.get(1).getActivityInstanceId()).isNotSameAs(details.get(0).getActivityInstanceId());

        HistoricActivityInstance historicActInst1 = historyService.createHistoricActivityInstanceQuery()
                .activityInstanceId(details.get(0).getActivityInstanceId()).singleResult();
        assertActivityInstancesAreSame(historicActInst1,
                runtimeService.createActivityInstanceQuery().activityInstanceId(historicActInst1.getId()).singleResult());

        HistoricActivityInstance historicActInst2 = historyService.createHistoricActivityInstanceQuery()
                .activityInstanceId(details.get(1).getActivityInstanceId()).singleResult();
        assertActivityInstancesAreSame(historicActInst2,
                runtimeService.createActivityInstanceQuery().activityInstanceId(historicActInst2.getId()).singleResult());

        assertThat(historicActInst2.getActivityId()).isEqualTo(historicActInst1.getActivityId());
    }

    @Test
    @Deployment
    public void testHistoricTaskInstanceQueryTaskVariableValueEquals() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Set some variables on the task
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 12345L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "stringValue");
        variables.put("booleanVar", true);
        Date date = Calendar.getInstance().getTime();
        variables.put("dateVar", date);
        variables.put("nullVar", null);

        taskService.setVariablesLocal(task.getId(), variables);

        // Validate all variable-updates are present in DB
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().taskId(task.getId()).count()).isEqualTo(7);

        // Query Historic task instances based on variable
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("longVar", 12345L).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("shortVar", (short) 123).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("integerVar", 1234).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("stringVar", "stringValue").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("booleanVar", true).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("dateVar", date).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("nullVar", null).count()).isEqualTo(1);

        // Update the variables
        variables.put("longVar", 67890L);
        variables.put("shortVar", (short) 456);
        variables.put("integerVar", 5678);
        variables.put("stringVar", "updatedStringValue");
        variables.put("booleanVar", false);
        Calendar otherCal = Calendar.getInstance();
        otherCal.add(Calendar.DAY_OF_MONTH, 1);
        Date otherDate = otherCal.getTime();
        variables.put("dateVar", otherDate);
        variables.put("nullVar", null);

        taskService.setVariablesLocal(task.getId(), variables);

        // Validate all variable-updates are present in DB
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().taskId(task.getId()).count()).isEqualTo(14);

        // Previous values should NOT match
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("longVar", 12345L).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("shortVar", (short) 123).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("integerVar", 1234).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("stringVar", "stringValue").count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("booleanVar", true).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("dateVar", date).count()).isZero();

        // New values should match
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("longVar", 67890L).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("shortVar", (short) 456).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("integerVar", 5678).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("stringVar", "updatedStringValue").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("booleanVar", false).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("dateVar", otherDate).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("nullVar", null).count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testHistoricTaskInstanceQueryProcessVariableValueEquals() throws Exception {
        // Set some variables on the process instance
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 12345L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "stringValue");
        variables.put("booleanVar", true);
        Date date = Calendar.getInstance().getTime();
        variables.put("dateVar", date);
        variables.put("nullVar", null);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest", variables);
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Validate all variable-updates are present in DB
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

        // Query Historic task instances based on process variable
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("longVar", 12345L).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("shortVar", (short) 123).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("integerVar", 1234).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("stringVar", "stringValue").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("booleanVar", true).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("dateVar", date).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("nullVar", null).count()).isEqualTo(1);

        // Update the variables
        variables.put("longVar", 67890L);
        variables.put("shortVar", (short) 456);
        variables.put("integerVar", 5678);
        variables.put("stringVar", "updatedStringValue");
        variables.put("booleanVar", false);
        Calendar otherCal = Calendar.getInstance();
        otherCal.add(Calendar.DAY_OF_MONTH, 1);
        Date otherDate = otherCal.getTime();
        variables.put("dateVar", otherDate);
        variables.put("nullVar", null);

        runtimeService.setVariables(processInstance.getId(), variables);

        // Validate all variable-updates are present in DB
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(14);

        // Previous values should NOT match
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("longVar", 12345L).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("shortVar", (short) 123).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("integerVar", 1234).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("stringVar", "stringValue").count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("booleanVar", true).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("dateVar", date).count()).isZero();

        // New values should match
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("longVar", 67890L).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("shortVar", (short) 456).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("integerVar", 5678).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("stringVar", "updatedStringValue").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("booleanVar", false).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("dateVar", otherDate).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("nullVar", null).count()).isEqualTo(1);

        // Set a task-variables, shouldn't affect the process-variable matches
        taskService.setVariableLocal(task.getId(), "longVar", 9999L);
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("longVar", 9999L).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("longVar", 67890L).count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testHistoricProcessInstanceVariableValueEquals() throws Exception {
        // Set some variables on the process instance
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 12345L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "stringValue");
        variables.put("booleanVar", true);
        Date date = Calendar.getInstance().getTime();
        variables.put("dateVar", date);
        variables.put("nullVar", null);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricProcessInstanceTest", variables);

        // Validate all variable-updates are present in DB
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

        // Query Historic process instances based on process variable
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("longVar", 12345L).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("shortVar", (short) 123).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("integerVar", 1234).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("stringVar", "stringValue").count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("booleanVar", true).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("dateVar", date).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("nullVar", null).count()).isEqualTo(1);

        // Update the variables
        variables.put("longVar", 67890L);
        variables.put("shortVar", (short) 456);
        variables.put("integerVar", 5678);
        variables.put("stringVar", "updatedStringValue");
        variables.put("booleanVar", false);
        Calendar otherCal = Calendar.getInstance();
        otherCal.add(Calendar.DAY_OF_MONTH, 1);
        Date otherDate = otherCal.getTime();
        variables.put("dateVar", otherDate);
        variables.put("nullVar", null);

        runtimeService.setVariables(processInstance.getId(), variables);

        // Validate all variable-updates are present in DB
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(14);

        // Previous values should NOT match
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("longVar", 12345L).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("shortVar", (short) 123).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("integerVar", 1234).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("stringVar", "stringValue").count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("booleanVar", true).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("dateVar", date).count()).isZero();

        // New values should match
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("longVar", 67890L).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("shortVar", (short) 456).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("integerVar", 5678).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("stringVar", "updatedStringValue").count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("booleanVar", false).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("dateVar", otherDate).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("nullVar", null).count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/history/FullHistoryTest.testHistoricProcessInstanceVariableValueEquals.bpmn20.xml" })
    public void testHistoricProcessInstanceVariableValueNotEquals() throws Exception {
        // Set some variables on the process instance
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 12345L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "stringValue");
        variables.put("booleanVar", true);
        Date date = Calendar.getInstance().getTime();
        Calendar otherCal = Calendar.getInstance();
        otherCal.add(Calendar.DAY_OF_MONTH, 1);
        Date otherDate = otherCal.getTime();
        variables.put("dateVar", date);
        variables.put("nullVar", null);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricProcessInstanceTest", variables);

        // Validate all variable-updates are present in DB
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

        // Query Historic process instances based on process variable, shouldn't match
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("longVar", 12345L).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("shortVar", (short) 123).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("integerVar", 1234).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("stringVar", "stringValue").count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("booleanVar", true).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("dateVar", date).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("nullVar", null).count()).isZero();

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("longVar", 67890L).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("shortVar", (short) 456).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("integerVar", 5678).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("stringVar", "updatedStringValue").count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("booleanVar", false).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("dateVar", otherDate).count()).isEqualTo(1);

        // Update the variables
        variables.put("longVar", 67890L);
        variables.put("shortVar", (short) 456);
        variables.put("integerVar", 5678);
        variables.put("stringVar", "updatedStringValue");
        variables.put("booleanVar", false);
        variables.put("dateVar", otherDate);
        variables.put("nullVar", null);

        runtimeService.setVariables(processInstance.getId(), variables);

        // Validate all variable-updates are present in DB
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(14);

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("longVar", 12345L).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("shortVar", (short) 123).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("integerVar", 1234).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("stringVar", "stringValue").count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("booleanVar", true).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("dateVar", date).count()).isEqualTo(1);

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("longVar", 67890L).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("shortVar", (short) 456).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("integerVar", 5678).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("stringVar", "updatedStringValue").count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("booleanVar", false).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("dateVar", otherDate).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("nullVar", null).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/history/FullHistoryTest.testHistoricProcessInstanceVariableValueEquals.bpmn20.xml" })
    public void testHistoricProcessInstanceVariableValueLessThanAndGreaterThan() throws Exception {
        // Set some variables on the process instance
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 12345L);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricProcessInstanceTest", variables);

        // Validate all variable-updates are present in DB
        assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("longVar", 12345L).count()).isZero();
        // assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("longVar",
        // 12344L).count()).isEqualTo(1);
        // assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar",
        // 12345L).count()).isEqualTo(1);
        // assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar",
        // 12344L).count()).isEqualTo(1);
        // assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar",
        // 12346L).count()).isZero();
        //
        // assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThan("longVar",
        // 12345L).count()).isZero();
        // assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThan("longVar",
        // 12346L).count()).isEqualTo(1);
        // assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("longVar",
        // 12345L).count()).isEqualTo(1);
        // assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("longVar",
        // 12346L).count()).isEqualTo(1);
        // assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("longVar",
        // 12344L).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/history/FullHistoryTest.testVariableUpdatesAreLinkedToActivity.bpmn20.xml" })
    public void testVariableUpdatesLinkedToActivity() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("ProcessWithSubProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("test", "1");
        taskService.complete(task.getId(), variables);

        // now we are in the subprocess
        task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        variables.clear();
        variables.put("test", "2");
        taskService.complete(task.getId(), variables);

        // now we are ended
        assertProcessEnded(pi.getId());

        // check history
        List<HistoricDetail> updates = historyService.createHistoricDetailQuery().variableUpdates().list();
        assertThat(updates).hasSize(2);

        Map<String, HistoricVariableUpdate> updatesMap = new HashMap<>();
        HistoricVariableUpdate update = (HistoricVariableUpdate) updates.get(0);
        updatesMap.put((String) update.getValue(), update);
        update = (HistoricVariableUpdate) updates.get(1);
        updatesMap.put((String) update.getValue(), update);

        HistoricVariableUpdate update1 = updatesMap.get("1");
        HistoricVariableUpdate update2 = updatesMap.get("2");

        assertThat(update1.getActivityInstanceId()).isNotNull();
        assertThat(update1.getExecutionId()).isNotNull();
        HistoricActivityInstance historicActivityInstance1 = historyService.createHistoricActivityInstanceQuery()
                .activityInstanceId(update1.getActivityInstanceId()).singleResult();
        assertThat(historicActivityInstance1.getActivityId()).isEqualTo("usertask1");

        assertThat(update2.getActivityInstanceId()).isNotNull();
        HistoricActivityInstance historicActivityInstance2 = historyService.createHistoricActivityInstanceQuery()
                .activityInstanceId(update2.getActivityInstanceId()).singleResult();
        assertThat(historicActivityInstance2.getActivityId()).isEqualTo("usertask2");

        /*
         * This is OK! The variable is set on the root execution, on a execution never run through the activity, where the process instances stands when calling the set Variable. But the ActivityId of
         * this flow node is used. So the execution id's doesn't have to be equal.
         *
         * execution id: On which execution it was set activity id: in which activity was the process instance when setting the variable
         */
        assertThat(historicActivityInstance2.getExecutionId()).isNotEqualTo(update2.getExecutionId());
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/jpa/JPAVariableTest.testQueryJPAVariable.bpmn20.xml" })
    public void testReadJpaVariableValueFromHistoricVariableUpdate() {

        EntityManagerSessionFactory entityManagerSessionFactory = (EntityManagerSessionFactory) processEngineConfiguration.getSessionFactories()
                .get(EntityManagerSession.class);

        EntityManagerFactory entityManagerFactory = entityManagerSessionFactory.getEntityManagerFactory();

        String executionId = runtimeService.startProcessInstanceByKey("JPAVariableProcess").getProcessInstanceId();
        String variableName = "name";

        FieldAccessJPAEntity entity = new FieldAccessJPAEntity();
        entity.setId(1L);
        entity.setValue("Test");

        EntityManager manager = entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();
        manager.persist(entity);
        manager.flush();
        manager.getTransaction().commit();
        manager.close();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(executionId).taskName("my task").singleResult();

        runtimeService.setVariable(executionId, variableName, entity);
        taskService.complete(task.getId());

        List<HistoricDetail> variableUpdates = historyService.createHistoricDetailQuery().processInstanceId(executionId).variableUpdates().list();

        assertThat(variableUpdates).hasSize(1);
        HistoricVariableUpdate update = (HistoricVariableUpdate) variableUpdates.get(0);
        assertThat(update.getValue()).isInstanceOf(FieldAccessJPAEntity.class);

        assertThat(((FieldAccessJPAEntity) update.getValue()).getId()).isEqualTo(entity.getId());
    }

    /**
     * Test confirming fix for ACT-1731
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testQueryHistoricTaskIncludeBinaryVariable() throws Exception {
        // Start process with a binary variable
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("binaryVariable", (Object) "It is I, le binary".getBytes()));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.setVariableLocal(task.getId(), "binaryTaskVariable", (Object) "It is I, le binary".getBytes());

        // Complete task
        taskService.complete(task.getId());

        // Query task, including processVariables
        HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).includeProcessVariables().singleResult();
        assertThat(historicTask).isNotNull();
        assertThat(historicTask.getProcessVariables()).isNotNull();
        byte[] bytes = (byte[]) historicTask.getProcessVariables().get("binaryVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");

        // Query task, including taskVariables
        historicTask = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).includeTaskLocalVariables().singleResult();
        assertThat(historicTask).isNotNull();
        assertThat(historicTask.getTaskLocalVariables()).isNotNull();
        bytes = (byte[]) historicTask.getTaskLocalVariables().get("binaryTaskVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");
    }

    /**
     * Test confirming fix for ACT-1731
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testQueryHistoricProcessInstanceIncludeBinaryVariable() throws Exception {
        // Start process with a binary variable
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("binaryVariable", (Object) "It is I, le binary".getBytes()));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Complete task to end process
        taskService.complete(task.getId());

        // Query task, including processVariables
        HistoricProcessInstance historicProcess = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId())
                .includeProcessVariables().singleResult();
        assertThat(historicProcess).isNotNull();
        assertThat(historicProcess.getProcessVariables()).isNotNull();
        byte[] bytes = (byte[]) historicProcess.getProcessVariables().get("binaryVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");

    }

    // Test for https://activiti.atlassian.net/browse/ACT-2186
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricVariableRemovedWhenRuntimeVariableIsRemoved()
            throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("var1", "Hello");
        vars.put("var2", "World");
        vars.put("var3", "!");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Verify runtime
        assertThat(runtimeService.getVariables(processInstance.getId())).hasSize(3);
        assertThat(runtimeService.getVariables(processInstance.getId(), Arrays.asList("var1", "var2", "var3"))).hasSize(3);
        assertThat(runtimeService.getVariable(processInstance.getId(), "var2")).isNotNull();

        // Verify history
        assertThat(historyService.createHistoricVariableInstanceQuery().list()).hasSize(3);
        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("var2").singleResult())
                .isNotNull();

        // Verify historic details
        List<HistoricDetail> details = historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).variableUpdates().orderByTime()
                .asc().list();
        assertThat(details).hasSize(3); // 3 vars
        for (HistoricDetail historicDetail : details) {
            assertThat(((HistoricVariableUpdate) historicDetail).getValue()).isNotNull();
        }

        // Remove one variable
        Thread.sleep(750);
        runtimeService.removeVariable(processInstance.getId(), "var2");

        // Verify runtime
        assertThat(runtimeService.getVariables(processInstance.getId())).hasSize(2);
        assertThat(runtimeService.getVariables(processInstance.getId(), Arrays.asList("var1", "var2", "var3"))).hasSize(2);
        assertThat(runtimeService.getVariable(processInstance.getId(), "var2")).isNull();

        // Verify history
        assertThat(historyService.createHistoricVariableInstanceQuery().list()).hasSize(2);
        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("var2").singleResult())
                .isNull();

        // Verify historic details
        details = historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).variableUpdates().orderByTime().asc().list();
        assertThat(details).hasSize(4); // 3 vars + 1 delete

        // The last entry should be the delete
        for (int i = 0; i < details.size(); i++) {
            if (i != 3) {
                assertThat(((HistoricVariableUpdate) details.get(i)).getValue()).isNotNull();
            } else {
                assertThat(((HistoricVariableUpdate) details.get(i)).getValue()).isNull();
            }
        }

    }

}
