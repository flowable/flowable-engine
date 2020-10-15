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

package org.flowable.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.api.runtime.ProcessInstanceQueryTest;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Frederik Heremans
 * @author Falko Menge
 */
public class HistoryServiceTest extends PluggableFlowableTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryServiceTest.class);

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceQuery() {
        // With a clean ProcessEngine, no instances should be available
        assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(1);

        // Complete the task and check if the size is count 1
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        taskService.complete(tasks.get(0).getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceQueryOrderBy() {
        // With a clean ProcessEngine, no instances should be available
        assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        taskService.complete(tasks.get(0).getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        historyService.createHistoricTaskInstanceQuery().orderByDeleteReason().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceId().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByTaskCreateTime().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByHistoricTaskInstanceDuration().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByHistoricTaskInstanceEndTime().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByProcessDefinitionId().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByProcessInstanceId().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByTaskDefinitionKey().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByTaskDescription().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByTaskId().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByTaskName().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByTaskOwner().asc().list();
        historyService.createHistoricTaskInstanceQuery().orderByTaskPriority().asc().list();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceUserIdAndActivityId() {
        identityService.setAuthenticatedUserId("johndoe");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
        assertThat(historicProcessInstance.getStartUserId()).isEqualTo("johndoe");
        assertThat(historicProcessInstance.getStartActivityId()).isEqualTo("theStart");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        taskService.complete(tasks.get(0).getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
        assertThat(historicProcessInstance.getEndActivityId()).isEqualTo("theEnd");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/callactivity/orderProcess.bpmn20.xml",
            "org/flowable/examples/bpmn/callactivity/checkCreditProcess.bpmn20.xml" })
    public void testOrderProcessWithCallActivity() {
        // After the process has started, the 'verify credit history' task should be active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("orderProcess");
        TaskQuery taskQuery = taskService.createTaskQuery();
        org.flowable.task.api.Task verifyCreditTask = taskQuery.singleResult();

        // Completing the task with approval, will end the subprocess and continue the original process
        taskService.complete(verifyCreditTask.getId(), CollectionUtil.singletonMap("creditApproved", true));
        org.flowable.task.api.Task prepareAndShipTask = taskQuery.singleResult();
        assertThat(prepareAndShipTask.getName()).isEqualTo("Prepare and Ship");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        // verify
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertThat(historicProcessInstance).isNotNull();
        assertThat(historicProcessInstance.getProcessDefinitionId()).contains("checkCreditProcess");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/callactivity/orderProcess.bpmn20.xml",
            "org/flowable/examples/bpmn/callactivity/checkCreditProcess.bpmn20.xml" })
    public void testExcludeSubprocesses() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("orderProcess");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().excludeSubprocesses(true).singleResult();
        assertThat(historicProcessInstance).isNotNull();
        assertThat(historicProcessInstance.getId()).isEqualTo(pi.getId());

        List<HistoricProcessInstance> instanceList = historyService.createHistoricProcessInstanceQuery().excludeSubprocesses(false).list();
        assertThat(instanceList).hasSize(2);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml", "org/flowable/examples/bpmn/callactivity/orderProcess.bpmn20.xml",
            "org/flowable/examples/bpmn/callactivity/checkCreditProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceQueryByProcessDefinitionKey() {

        String processDefinitionKey = "oneTaskProcess";
        runtimeService.startProcessInstanceByKey(processDefinitionKey);
        runtimeService.startProcessInstanceByKey("orderProcess");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processDefinitionKey(processDefinitionKey)
                .singleResult();
        assertThat(historicProcessInstance).isNotNull();
        assertThat(historicProcessInstance.getProcessDefinitionKey()).isEqualTo(processDefinitionKey);
        assertThat(historicProcessInstance.getStartActivityId()).isEqualTo("theStart");

        // now complete the task to end the process instance
        org.flowable.task.api.Task task = taskService.createTaskQuery().processDefinitionKey("checkCreditProcess").singleResult();
        Map<String, Object> map = new HashMap<>();
        map.put("creditApproved", true);
        taskService.complete(task.getId(), map);

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        // and make sure the super process instance is set correctly on the HistoricProcessInstance
        HistoricProcessInstance historicProcessInstanceSub = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("checkCreditProcess")
                .singleResult();
        HistoricProcessInstance historicProcessInstanceSuper = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("orderProcess")
                .singleResult();
        assertThat(historicProcessInstanceSub.getSuperProcessInstanceId()).isEqualTo(historicProcessInstanceSuper.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml" })
    public void testHistoricProcessInstanceQueryByProcessInstanceIds() {
        HashSet<String> processInstanceIds = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            processInstanceIds.add(runtimeService.startProcessInstanceByKey("oneTaskProcess", String.valueOf(i)).getId());
        }
        processInstanceIds.add(runtimeService.startProcessInstanceByKey("oneTaskProcess2", "1").getId());

        // start an instance that will not be part of the query
        runtimeService.startProcessInstanceByKey("oneTaskProcess2", "2");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        HistoricProcessInstanceQuery processInstanceQuery = historyService.createHistoricProcessInstanceQuery().processInstanceIds(processInstanceIds);
        assertThat(processInstanceQuery.count()).isEqualTo(5);

        List<HistoricProcessInstance> processInstances = processInstanceQuery.list();
        assertThat(processInstances).hasSize(5);

        for (HistoricProcessInstance historicProcessInstance : processInstances) {
            assertThat(processInstanceIds).contains(historicProcessInstance.getId());
        }
    }

    @Test
    public void testHistoricProcessInstanceQueryByProcessInstanceIdsEmpty() {
        assertThatThrownBy(() -> historyService.createHistoricProcessInstanceQuery().processInstanceIds(new HashSet<>()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Set of process instance ids is empty");
    }

    @Test
    public void testHistoricProcessInstanceQueryByProcessInstanceIdsNull() {
        assertThatThrownBy(() -> historyService.createHistoricProcessInstanceQuery().processInstanceIds(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Set of process instance ids is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceQueryForDelete() {
        String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
        runtimeService.deleteProcessInstance(processInstanceId, null);

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        HistoricProcessInstanceQuery processInstanceQuery = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId);
        assertThat(processInstanceQuery.count()).isEqualTo(1);
        HistoricProcessInstance processInstance = processInstanceQuery.singleResult();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);
        assertThat(processInstance.getDeleteReason()).isEqualTo(DeleteReason.PROCESS_INSTANCE_DELETED);

        processInstanceQuery = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).deleted();
        assertThat(processInstanceQuery.count()).isEqualTo(1);
        processInstance = processInstanceQuery.singleResult();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);
        assertThat(processInstance.getDeleteReason()).isEqualTo(DeleteReason.PROCESS_INSTANCE_DELETED);

        processInstanceQuery = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).notDeleted();
        assertThat(processInstanceQuery.count()).isZero();

        historyService.deleteHistoricProcessInstance(processInstanceId);

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        processInstanceQuery = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId);
        assertThat(processInstanceQuery.count()).isZero();

        processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
        runtimeService.deleteProcessInstance(processInstanceId, "custom message");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        processInstanceQuery = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId);
        assertThat(processInstanceQuery.count()).isEqualTo(1);
        processInstance = processInstanceQuery.singleResult();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);
        assertThat(processInstance.getDeleteReason()).isEqualTo("custom message");

        processInstanceQuery = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).deleted();
        assertThat(processInstanceQuery.count()).isEqualTo(1);
        processInstance = processInstanceQuery.singleResult();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);
        assertThat(processInstance.getDeleteReason()).isEqualTo("custom message");

        processInstanceQuery = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).notDeleted();
        assertThat(processInstanceQuery.count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml" })
    public void testHistoricProcessInstanceQueryByDeploymentId() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        for (int i = 0; i < 4; i++) {
            runtimeService.startProcessInstanceByKey("oneTaskProcess", String.valueOf(i));
        }
        runtimeService.startProcessInstanceByKey("oneTaskProcess2", "1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        HistoricProcessInstanceQuery processInstanceQuery = historyService.createHistoricProcessInstanceQuery().deploymentId(deployment.getId());
        assertThat(processInstanceQuery.count()).isEqualTo(5);
        assertThat(processInstanceQuery.list().get(0).getDeploymentId()).isEqualTo(deployment.getId());

        List<HistoricProcessInstance> processInstances = processInstanceQuery.list();
        assertThat(processInstances).hasSize(5);

        processInstanceQuery = historyService.createHistoricProcessInstanceQuery().deploymentId("invalid");
        assertThat(processInstanceQuery.count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml" })
    public void testHistoricProcessInstanceQueryByDeploymentIdIn() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        for (int i = 0; i < 4; i++) {
            runtimeService.startProcessInstanceByKey("oneTaskProcess", String.valueOf(i));
        }
        runtimeService.startProcessInstanceByKey("oneTaskProcess2", "1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        List<String> deploymentIds = new ArrayList<>();
        deploymentIds.add(deployment.getId());
        deploymentIds.add("invalid");
        HistoricProcessInstanceQuery processInstanceQuery = historyService.createHistoricProcessInstanceQuery().deploymentIdIn(deploymentIds);
        assertThat(processInstanceQuery.count()).isEqualTo(5);

        List<HistoricProcessInstance> processInstances = processInstanceQuery.list();
        assertThat(processInstances).hasSize(5);

        deploymentIds = new ArrayList<>();
        deploymentIds.add("invalid");
        processInstanceQuery = historyService.createHistoricProcessInstanceQuery().deploymentIdIn(deploymentIds);
        assertThat(processInstanceQuery.count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml" })
    public void testHistoricTaskInstanceQueryByDeploymentId() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        for (int i = 0; i < 4; i++) {
            runtimeService.startProcessInstanceByKey("oneTaskProcess", String.valueOf(i));
        }
        runtimeService.startProcessInstanceByKey("oneTaskProcess2", "1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        HistoricTaskInstanceQuery taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().deploymentId(deployment.getId());
        assertThat(taskInstanceQuery.count()).isEqualTo(5);

        List<HistoricTaskInstance> taskInstances = taskInstanceQuery.list();
        assertThat(taskInstances).hasSize(5);

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().deploymentId("invalid");
        assertThat(taskInstanceQuery.count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml" })
    public void testHistoricTaskInstanceQueryByDeploymentIdIn() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        for (int i = 0; i < 4; i++) {
            runtimeService.startProcessInstanceByKey("oneTaskProcess", String.valueOf(i));
        }
        runtimeService.startProcessInstanceByKey("oneTaskProcess2", "1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        List<String> deploymentIds = new ArrayList<>();
        deploymentIds.add(deployment.getId());
        HistoricTaskInstanceQuery taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().deploymentIdIn(deploymentIds);
        assertThat(taskInstanceQuery.count()).isEqualTo(5);

        List<HistoricTaskInstance> taskInstances = taskInstanceQuery.list();
        assertThat(taskInstances).hasSize(5);

        deploymentIds.add("invalid");
        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().deploymentIdIn(deploymentIds);
        assertThat(taskInstanceQuery.count()).isEqualTo(5);

        deploymentIds = new ArrayList<>();
        deploymentIds.add("invalid");
        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().deploymentIdIn(deploymentIds);
        assertThat(taskInstanceQuery.count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml" })
    public void testHistoricTaskInstanceOrQueryByDeploymentId() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        for (int i = 0; i < 4; i++) {
            runtimeService.startProcessInstanceByKey("oneTaskProcess", String.valueOf(i));
        }
        runtimeService.startProcessInstanceByKey("oneTaskProcess2", "1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        HistoricTaskInstanceQuery taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().or().deploymentId(deployment.getId()).endOr();
        assertThat(taskInstanceQuery.count()).isEqualTo(5);

        List<HistoricTaskInstance> taskInstances = taskInstanceQuery.list();
        assertThat(taskInstances).hasSize(5);

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().or().deploymentId("invalid").endOr();
        assertThat(taskInstanceQuery.count()).isZero();

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().or().taskDefinitionKey("theTask").deploymentId("invalid").endOr();
        assertThat(taskInstanceQuery.count()).isEqualTo(5);

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().taskDefinitionKey("theTask").or().deploymentId("invalid").endOr();
        assertThat(taskInstanceQuery.count()).isZero();

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery()
                .or()
                .taskDefinitionKey("theTask")
                .deploymentId("invalid")
                .endOr()
                .or()
                .processDefinitionKey("oneTaskProcess")
                .processDefinitionId("invalid")
                .endOr();
        assertThat(taskInstanceQuery.count()).isEqualTo(4);

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery()
                .or()
                .taskDefinitionKey("theTask")
                .deploymentId("invalid")
                .endOr()
                .or()
                .processDefinitionKey("oneTaskProcess2")
                .processDefinitionId("invalid")
                .endOr();
        assertThat(taskInstanceQuery.count()).isEqualTo(1);

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery()
                .or()
                .taskDefinitionKey("theTask")
                .deploymentId("invalid")
                .endOr()
                .or()
                .processDefinitionKey("oneTaskProcess")
                .processDefinitionId("invalid")
                .endOr()
                .processInstanceBusinessKey("1");
        assertThat(taskInstanceQuery.count()).isEqualTo(1);

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery()
                .or()
                .taskDefinitionKey("theTask")
                .deploymentId("invalid")
                .endOr()
                .or()
                .processDefinitionKey("oneTaskProcess2")
                .processDefinitionId("invalid")
                .endOr()
                .processInstanceBusinessKey("1");
        assertThat(taskInstanceQuery.count()).isEqualTo(1);

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery()
                .or()
                .taskDefinitionKey("theTask")
                .deploymentId("invalid")
                .endOr()
                .or()
                .processDefinitionKey("oneTaskProcess2")
                .processDefinitionId("invalid")
                .endOr()
                .processInstanceBusinessKey("2");
        assertThat(taskInstanceQuery.count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml" })
    public void testHistoricTaskInstanceOrQueryByDeploymentIdIn() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        for (int i = 0; i < 4; i++) {
            runtimeService.startProcessInstanceByKey("oneTaskProcess", String.valueOf(i));
        }
        runtimeService.startProcessInstanceByKey("oneTaskProcess2", "1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        List<String> deploymentIds = new ArrayList<>();
        deploymentIds.add(deployment.getId());
        HistoricTaskInstanceQuery taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().or().deploymentIdIn(deploymentIds)
                .processDefinitionId("invalid").endOr();
        assertThat(taskInstanceQuery.count()).isEqualTo(5);

        List<HistoricTaskInstance> taskInstances = taskInstanceQuery.list();
        assertThat(taskInstances).hasSize(5);

        deploymentIds.add("invalid");
        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().or().deploymentIdIn(deploymentIds).processDefinitionId("invalid").endOr();
        assertThat(taskInstanceQuery.count()).isEqualTo(5);

        deploymentIds = new ArrayList<>();
        deploymentIds.add("invalid");
        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().or().deploymentIdIn(deploymentIds).processDefinitionId("invalid").endOr();
        assertThat(taskInstanceQuery.count()).isZero();

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().or().taskDefinitionKey("theTask").deploymentIdIn(deploymentIds).endOr();
        assertThat(taskInstanceQuery.count()).isEqualTo(5);

        taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().taskDefinitionKey("theTask").or().deploymentIdIn(deploymentIds).endOr();
        assertThat(taskInstanceQuery.count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testLocalizeTasks() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId())
                .list();
        assertThat(tasks)
                .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getDescription)
                .containsExactly(tuple("my task", null));

        ObjectNode infoNode = dynamicBpmnService.changeLocalizationName("en-GB", "theTask", "My localized name");
        dynamicBpmnService.changeLocalizationDescription("en-GB", "theTask", "My localized description", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
        tasks = historyService.createHistoricTaskInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list();
        assertThat(tasks)
                .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getDescription)
                .containsExactly(tuple("my task", null));

        tasks = historyService.createHistoricTaskInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("en-GB").list();
        assertThat(tasks)
                .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getDescription)
                .containsExactly(tuple("My localized name", "My localized description"));

        tasks = historyService.createHistoricTaskInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).listPage(0, 10);
        assertThat(tasks)
                .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getDescription)
                .containsExactly(tuple("my task", null));

        tasks = historyService.createHistoricTaskInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("en-GB").listPage(0, 10);
        assertThat(tasks)
                .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getDescription)
                .containsExactly(tuple("My localized name", "My localized description"));

        HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId())
                .singleResult();
        assertThat(task.getName()).isEqualTo("my task");
        assertThat(task.getDescription()).isNull();

        task = historyService.createHistoricTaskInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("en-GB").singleResult();
        assertThat(task.getName()).isEqualTo("My localized name");
        assertThat(task.getDescription()).isEqualTo("My localized description");

        task = historyService.createHistoricTaskInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");
        assertThat(task.getDescription()).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/concurrentExecution.bpmn20.xml" })
    public void testHistoricVariableInstancesOnParallelExecution() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("rootValue", "test");
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("concurrent", vars);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        for (org.flowable.task.api.Task task : tasks) {
            Map<String, Object> variables = new HashMap<>();
            // set token local variable
            LOGGER.debug("setting variables on task {}, execution {}", task.getId(), task.getExecutionId());
            runtimeService.setVariableLocal(task.getExecutionId(), "parallelValue1", task.getName());
            runtimeService.setVariableLocal(task.getExecutionId(), "parallelValue2", "test");
            taskService.complete(task.getId(), variables);
        }
        taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("rootValue", "test").count()).isEqualTo(1);

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("parallelValue1", "Receive Payment").count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("parallelValue1", "Ship Order").count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("parallelValue2", "test").count()).isEqualTo(1);
    }

    /**
     * basically copied from {@link ProcessInstanceQueryTest}
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryStringVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("stringVar", "abcdef");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult().getId());

        vars = new HashMap<>();
        vars.put("stringVar", "abcdef");
        vars.put("stringVar2", "ghijkl");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult().getId());

        vars = new HashMap<>();
        vars.put("stringVar", "azerty");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult().getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        // Test EQUAL on single string variable, should result in 2 matches
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().variableValueEquals("stringVar", "abcdef");
        List<HistoricProcessInstance> processInstances = query.list();
        assertThat(processInstances).hasSize(2);

        // Test EQUAL on two string variables, should result in single match
        query = historyService.createHistoricProcessInstanceQuery().variableValueEquals("stringVar", "abcdef").variableValueEquals("stringVar2", "ghijkl");
        HistoricProcessInstance resultInstance = query.singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

        // Test NOT_EQUAL, should return only 1 resultInstance
        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("stringVar", "abcdef").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN, should return only matching 'azerty'
        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("stringVar", "abcdef").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("stringVar", "z").singleResult();
        assertThat(resultInstance).isNull();

        // Test GREATER_THAN_OR_EQUAL, should return 3 results
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "abcdef").count()).isEqualTo(3);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "z").count()).isZero();

        // Test LESS_THAN, should return 2 results
        processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLessThan("stringVar", "abcdeg").list();
        assertThat(processInstances)
                .extracting(HistoricProcessInstance::getId)
                .containsOnly(processInstance1.getId(), processInstance2.getId());

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThan("stringVar", "abcdef").count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "z").count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "abcdef").list();
        assertThat(processInstances)
                .extracting(HistoricProcessInstance::getId)
                .containsOnly(processInstance1.getId(), processInstance2.getId());

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "z").count()).isEqualTo(3);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "aa").count()).isZero();

        // Test LIKE
        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueLike("stringVar", "azert%").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueLike("stringVar", "%y").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueLike("stringVar", "%zer%").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLike("stringVar", "a%").count()).isEqualTo(3);
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLike("stringVar", "%x%").count()).isZero();

        // Test value-only matching
        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueEquals("azerty").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        processInstances = historyService.createHistoricProcessInstanceQuery().variableValueEquals("abcdef").list();
        assertThat(processInstances)
                .extracting(HistoricProcessInstance::getId)
                .containsOnly(processInstance1.getId(), processInstance2.getId());

        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueEquals("notmatchinganyvalues").singleResult();
        assertThat(resultInstance).isNull();

        historyService.deleteHistoricProcessInstance(processInstance1.getId());
        historyService.deleteHistoricProcessInstance(processInstance2.getId());
        historyService.deleteHistoricProcessInstance(processInstance3.getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryEqualsIgnoreCase() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mixed", "AbCdEfG");
        vars.put("lower", "ABCDEFG");
        vars.put("upper", "abcdefg");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().variableValueEqualsIgnoreCase("mixed", "abcdefg").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        instance = historyService.createHistoricProcessInstanceQuery().variableValueEqualsIgnoreCase("lower", "abcdefg").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        instance = historyService.createHistoricProcessInstanceQuery().variableValueEqualsIgnoreCase("upper", "abcdefg").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        // Pass in non-lower-case string
        instance = historyService.createHistoricProcessInstanceQuery().variableValueEqualsIgnoreCase("upper", "ABCdefg").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        // Pass in null-value, should cause exception
        assertThatThrownBy(() -> historyService.createHistoricProcessInstanceQuery().variableValueEqualsIgnoreCase("upper", null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("value is null");

        // Pass in null name, should cause exception
        assertThatThrownBy(() -> historyService.createHistoricProcessInstanceQuery().variableValueEqualsIgnoreCase(null, "abcdefg").singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
    }

    /**
     * Only do one second type, as the logic is same as in {@link ProcessInstanceQueryTest} and I do not want to duplicate all test case logic here. Basically copied from
     * {@link ProcessInstanceQueryTest}
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryDateVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        Date date1 = Calendar.getInstance().getTime();
        vars.put("dateVar", date1);

        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult().getId());

        Date date2 = Calendar.getInstance().getTime();
        vars = new HashMap<>();
        vars.put("dateVar", date1);
        vars.put("dateVar2", date2);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult().getId());

        Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 1);
        vars = new HashMap<>();
        vars.put("dateVar", nextYear.getTime());
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult().getId());

        Calendar nextMonth = Calendar.getInstance();
        nextMonth.add(Calendar.MONTH, 1);

        Calendar twoYearsLater = Calendar.getInstance();
        twoYearsLater.add(Calendar.YEAR, 2);

        Calendar oneYearAgo = Calendar.getInstance();
        oneYearAgo.add(Calendar.YEAR, -1);

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        // Query on single short variable, should result in 2 matches
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().variableValueEquals("dateVar", date1);
        List<HistoricProcessInstance> processInstances = query.list();
        assertThat(processInstances).hasSize(2);

        // Query on two short variables, should result in single value
        query = historyService.createHistoricProcessInstanceQuery().variableValueEquals("dateVar", date1).variableValueEquals("dateVar2", date2);
        HistoricProcessInstance resultInstance = query.singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        Date unexistingDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/01/1989 12:00:00");
        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueEquals("dateVar", unexistingDate).singleResult();
        assertThat(resultInstance).isNull();

        // Test NOT_EQUALS
        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("dateVar", date1).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("dateVar", nextMonth.getTime()).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("dateVar", nextYear.getTime()).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", nextMonth.getTime()).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", nextYear.getTime()).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

        // Test LESS_THAN
        processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLessThan("dateVar", nextYear.getTime()).list();
        assertThat(processInstances)
                .extracting(HistoricProcessInstance::getId)
                .containsOnly(processInstance1.getId(), processInstance2.getId());

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThan("dateVar", date1).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThan("dateVar", twoYearsLater.getTime()).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("dateVar", nextYear.getTime()).list();
        assertThat(processInstances).hasSize(3);

        assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isZero();

        // Test value-only matching
        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueEquals(nextYear.getTime()).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        processInstances = historyService.createHistoricProcessInstanceQuery().variableValueEquals(date1).list();
        assertThat(processInstances)
                .extracting(HistoricProcessInstance::getId)
                .containsOnly(processInstance1.getId(), processInstance2.getId());

        resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueEquals(twoYearsLater.getTime()).singleResult();
        assertThat(resultInstance).isNull();

        historyService.deleteHistoricProcessInstance(processInstance1.getId());
        historyService.deleteHistoricProcessInstance(processInstance2.getId());
        historyService.deleteHistoricProcessInstance(processInstance3.getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testNativeHistoricProcessInstanceTest() {
        // just test that the query will be constructed and executed, details are tested in the TaskQueryTest
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
        assertThat(historyService.createNativeHistoricProcessInstanceQuery()
                .sql("SELECT count(*) FROM " + managementService.getTableName(HistoricProcessInstance.class)).count()).isEqualTo(1);
        assertThat(
                historyService.createNativeHistoricProcessInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class))
                        .list()).hasSize(1);
        // assertThat(historyService.createNativeHistoricProcessInstanceQuery().isEqualTo(1).sql("SELECT * FROM " +
        // managementService.getTableName(HistoricProcessInstance.class)).listPage(0, 1).size());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testNativeHistoricTaskInstanceTest() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
        assertThat(historyService.createNativeHistoricTaskInstanceQuery()
                .sql("SELECT count(*) FROM " + managementService.getTableName(HistoricProcessInstance.class)).count()).isEqualTo(1);
        assertThat(historyService.createNativeHistoricTaskInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class))
                .list()).hasSize(1);
        assertThat(historyService.createNativeHistoricTaskInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class))
                .listPage(0, 1)).hasSize(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testNativeHistoricActivityInstanceTest() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
        assertThat(historyService.createNativeHistoricActivityInstanceQuery()
                .sql("SELECT count(*) FROM " + managementService.getTableName(HistoricProcessInstance.class)).count()).isEqualTo(1);
        assertThat(
                historyService.createNativeHistoricActivityInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class))
                        .list()).hasSize(1);
        assertThat(
                historyService.createNativeHistoricActivityInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class))
                        .listPage(0, 1)).hasSize(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceQueryByProcessDefinitionName() {

        String processDefinitionKey = "oneTaskProcess";
        String processDefinitionName = "The One Task Process";
        runtimeService.startProcessInstanceByKey(processDefinitionKey);

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionName(processDefinitionName).list().get(0).getProcessDefinitionName())
                .isEqualTo(processDefinitionName);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionName(processDefinitionName).list()).hasSize(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionName(processDefinitionName).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionName("invalid").list()).isEmpty();
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionName("invalid").count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionName(processDefinitionName).processDefinitionId("invalid").endOr()
                .list().get(0).getProcessDefinitionName()).isEqualTo(processDefinitionName);
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionName(processDefinitionName).processDefinitionId("invalid").endOr()
                .list()).hasSize(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionName(processDefinitionName).processDefinitionId("invalid").endOr()
                .count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceQueryByProcessDefinitionCategory() {
        String processDefinitionKey = "oneTaskProcess";
        String processDefinitionCategory = "ExamplesCategory";
        runtimeService.startProcessInstanceByKey(processDefinitionKey);

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionCategory(processDefinitionCategory).list()).hasSize(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionCategory(processDefinitionCategory).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionCategory("invalid").list()).isEmpty();
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionCategory("invalid").count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionCategory(processDefinitionCategory).processDefinitionId("invalid")
                .endOr().list()).hasSize(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionCategory(processDefinitionCategory).processDefinitionId("invalid")
                .endOr().count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricIdentityLinksForProcessInstance() {
        Date processInstanceStartTime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(processInstanceStartTime);
        identityService.setAuthenticatedUserId("johndoe");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        List<HistoricIdentityLink> historicIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getProcessInstanceId());

        assertThat(historicIdentityLinks).hasSize(1);

        HistoricIdentityLink historicIdentityLink = historicIdentityLinks.get(0);
        assertThat(historicIdentityLink.getType()).isEqualTo(IdentityLinkType.STARTER);
        assertThat(historicIdentityLink.getUserId()).isEqualTo("johndoe");
        assertThat(historicIdentityLink.getCreateTime()).isNotNull();

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
        assertThat(historicProcessInstance.getStartUserId()).isEqualTo("johndoe");
        assertThat(historicProcessInstance.getStartActivityId()).isEqualTo("theStart");

        Date taskCompleteTime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(taskCompleteTime);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        taskService.complete(tasks.get(0).getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        historicIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getProcessInstanceId());

        assertThat(historicIdentityLinks).hasSize(2);

        historicIdentityLinks.sort(Comparator.comparing(HistoricIdentityLink::getCreateTime));

        historicIdentityLink = historicIdentityLinks.get(1);
        assertThat(historicIdentityLink.getType()).isEqualTo(IdentityLinkType.PARTICIPANT);
        assertThat(historicIdentityLink.getUserId()).isEqualTo("johndoe");
        assertThat(historicIdentityLink.getCreateTime()).isNotNull();

        managementService.executeCommand(commandContext -> {
            processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService()
                .deleteHistoricTaskLogEntriesForProcessDefinition(processInstance.getProcessDefinitionId());
            return null;
        });
    }

}
