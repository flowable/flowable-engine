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

package org.flowable.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class RuntimeServiceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceWithVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("basicType", new DummySerializable());
        runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        org.flowable.task.api.Task task = taskService.createTaskQuery().includeProcessVariables().singleResult();
        assertThat(task.getProcessVariables()).isNotNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceWithLongStringVariable() {
        Map<String, Object> vars = new HashMap<>();
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 4001; i++) {
            longString.append("c");
        }
        vars.put("longString", longString.toString());
        runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        org.flowable.task.api.Task task = taskService.createTaskQuery().includeProcessVariables().singleResult();
        assertThat(task.getProcessVariables())
                .containsEntry("longString", longString.toString());
    }

    @Test
    public void testStartProcessInstanceByKeyNullKey() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testStartProcessInstanceByKeyUnexistingKey() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("unexistingkey"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("No process definition found for key 'unexistingkey'");
    }

    @Test
    public void testStartProcessInstanceByIdNullId() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceById(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testStartProcessInstanceByIdUnexistingId() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceById("unexistingId"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("no deployed process definition found with id 'unexistingId'");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceByIdNullVariables() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess", (Map<String, Object>) null);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceWithBusinessKey() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        // by key
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "123");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getBusinessKey()).isEqualTo("123");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);

        // by key with variables
        processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "456", CollectionUtil.singletonMap("var", "value"));
        assertThat(processInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(2);
        assertThat(runtimeService.getVariable(processInstance.getId(), "var")).isEqualTo("value");

        // by id
        processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(), "789");
        assertThat(processInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(3);

        // by id with variables
        processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(), "101123", CollectionUtil.singletonMap("var", "value2"));
        assertThat(processInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(4);
        assertThat(runtimeService.getVariable(processInstance.getId(), "var")).isEqualTo("value2");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceByProcessInstanceBuilder() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        // by key
        ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess").businessKey("123").start();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getBusinessKey()).isEqualTo("123");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);

        processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        // by key, with processInstance name with variables
        processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess").businessKey("456").variable("var", "value").name("processName1")
                .start();
        assertThat(processInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(2);
        assertThat(processInstance.getName()).isEqualTo("processName1");
        assertThat(processInstance.getBusinessKey()).isEqualTo("456");
        assertThat(runtimeService.getVariable(processInstance.getId(), "var")).isEqualTo("value");

        processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        // by id
        processInstance = processInstanceBuilder.processDefinitionId(processDefinition.getId()).businessKey("789").start();
        assertThat(processInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(3);
        assertThat(processInstance.getBusinessKey()).isEqualTo("789");

        processInstanceBuilder = runtimeService.createProcessInstanceBuilder();
        // by id with variables
        processInstance = processInstanceBuilder.processDefinitionId(processDefinition.getId()).businessKey("101123").variable("var", "value2").start();
        assertThat(processInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(4);
        assertThat(runtimeService.getVariable(processInstance.getId(), "var")).isEqualTo("value2");
        assertThat(processInstance.getBusinessKey()).isEqualTo("101123");

        processInstanceBuilder = runtimeService.createProcessInstanceBuilder();
        // by id and processInstance name
        processInstance = processInstanceBuilder.processDefinitionId(processDefinition.getId()).businessKey("101124").name("processName2").start();
        assertThat(processInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(5);
        assertThat(processInstance.getName()).isEqualTo("processName2");
        assertThat(processInstance.getBusinessKey()).isEqualTo("101124");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceByProcessInstanceBuilderAsync() {
        repositoryService.createProcessDefinitionQuery().singleResult();

        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        // by key
        ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess").businessKey("123").startAsync();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getBusinessKey()).isEqualTo("123");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count())
                .as("Process is started, but its execution waits on the job").isZero();

        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.isExclusive()).isFalse();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 2000, 200);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).as("The task is created from the job execution")
                .isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceByProcessInstanceBuilderAsyncWithDefinitionId() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        // by definitionId
        ProcessInstance processInstance = processInstanceBuilder.processDefinitionId(processDefinition.getId()).businessKey("123").startAsync();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getBusinessKey()).isEqualTo("123");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count())
                .as("Process is started, but its execution waits on the job").isZero();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 2000, 200);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).as("The task is created from the job execution")
                .isEqualTo(1);
    }

    @Test
    public void testStartProcessInstanceByProcessInstanceBuilderAsyncWithoutKeyAndDefinitionId() {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        assertThatThrownBy(() -> processInstanceBuilder.startAsync())
            .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
            .hasMessage("No processDefinitionId, processDefinitionKey provided");
    }

    @Test
    public void testStartProcessInstanceByProcessInstanceBuilderAsyncWithNonExistingDefKey() {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        assertThatThrownBy(() -> processInstanceBuilder.processDefinitionKey("nonExistingKey").startAsync())
            .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
            .hasMessage("No process definition found for key 'nonExistingKey'");
    }

    @Test
    public void testStartProcessInstanceByProcessInstanceBuilderAsyncWithNonExistingDefId() {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        assertThatThrownBy(() -> processInstanceBuilder.processDefinitionId("nonExistingDefinitionId").startAsync())
            .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
            .hasMessage("no deployed process definition found with id 'nonExistingDefinitionId'");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceDefinitionInformation() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess").start();

        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
        assertThat(processInstance.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
        assertThat(processInstance.getProcessDefinitionVersion().intValue()).isEqualTo(processDefinition.getVersion());
        assertThat(processInstance.getProcessDefinitionName()).isEqualTo(processDefinition.getName());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testProcessInstanceDefinitionInformationWithoutProcessDefinitionName() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("twoTasksProcess").start();

        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
        assertThat(processInstance.getProcessDefinitionKey()).isEqualTo("twoTasksProcess");
        assertThat(processInstance.getProcessDefinitionVersion().intValue()).isEqualTo(processDefinition.getVersion());
        assertThat(processInstance.getProcessDefinitionName()).isNull();
    }

    @Test
    public void testStartProcessInstanceByProcessInstanceBuilderWithTenantId() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml").
                tenantId("flowable").
                deploy();
        try {
            ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

            ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess").businessKey("123").
                    tenantId("flowable").start();
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getBusinessKey()).isEqualTo("123");
            assertThat(processInstance.getTenantId()).isEqualTo("flowable");

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTenantId()).isEqualTo("flowable");
        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testStartProcessInstanceByProcessInstanceBuilderWithOverrideTenantId() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
            .tenantId("flowable")
            .deploy();

        try {
            ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

            ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess")
                    .businessKey("123")
                    .tenantId("flowable")
                    .overrideProcessDefinitionTenantId("customTenant")
                    .start();

            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getBusinessKey()).isEqualTo("123");
            assertThat(processInstance.getTenantId()).isEqualTo("customTenant");

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTenantId()).isEqualTo("customTenant");

        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testStartProcessInstanceByProcessInstanceBuilderWithDefaultDefinitionTenantId() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
            .deploy();

        try {
            ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

            ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess")
                    .businessKey("123")
                    .overrideProcessDefinitionTenantId("customTenant")
                    .start();

            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getBusinessKey()).isEqualTo("123");
            assertThat(processInstance.getTenantId()).isEqualTo("customTenant");

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTenantId()).isEqualTo("customTenant");

        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testNonUniqueBusinessKey() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "123");

        // Behaviour changed: https://activiti.atlassian.net/browse/ACT-1860
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "123");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("123").count()).isEqualTo(2);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceFormWithoutFormKey() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("basicType", new DummySerializable());

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        runtimeService.startProcessInstanceWithForm(processDefinition.getId(), null, vars, null);
        org.flowable.task.api.Task task = taskService.createTaskQuery().includeProcessVariables().singleResult();
        assertThat(task.getProcessVariables()).isNotNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceFormWithoutVariables() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "outcome", null, null);
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceFormWithEmptyVariables() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "outcome", new HashMap<>(), null);
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();
    }

    // some databases might react strange on having multiple times null for the
    // business key
    // when the unique constraint is {processDefinitionId, businessKey}
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testMultipleNullBusinessKeys() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance.getBusinessKey()).isNull();

        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testDeleteProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);

        String deleteReason = "testing instance deletion";
        runtimeService.deleteProcessInstance(processInstance.getId(), deleteReason);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isZero();

        // test that the delete reason of the process instance shows up as
        // delete reason of the task in history ACT-848
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                    .singleResult();

            assertThat(historicTaskInstance.getDeleteReason()).isEqualTo(deleteReason);

            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId())
                    .singleResult();

            assertThat(historicInstance).isNotNull();
            assertThat(historicInstance.getDeleteReason()).isEqualTo(deleteReason);
            assertThat(historicInstance.getEndTime()).isNotNull();
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcessWithListener.bpmn20.xml" })
    public void testDeleteProcessInstanceWithListener() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);

        String deleteReason = "testing instance deletion";
        runtimeService.deleteProcessInstance(processInstance.getId(), deleteReason);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();

            assertThat(historicInstance).isNotNull();
            assertThat(historicInstance.getDeleteReason()).isEqualTo(deleteReason);
            assertThat(historicInstance.getEndTime()).isNotNull();
        }
    }
    
    @Test
    @Deployment(resources = { 
            "org/flowable/engine/test/api/taskProcessWithCallActivityListener.bpmn20.xml",
            "org/flowable/engine/test/api/oneTaskProcessWithListener.bpmn20.xml" 
    })
    public void testDeleteProcessInstanceWithCallActivityListener() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskAndCallActivityProcess");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("taskAndCallActivityProcess").count()).isEqualTo(1);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").singleResult();
        assertThat(subProcessInstance).isNotNull();
        
        String deleteReason = "testing instance deletion";
        runtimeService.deleteProcessInstance(processInstance.getId(), deleteReason);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("taskAndCallActivityProcess").count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();

            assertThat(historicInstance).isNotNull();
            assertThat(historicInstance.getDeleteReason()).isEqualTo(deleteReason);
            assertThat(historicInstance.getEndTime()).isNotNull();
            
            HistoricProcessInstance historicSubInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(subProcessInstance.getId())
                    .singleResult();

            assertThat(historicSubInstance).isNotNull();
            assertThat(historicSubInstance.getDeleteReason()).isEqualTo(deleteReason);
            assertThat(historicSubInstance.getEndTime()).isNotNull();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testDeleteProcessInstanceNullReason() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);

        // Deleting without a reason should be possible
        runtimeService.deleteProcessInstance(processInstance.getId(), null);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId())
                    .singleResult();

            assertThat(historicInstance).isNotNull();
            assertThat(historicInstance.getDeleteReason()).isEqualTo(DeleteReason.PROCESS_INSTANCE_DELETED);
        }
    }

    @Test
    public void testDeleteProcessInstanceUnexistingId() {
        assertThatThrownBy(() -> runtimeService.deleteProcessInstance("enexistingInstanceId", null))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("No process instance found for id");
    }

    @Test
    public void testDeleteProcessInstanceNullId() {
        assertThatThrownBy(() -> runtimeService.deleteProcessInstance(null, "test null id delete"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("processInstanceId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testFindActiveActivityIds() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<String> activities = runtimeService.getActiveActivityIds(processInstance.getId());
        assertThat(activities).hasSize(1);
    }

    @Test
    public void testFindActiveActivityIdsUnexistingExecututionId() {
        assertThatThrownBy(() -> runtimeService.getActiveActivityIds("unexistingExecutionId"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("execution unexistingExecutionId doesn't exist");
    }

    @Test
    public void testFindActiveActivityIdsNullExecututionId() {
        assertThatThrownBy(() -> runtimeService.getActiveActivityIds(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("executionId is null");
    }

    /**
     * Testcase to reproduce ACT-950 (https://jira.codehaus.org/browse/ACT-950)
     */
    @Test
    @Deployment
    public void testFindActiveActivityIdProcessWithErrorEventAndSubProcess() {
        ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey("errorEventSubprocess");

        List<String> activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
        assertThat(activeActivities).hasSize(5);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("ParallelUserTask", "MainUserTask");

        org.flowable.task.api.Task parallelUserTask = null;
        for (org.flowable.task.api.Task task : tasks) {
            if ("ParallelUserTask".equals(task.getName())) {
                parallelUserTask = task;
            }
        }
        taskService.complete(parallelUserTask.getId());

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subprocess1WaitBeforeError")
                .singleResult();
        runtimeService.trigger(execution.getId());

        activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
        assertThat(activeActivities).hasSize(4);

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("BeforeError", "MainUserTask");

        org.flowable.task.api.Task beforeErrorUserTask = null;
        for (org.flowable.task.api.Task task : tasks) {
            if ("BeforeError".equals(task.getName())) {
                beforeErrorUserTask = task;
            }
        }
        taskService.complete(beforeErrorUserTask.getId());

        activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
        assertThat(activeActivities).hasSize(2);

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("AfterError", "MainUserTask");

        org.flowable.task.api.Task afterErrorUserTask = null;
        for (org.flowable.task.api.Task task : tasks) {
            if ("AfterError".equals(task.getName())) {
                afterErrorUserTask = task;
            }
        }
        taskService.complete(afterErrorUserTask.getId());

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("MainUserTask");

        activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
        assertThat(activeActivities)
                .containsOnly("MainUserTask");

        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSignalUnexistingExecututionId() {
        assertThatThrownBy(() -> runtimeService.trigger("unexistingExecutionId"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("execution unexistingExecutionId doesn't exist");
    }

    @Test
    public void testSignalNullExecutionId() {
        assertThatThrownBy(() -> runtimeService.trigger(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("executionId is null");
    }

    @Test
    @Deployment
    public void testSignalWithProcessVariables() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSignalWithProcessVariables");
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("variable", "value");

        // signal the execution while passing in the variables
        Execution execution = runtimeService.createExecutionQuery().activityId("receiveMessage").singleResult();
        runtimeService.trigger(execution.getId(), processVariables);

        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables).isEqualTo(processVariables);
    }

    @Test
    public void testGetVariablesUnexistingExecutionId() {
        assertThatThrownBy(() -> runtimeService.getVariables("unexistingExecutionId"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("execution unexistingExecutionId doesn't exist");
    }

    @Test
    public void testGetVariablesNullExecutionId() {
        assertThatThrownBy(() -> runtimeService.getVariables(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("executionId is null");
    }

    @Test
    public void testGetVariableUnexistingExecutionId() {
        assertThatThrownBy(() -> runtimeService.getVariable("unexistingExecutionId", "var"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("execution unexistingExecutionId doesn't exist");
    }

    @Test
    public void testGetVariableNullExecutionId() {
        assertThatThrownBy(() -> runtimeService.getVariable(null, "var"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("executionId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableUnexistingVariableName() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Object variableValue = runtimeService.getVariable(processInstance.getId(), "unexistingVariable");
        assertThat(variableValue).isNull();
    }

    @Test
    public void testSetVariableUnexistingExecutionId() {
        assertThatThrownBy(() -> runtimeService.setVariable("unexistingExecutionId", "variableName", "value"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("execution unexistingExecutionId doesn't exist");
    }

    @Test
    public void testSetVariableNullExecutionId() {
        assertThatThrownBy(() -> runtimeService.setVariable(null, "variableName", "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("executionId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetVariableNullVariableName() {
        assertThatThrownBy(() -> {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            runtimeService.setVariable(processInstance.getId(), null, "variableValue");
        })
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("variableName is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.setVariables(processInstance.getId(), vars);

        assertThat(runtimeService.getVariable(processInstance.getId(), "variable1")).isEqualTo("value1");
        assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");
    }

    @Test
    public void testSetVariablesUnexistingExecutionId() {
        assertThatThrownBy(() -> runtimeService.setVariables("unexistingexecution", new HashMap<>()))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("execution unexistingexecution doesn't exist");
    }

    @Test
    public void testSetVariablesNullExecutionId() {
        assertThatThrownBy(() -> runtimeService.setVariables(null, new HashMap<>()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("executionId is null");
    }

    private void checkHistoricVariableUpdateEntity(String variableName, String processInstanceId) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            boolean deletedVariableUpdateFound = false;

            List<HistoricDetail> resultSet = historyService.createHistoricDetailQuery().processInstanceId(processInstanceId).list();
            for (HistoricDetail currentHistoricDetail : resultSet) {
                assertThat(currentHistoricDetail).isInstanceOf(HistoricDetailVariableInstanceUpdateEntity.class);
                HistoricDetailVariableInstanceUpdateEntity historicVariableUpdate = (HistoricDetailVariableInstanceUpdateEntity) currentHistoricDetail;

                if (historicVariableUpdate.getName().equals(variableName)) {
                    if (historicVariableUpdate.getValue() == null) {
                        if (deletedVariableUpdateFound) {
                            fail("Mismatch: A HistoricVariableUpdateEntity with a null value already found");
                        } else {
                            deletedVariableUpdateFound = true;
                        }
                    }
                }
            }

            assertThat(deletedVariableUpdateFound).isTrue();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.setVariables(processInstance.getId(), vars);

        runtimeService.removeVariable(processInstance.getId(), "variable1");

        assertThat(runtimeService.getVariable(processInstance.getId(), "variable1")).isNull();
        assertThat(runtimeService.getVariableLocal(processInstance.getId(), "variable1")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneSubProcess.bpmn20.xml" })
    public void testRemoveVariableInParentScope() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);
        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        runtimeService.removeVariable(currentTask.getExecutionId(), "variable1");

        assertThat(runtimeService.getVariable(processInstance.getId(), "variable1")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    }

    @Test
    public void testRemoveVariableNullExecutionId() {
        assertThatThrownBy(() -> runtimeService.removeVariable(null, "variable"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("executionId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariableLocal() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        runtimeService.removeVariableLocal(processInstance.getId(), "variable1");

        assertThat(runtimeService.getVariable(processInstance.getId(), "variable1")).isNull();
        assertThat(runtimeService.getVariableLocal(processInstance.getId(), "variable1")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneSubProcess.bpmn20.xml" })
    public void testRemoveVariableLocalWithParentScope() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);
        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();
        runtimeService.setVariableLocal(currentTask.getExecutionId(), "localVariable", "local value");

        assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "localVariable")).isEqualTo("local value");

        runtimeService.removeVariableLocal(currentTask.getExecutionId(), "localVariable");

        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "localVariable")).isNull();
        assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "localVariable")).isNull();

        assertThat(runtimeService.getVariable(processInstance.getId(), "variable1")).isEqualTo("value1");
        assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");

        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable1")).isEqualTo("value1");
        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable2")).isEqualTo("value2");

        checkHistoricVariableUpdateEntity("localVariable", processInstance.getId());
    }

    @Test
    public void testRemoveLocalVariableNullExecutionId() {
        assertThatThrownBy(() -> runtimeService.removeVariableLocal(null, "variable"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("executionId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        runtimeService.setVariable(processInstance.getId(), "variable3", "value3");

        runtimeService.removeVariables(processInstance.getId(), vars.keySet());

        assertThat(runtimeService.getVariable(processInstance.getId(), "variable1")).isNull();
        assertThat(runtimeService.getVariableLocal(processInstance.getId(), "variable1")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isNull();
        assertThat(runtimeService.getVariableLocal(processInstance.getId(), "variable2")).isNull();

        assertThat(runtimeService.getVariable(processInstance.getId(), "variable3")).isEqualTo("value3");
        assertThat(runtimeService.getVariableLocal(processInstance.getId(), "variable3")).isEqualTo("value3");

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneSubProcess.bpmn20.xml" })
    public void testRemoveVariablesWithParentScope() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);
        runtimeService.setVariable(processInstance.getId(), "variable3", "value3");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        runtimeService.removeVariables(currentTask.getExecutionId(), vars.keySet());

        assertThat(runtimeService.getVariable(processInstance.getId(), "variable1")).isNull();
        assertThat(runtimeService.getVariableLocal(processInstance.getId(), "variable1")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isNull();
        assertThat(runtimeService.getVariableLocal(processInstance.getId(), "variable2")).isNull();

        assertThat(runtimeService.getVariable(processInstance.getId(), "variable3")).isEqualTo("value3");
        assertThat(runtimeService.getVariableLocal(processInstance.getId(), "variable3")).isEqualTo("value3");

        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable1")).isNull();
        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable2")).isNull();

        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable3")).isEqualTo("value3");

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
    }

    @Test
    public void testRemoveVariablesNullExecutionId() {
        assertThatThrownBy(() -> runtimeService.removeVariables(null, new HashSet<>()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("executionId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneSubProcess.bpmn20.xml" })
    public void testRemoveVariablesLocalWithParentScope() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();
        Map<String, Object> varsToDelete = new HashMap<>();
        varsToDelete.put("variable3", "value3");
        varsToDelete.put("variable4", "value4");
        varsToDelete.put("variable5", "value5");
        runtimeService.setVariablesLocal(currentTask.getExecutionId(), varsToDelete);
        runtimeService.setVariableLocal(currentTask.getExecutionId(), "variable6", "value6");

        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable3")).isEqualTo("value3");
        assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable3")).isEqualTo("value3");
        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable4")).isEqualTo("value4");
        assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable4")).isEqualTo("value4");
        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable5")).isEqualTo("value5");
        assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable5")).isEqualTo("value5");
        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable6")).isEqualTo("value6");
        assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable6")).isEqualTo("value6");

        runtimeService.removeVariablesLocal(currentTask.getExecutionId(), varsToDelete.keySet());

        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable1")).isEqualTo("value1");
        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable2")).isEqualTo("value2");

        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable3")).isNull();
        assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable3")).isNull();
        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable4")).isNull();
        assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable4")).isNull();
        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable5")).isNull();
        assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable5")).isNull();

        assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable6")).isEqualTo("value6");
        assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable6")).isEqualTo("value6");

        checkHistoricVariableUpdateEntity("variable3", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable4", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable5", processInstance.getId());
    }

    @Test
    public void testRemoveVariablesLocalNullExecutionId() {
        assertThatThrownBy(() -> runtimeService.removeVariablesLocal(null, new HashSet<>()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("executionId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/RuntimeServiceTest.catchAlertSignal.bpmn20.xml",
        "org/flowable/engine/test/api/runtime/RuntimeServiceTest.catchPanicSignal.bpmn20.xml" })
    public void testSignalEventReceived() {

        startSignalCatchProcesses();
        // 15, because the signal catch is a scope
        assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(15);
        runtimeService.signalEventReceived("alert");
        assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(9);
        runtimeService.signalEventReceived("panic");
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        // //// test signalEventReceived(String, String)
        startSignalCatchProcesses();

        // signal the executions one at a time:
        for (int executions = 3; executions > 0; executions--) {
            List<Execution> page = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").listPage(0, 1);
            runtimeService.signalEventReceived("alert", page.get(0).getId());

            assertThat(runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").count()).isEqualTo(executions - 1);
        }

        for (int executions = 3; executions > 0; executions--) {
            List<Execution> page = runtimeService.createExecutionQuery().signalEventSubscriptionName("panic").listPage(0, 1);
            runtimeService.signalEventReceived("panic", page.get(0).getId());

            assertThat(runtimeService.createExecutionQuery().signalEventSubscriptionName("panic").count()).isEqualTo(executions - 1);
        }

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/RuntimeServiceTest.catchAlertMessage.bpmn20.xml",
        "org/flowable/engine/test/api/runtime/RuntimeServiceTest.catchPanicMessage.bpmn20.xml" })
    public void testMessageEventReceived() {

        startMessageCatchProcesses();
        // 12, because the signal catch is a scope
        assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(12);

        // signal the executions one at a time:
        for (int executions = 3; executions > 0; executions--) {
            List<Execution> page = runtimeService.createExecutionQuery().messageEventSubscriptionName("alert").listPage(0, 1);
            runtimeService.messageEventReceived("alert", page.get(0).getId());

            assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName("alert").count()).isEqualTo(executions - 1);
        }

        for (int executions = 3; executions > 0; executions--) {
            List<Execution> page = runtimeService.createExecutionQuery().messageEventSubscriptionName("panic").listPage(0, 1);
            runtimeService.messageEventReceived("panic", page.get(0).getId());

            assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName("panic").count()).isEqualTo(executions - 1);
        }

    }

    @Test
    public void testSignalEventReceivedNonExistingExecution() {
        assertThatThrownBy(() -> runtimeService.signalEventReceived("alert", "nonexistingExecution"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class);
    }

    @Test
    public void testMessageEventReceivedNonExistingExecution() {
        assertThatThrownBy(() -> runtimeService.messageEventReceived("alert", "nonexistingExecution"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/RuntimeServiceTest.catchAlertSignal.bpmn20.xml" })
    public void testExecutionWaitingForDifferentSignal() {
        runtimeService.startProcessInstanceByKey("catchAlertSignal");
        Execution execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        assertThatThrownBy(() -> runtimeService.signalEventReceived("bogusSignal", execution.getId()))
                .isExactlyInstanceOf(FlowableException.class);

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetProcessInstanceName() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getName()).isNull();

        // Set the name
        runtimeService.setProcessInstanceName(processInstance.getId(), "New name");
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getName()).isEqualTo("New name");

        // Set the name to null
        runtimeService.setProcessInstanceName(processInstance.getId(), null);
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getName()).isNull();

        // Set name for unexisting process instance, should fail
        assertThatThrownBy(() -> runtimeService.setProcessInstanceName("unexisting", null))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class);

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getName()).isNull();

        // Set name for suspended process instance, should fail
        String processInstanceId = processInstance.getId();
        runtimeService.suspendProcessInstanceById(processInstanceId);
        assertThatThrownBy(() -> runtimeService.setProcessInstanceName(processInstanceId, null))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("process instance " + processInstanceId + " is suspended, cannot set name");

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getName()).isNull();
    }

    private void startSignalCatchProcesses() {
        for (int i = 0; i < 3; i++) {
            runtimeService.startProcessInstanceByKey("catchAlertSignal");
            runtimeService.startProcessInstanceByKey("catchPanicSignal");
        }
    }

    private void startMessageCatchProcesses() {
        for (int i = 0; i < 3; i++) {
            runtimeService.startProcessInstanceByKey("catchAlertMessage");
            runtimeService.startProcessInstanceByKey("catchPanicMessage");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableUnexistingVariableNameWithCast() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        String variableValue = runtimeService.getVariable(processInstance.getId(), "unexistingVariable", String.class);
        assertThat(variableValue).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableExistingVariableNameWithCast() {
        Map<String, Object> params = new HashMap<>();
        params.put("var1", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", params);
        Boolean variableValue = runtimeService.getVariable(processInstance.getId(), "var1", Boolean.class);
        assertThat(variableValue).isTrue();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableExistingVariableNameWithInvalidCast() {
        Map<String, Object> params = new HashMap<>();
        params.put("var1", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", params);
        assertThatThrownBy(() -> runtimeService.getVariable(processInstance.getId(), "var1", String.class))
            .isExactlyInstanceOf(ClassCastException.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalUnexistingVariableNameWithCast() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        String variableValue = runtimeService.getVariableLocal(processInstance.getId(), "var1", String.class);
        assertThat(variableValue).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalExistingVariableNameWithCast() {
        Map<String, Object> params = new HashMap<>();
        params.put("var1", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", params);
        Boolean variableValue = runtimeService.getVariableLocal(processInstance.getId(), "var1", Boolean.class);
        assertThat(variableValue).isTrue();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalExistingVariableNameWithInvalidCast() {
        Map<String, Object> params = new HashMap<>();
        params.put("var1", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", params);
        assertThatThrownBy(() -> runtimeService.getVariableLocal(processInstance.getId(), "var1", String.class))
            .isExactlyInstanceOf(ClassCastException.class);
    }

    // Test for https://activiti.atlassian.net/browse/ACT-2186
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricVariableRemovedWhenRuntimeVariableIsRemoved() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("var1", "Hello");
            vars.put("var2", "World");
            vars.put("var3", "!");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

            // Verify runtime
            assertThat(runtimeService.getVariables(processInstance.getId())).hasSize(3);
            assertThat(runtimeService.getVariables(processInstance.getId(), Arrays.asList("var1", "var2", "var3"))).hasSize(3);
            assertThat(runtimeService.getVariable(processInstance.getId(), "var2")).isNotNull();

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            // Verify history
            assertThat(historyService.createHistoricVariableInstanceQuery().list()).hasSize(3);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("var2").singleResult())
                    .isNotNull();

            // Remove one variable
            runtimeService.removeVariable(processInstance.getId(), "var2");

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            // Verify runtime
            assertThat(runtimeService.getVariables(processInstance.getId())).hasSize(2);
            assertThat(runtimeService.getVariables(processInstance.getId(), Arrays.asList("var1", "var2", "var3"))).hasSize(2);
            assertThat(runtimeService.getVariable(processInstance.getId(), "var2")).isNull();

            // Verify history
            assertThat(historyService.createHistoricVariableInstanceQuery().list()).hasSize(2);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("var2").singleResult())
                    .isNull();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartTimeProcessInstance() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date noon = calendar.getTime();

        processEngineConfiguration.getClock().setCurrentTime(noon);
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(processInstance.getStartTime()).isEqualTo(noon);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testAuthenticatedStartUserProcessInstance() {
        final String authenticatedUser = "user1";
        identityService.setAuthenticatedUserId(authenticatedUser);
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(processInstance.getStartUserId()).isEqualTo(authenticatedUser);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testNoAuthenticatedStartUserProcessInstance() {
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(processInstance.getStartUserId()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testAdhocCallbacks() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .callbackId("nonExistingCase")
                .callbackType(CallbackTypes.CASE_ADHOC_CHILD)
                .start();

        assertThat(processInstance.getCallbackId()).isEqualTo("nonExistingCase");
        assertThat(processInstance.getCallbackType()).isEqualTo(CallbackTypes.CASE_ADHOC_CHILD);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartByProcessInstanceBuilderWithFallbackToDefaultTenant() {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess")
                .tenantId("flowable")
                .fallbackToDefaultTenant()
                .start();

        assertThat(processInstance).isNotNull();
    }

    @Test
    public void testStartByProcessInstanceBuilderWithFallbackToDefaultTenant_definitionNotFound() {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        assertThatThrownBy(() -> processInstanceBuilder.processDefinitionKey("nonExistingDefinition")
                .tenantId("flowable")
                .fallbackToDefaultTenant()
                .start()
        )
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("No process definition found for key 'nonExistingDefinition'. Fallback to default tenant was also applied.");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" },
        tenantId = "nonDefaultTenant"
    )
    public void testStartByProcessInstanceBuilderWithFallbackToDefaultTenant_definitionNotFoundInNonDefaultTenant() {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        assertThatThrownBy(() -> processInstanceBuilder.processDefinitionKey("oneTaskProcess")
                .tenantId("flowable")
                .fallbackToDefaultTenant()
                .start()
        )
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("No process definition found for key 'oneTaskProcess'. Fallback to default tenant was also applied.");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartAsyncWithFallbackToDefaultTenant() {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess")
                .tenantId("flowable")
                .fallbackToDefaultTenant()
                .startAsync();

        assertThat(processInstance).isNotNull();
    }

    @Test
    public void testStartAsyncWithFallbackToDefaultTenant_definitionNotFound() {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        assertThatThrownBy(() -> processInstanceBuilder.processDefinitionKey("nonExistingDefinition")
                .tenantId("flowable")
                .fallbackToDefaultTenant()
                .startAsync()
        )
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("No process definition found for key 'nonExistingDefinition'. Fallback to default tenant was also applied.");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" },
        tenantId = "nonDefaultTenant"
    )
    public void testStartAsyncWithFallbackToDefaultTenant_definitionNotFoundInNonDefaultTenant() {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        assertThatThrownBy(() -> processInstanceBuilder.processDefinitionKey("oneTaskProcess")
                .tenantId("flowable")
                .fallbackToDefaultTenant()
                .startAsync()
        )
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("No process definition found for key 'oneTaskProcess'. Fallback to default tenant was also applied.");
    }

}
