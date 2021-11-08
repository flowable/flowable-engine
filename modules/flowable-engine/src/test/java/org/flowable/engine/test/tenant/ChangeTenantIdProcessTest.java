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
package org.flowable.engine.test.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableChangeTenantIdEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.BpmnChangeTenantIdEntityTypes;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeTenantIdProcessTest extends PluggableFlowableTestCase {

    private static final String TEST_TENANT_A = "test-tenant-a";
    private static final String TEST_TENANT_B = "test-tenant-b";
    private static final String TEST_TENANT_C = "test-tenant-c";

    protected TestEventListener eventListener = new TestEventListener();

    @BeforeEach
    void setUp() {
        deploymentIdsForAutoCleanup.add(repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/tenant/testProcess.bpmn20.xml").tenantId(TEST_TENANT_A)
                .deploy().getId());
        deploymentIdsForAutoCleanup.add(repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/tenant/testProcess.bpmn20.xml").tenantId(TEST_TENANT_B)
                .deploy().getId());
        deploymentIdsForAutoCleanup.add(repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/tenant/testProcess.bpmn20.xml").tenantId(TEST_TENANT_C)
                .deploy().getId());
        deploymentIdsForAutoCleanup.add(repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/tenant/testProcessDup.bpmn20.xml").deploy().getId());
        deploymentIdsForAutoCleanup.add(repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/tenant/testProcessForJobsAndEventSubscriptions.bpmn20.xml").tenantId(TEST_TENANT_A)
                .deploy().getId());

        processEngineConfiguration.getEventDispatcher().addEventListener(eventListener);
    }

    @AfterEach
    void cleanUp() {
        processEngineConfiguration.getEventDispatcher().removeEventListener(eventListener);
    }

    @Test
    void testChangeTenantIdProcessInstance() {
        //testDeployments() {
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(5);
        assertThat(repositoryService.createDeploymentQuery().deploymentWithoutTenantId().count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_A).count()).isEqualTo(2);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_B).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_C).count()).isEqualTo(1);

        //Starting process instances that will be completed
        String processInstanceIdACompleted = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdACompleted", 2);
        String processInstanceIdBCompleted = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBCompleted", 2);
        String processInstanceIdCCompleted = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCCompleted", 2);

        //Starting process instances that will remain active and moving jobs to different states
        String processInstanceIdAActive = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdAActive", 1);
        String processInstanceIdBActive = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBActive", 1);
        String processInstanceIdCActive = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCActive", 1);
        String processInstanceIdAAForJobs = startProcess(TEST_TENANT_A, "testProcessForJobsAndEventSubscriptions", "processInstanceIdAAForJobs", 0);
        Job jobToBeSentToDeadLetter = managementService.createTimerJobQuery().processInstanceId(processInstanceIdAAForJobs)
                .elementName("Timer to create a deadletter job").singleResult();
        Job jobInTheDeadLetterQueue = managementService.moveJobToDeadLetterJob(jobToBeSentToDeadLetter.getId());
        assertThat(jobInTheDeadLetterQueue).as("We have a job in the deadletter queue.").isNotNull();
        String processInstanceIdAForSuspendedJobs = startProcess(TEST_TENANT_A, "testProcessForJobsAndEventSubscriptions", "processInstanceIdAAForJobsActive",
                0);
        runtimeService.suspendProcessInstanceById(processInstanceIdAForSuspendedJobs);
        Job aSuspendedJob = managementService.createSuspendedJobQuery().processInstanceId(processInstanceIdAForSuspendedJobs)
                .elementName("Timer to create a suspended job").singleResult();
        assertThat(aSuspendedJob).as("We have a suspended job.").isNotNull();

        Set<String> processInstancesTenantA = new HashSet<>(
                Arrays.asList(processInstanceIdACompleted, processInstanceIdAActive, processInstanceIdAAForJobs, processInstanceIdAForSuspendedJobs));
        Set<String> processInstancesTenantB = new HashSet<>(Arrays.asList(processInstanceIdBCompleted, processInstanceIdBActive));
        Set<String> processInstancesTenantC = new HashSet<>(Arrays.asList(processInstanceIdCCompleted, processInstanceIdCActive));

        // Prior to changing the Tenant Id, all elements are associated to the original tenant
        checkTenantIdForAllInstances(processInstancesTenantA, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First, we simulate the change
        ChangeTenantIdResult simulationResult = managementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkTenantIdForAllInstances(processInstancesTenantA, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = managementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).complete();

        // All the instances should now be assigned to the tenant B
        checkTenantIdForAllInstances(processInstancesTenantA, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = new HashMap<>();
        resultMap.put(BpmnChangeTenantIdEntityTypes.ACTIVITY_INSTANCES, 35L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.EXECUTIONS, 16L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS, 2L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.TASKS, 1L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS, 1L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_ACTIVITY_INSTANCES, 44L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_PROCESS_INSTANCES, 4L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_TASK_LOG_ENTRIES, 7L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_TASK_INSTANCES, 4L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.JOBS, 1L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.SUSPENDED_JOBS, 5L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.TIMER_JOBS, 2L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.DEADLETTER_JOBS, 1L);

        //Check that all the entities are returned
        assertThat(simulationResult.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());
        assertThat(result.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());

        resultMap.forEach((key, value) -> {
            //Check simulation result content
            assertThat(simulationResult.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);

            //Check result content
            assertThat(result.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);
        });

        //Check that we can complete the active instances that we have changed
        completeTask(processInstanceIdAActive);
        assertProcessEnded(processInstanceIdAActive);
        completeTask(processInstanceIdBActive);
        assertProcessEnded(processInstanceIdBActive);
        completeTask(processInstanceIdCActive);
        assertProcessEnded(processInstanceIdCActive);

        assertThat(eventListener.events).hasSize(1);
        FlowableChangeTenantIdEvent event = eventListener.events.get(0);
        assertThat(event.getEngineScopeType()).isEqualTo(ScopeTypes.BPMN);
        assertThat(event.getSourceTenantId()).isEqualTo(TEST_TENANT_A);
        assertThat(event.getTargetTenantId()).isEqualTo(TEST_TENANT_B);
        assertThat(event.getDefinitionTenantId()).isNull();
    }

    @Test
    void testChangeTenantIdProcessInstanceFromEmptyTenant() {
        //testDeployments() {
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(5);
        assertThat(repositoryService.createDeploymentQuery().deploymentWithoutTenantId().count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_A).count()).isEqualTo(2);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_B).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_C).count()).isEqualTo(1);

        //Starting process instances that will be completed
        String processInstanceIdNoTenantCompleted = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdNoTenantCompleted", 2, "");
        String processInstanceIdBCompleted = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBCompleted", 2);
        String processInstanceIdCCompleted = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCCompleted", 2);

        //Starting process instances that will remain active and moving jobs to different states
        String processInstanceIdNoTenantActive = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdNoTenantActive", 1, "");
        String processInstanceIdBActive = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBActive", 1);
        String processInstanceIdCActive = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCActive", 1);
        String processInstanceIdNoTenantForJobs = startProcess(TEST_TENANT_A, "testProcessForJobsAndEventSubscriptions", "processInstanceIdNoTenantForJobs", 0,
                "");
        Job jobToBeSentToDeadLetter = managementService.createTimerJobQuery().processInstanceId(processInstanceIdNoTenantForJobs)
                .elementName("Timer to create a deadletter job").singleResult();
        Job jobInTheDeadLetterQueue = managementService.moveJobToDeadLetterJob(jobToBeSentToDeadLetter.getId());
        assertThat(jobInTheDeadLetterQueue).as("We have a job in the deadletter queue.").isNotNull();
        String processInstanceIdNoTenantForSuspendedJobs = startProcess(TEST_TENANT_A, "testProcessForJobsAndEventSubscriptions",
                "processInstanceIdAAForJobsActive", 0, "");
        runtimeService.suspendProcessInstanceById(processInstanceIdNoTenantForSuspendedJobs);
        Job aSuspendedJob = managementService.createSuspendedJobQuery().processInstanceId(processInstanceIdNoTenantForSuspendedJobs)
                .elementName("Timer to create a suspended job").singleResult();
        assertThat(aSuspendedJob).as("We have a suspended job.").isNotNull();

        Set<String> processInstancesNoTenant = new HashSet<>(
                Arrays.asList(processInstanceIdNoTenantCompleted, processInstanceIdNoTenantActive, processInstanceIdNoTenantForJobs,
                        processInstanceIdNoTenantForSuspendedJobs));
        Set<String> processInstancesTenantB = new HashSet<>(Arrays.asList(processInstanceIdBCompleted, processInstanceIdBActive));
        Set<String> processInstancesTenantC = new HashSet<>(Arrays.asList(processInstanceIdCCompleted, processInstanceIdCActive));

        // Prior to changing the Tenant Id, all elements are associated to the original tenant
        checkTenantIdForAllInstances(processInstancesNoTenant, "", "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First, we simulate the change
        ChangeTenantIdResult simulationResult = managementService
                .createChangeTenantIdBuilder("", TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkTenantIdForAllInstances(processInstancesNoTenant, "", "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = managementService
                .createChangeTenantIdBuilder("", TEST_TENANT_B).complete();

        // All the instances should now be assigned to the tenant B
        checkTenantIdForAllInstances(processInstancesNoTenant, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = new HashMap<>();
        resultMap.put(BpmnChangeTenantIdEntityTypes.ACTIVITY_INSTANCES, 35L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.EXECUTIONS, 16L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS, 2L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.TASKS, 1L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS, 1L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_ACTIVITY_INSTANCES, 44L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_PROCESS_INSTANCES, 4L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_TASK_LOG_ENTRIES, 7L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_TASK_INSTANCES, 4L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.JOBS, 1L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.SUSPENDED_JOBS, 5L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.TIMER_JOBS, 2L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.DEADLETTER_JOBS, 1L);

        //Check that all the entities are returned
        assertThat(simulationResult.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());
        assertThat(result.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());

        resultMap.forEach((key, value) -> {
            //Check simulation result content
            assertThat(simulationResult.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);

            //Check result content
            assertThat(result.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);
        });

        //Check that we can complete the active instances that we have changed
        completeTask(processInstanceIdNoTenantActive);
        assertProcessEnded(processInstanceIdNoTenantActive);
        completeTask(processInstanceIdBActive);
        assertProcessEnded(processInstanceIdBActive);
        completeTask(processInstanceIdCActive);
        assertProcessEnded(processInstanceIdCActive);

        assertThat(eventListener.events).hasSize(1);
        FlowableChangeTenantIdEvent event = eventListener.events.get(0);
        assertThat(event.getEngineScopeType()).isEqualTo(ScopeTypes.BPMN);
        assertThat(event.getSourceTenantId()).isEqualTo("");
        assertThat(event.getTargetTenantId()).isEqualTo(TEST_TENANT_B);
        assertThat(event.getDefinitionTenantId()).isNull();
    }

    private void checkTenantIdForAllInstances(Set<String> processInstanceIds, String expectedTenantId, String moment) {
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceIds(processInstanceIds).list())
                .isNotEmpty()
                .allSatisfy(pi -> {
                    assertThat(StringUtils.defaultIfEmpty(pi.getTenantId(), ""))
                            .as("Active process instance '%s' %s must belong to %s but belongs to %s.",
                                    pi.getName(), moment, expectedTenantId, pi.getTenantId())
                            .isEqualTo(expectedTenantId);

                    assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(pi.getId()).list())
                            .isNotEmpty()
                            .allSatisfy(ai -> assertThat(StringUtils.defaultIfEmpty(ai.getTenantId(), ""))
                                    .as("Active activity instance %s from %s %s must belong to %s but belongs to %s.",
                                            ai.getActivityName(), pi.getName(), moment, expectedTenantId, ai.getTenantId())
                                    .isEqualTo(expectedTenantId));

                    assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).list())
                            .isNotEmpty()
                            .allSatisfy(ex -> assertThat(StringUtils.defaultIfEmpty(ex.getTenantId(), ""))
                                    .as("Execution %s from %s %s must belong to %s but belongs to %s.",
                                            ex.getName(), pi.getName(), moment, expectedTenantId, ex.getTenantId())
                                    .isEqualTo(expectedTenantId));

                    assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(pi.getId()).list())
                            .allSatisfy(eve -> assertThat(StringUtils.defaultIfEmpty(eve.getTenantId(), ""))
                                    .as("Event Subscription %s from %s %s must belong to %s but belongs to %s.",
                                            eve.getId(), pi.getName(), moment, expectedTenantId, eve.getTenantId())
                                    .isEqualTo(expectedTenantId));

                    assertThat(taskService.createTaskQuery().processInstanceId(pi.getId()).list())
                            .allSatisfy(task -> assertThat(StringUtils.defaultIfEmpty(task.getTenantId(), ""))
                                    .as("Task %s from %s %s must belong to %s but belongs to %s.",
                                            task.getName(), pi.getName(), moment, expectedTenantId, task.getTenantId())
                                    .isEqualTo(expectedTenantId));

                    assertThat(managementService.createJobQuery().processInstanceId(pi.getId()).list())
                            .allSatisfy(job -> assertThat(StringUtils.defaultIfEmpty(job.getTenantId(), ""))
                                    .as("Job %s from %s %s must belong to %s but belongs to %s.",
                                            job.getId(), pi.getName(), moment, expectedTenantId, job.getTenantId())
                                    .isEqualTo(expectedTenantId));

                    assertThat(managementService.createDeadLetterJobQuery().processInstanceId(pi.getId()).list())
                            .allSatisfy(job -> assertThat(StringUtils.defaultIfEmpty(job.getTenantId(), ""))
                                    .as("Dead Letter Job %s from %s %s must belong to %s but belongs to %s.",
                                            job.getId(), pi.getName(), moment, expectedTenantId, job.getTenantId())
                                    .isEqualTo(expectedTenantId));

                    assertThat(managementService.createTimerJobQuery().processInstanceId(pi.getId()).list())
                            .allSatisfy(job -> assertThat(StringUtils.defaultIfEmpty(job.getTenantId(), ""))
                                    .as("Timer Job %s from %s %s must belong to %s but belongs to %s.",
                                            job.getId(), pi.getName(), moment, expectedTenantId, job.getTenantId())
                                    .isEqualTo(expectedTenantId));

                    assertThat(managementService.createExternalWorkerJobQuery().processInstanceId(pi.getId()).list())
                            .allSatisfy(job -> assertThat(StringUtils.defaultIfEmpty(job.getTenantId(), ""))
                                    .as("External Worker Job %s from %s %s must belong to %s but belongs to %s.",
                                            job.getId(), pi.getName(), moment, expectedTenantId, job.getTenantId())
                                    .isEqualTo(expectedTenantId));
                });

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceIds(processInstanceIds).list())
                    .hasSize(processInstanceIds.size())
                    .allSatisfy(hpi -> {
                        assertThat(StringUtils.defaultIfEmpty(hpi.getTenantId(), ""))
                                .as("Historic process instance '%s' %s must belong to %s but belongs to %s.",
                                        hpi.getName(), moment, expectedTenantId, hpi.getTenantId())
                                .isEqualTo(expectedTenantId);

                        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(hpi.getId()).list())
                                .isNotEmpty()
                                .allSatisfy(hai -> assertThat(StringUtils.defaultIfEmpty(hai.getTenantId(), ""))
                                        .as("Historic activity instance %s from %s %s must belong to %s but belongs to %s.",
                                                hai.getActivityName(), hpi.getName(), moment, expectedTenantId, hai.getTenantId())
                                        .isEqualTo(expectedTenantId));

                        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(hpi.getId()).list())
                                .allSatisfy(task -> assertThat(StringUtils.defaultIfEmpty(task.getTenantId(), ""))
                                        .as("Historic Task %s from %s %s must belong to %s but belongs to %s.",
                                                task.getName(), hpi.getName(), moment, expectedTenantId, task.getTenantId())
                                        .isEqualTo(expectedTenantId));

                        assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(hpi.getId()).list())
                                .allSatisfy(log -> assertThat(StringUtils.defaultIfEmpty(log.getTenantId(), ""))
                                        .as("Historic Task Log Entry %s from %s %s must belong to %s but belongs to %s.",
                                                log.getLogNumber(), hpi.getName(), moment, expectedTenantId, log.getTenantId())
                                        .isEqualTo(expectedTenantId));
                    });
        }

    }

    @Test
    void changeTenantIdWithDefinedDefinitionTenant() {

        //Starting process instances that will be completed
        String processInstanceIdACompleted = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdACompleted", 2);
        String processInstanceIdADTCompleted = startProcess(TEST_TENANT_A, "testProcessDup", "processInstanceIdADTCompleted", 2,
                TEST_TENANT_A); // For this instance we want to override the tenant Id.
        String processInstanceIdBCompleted = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBCompleted", 2);
        String processInstanceIdCCompleted = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCCompleted", 2);

        //Starting process instances that will remain active
        String processInstanceIdAActive = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdAActive", 1);
        String processInstanceIdADTActive = startProcess(TEST_TENANT_A, "testProcessDup", "processInstanceIdADTActive", 1,
                TEST_TENANT_A); // For this instance we want to override the tenant Id.
        String processInstanceIdBActive = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBActive", 1);
        String processInstanceIdCActive = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCActive", 1);

        Set<String> processInstancesTenantADTOnly = new HashSet<>(Arrays.asList(processInstanceIdADTCompleted, processInstanceIdADTActive));
        Set<String> processInstancesTenantANonDT = new HashSet<>(Arrays.asList(processInstanceIdACompleted, processInstanceIdAActive));
        Set<String> processInstancesTenantAAll = new HashSet<>(
                Arrays.asList(processInstanceIdACompleted, processInstanceIdAActive, processInstanceIdADTCompleted, processInstanceIdADTActive));
        Set<String> processInstancesTenantB = new HashSet<>(Arrays.asList(processInstanceIdBCompleted, processInstanceIdBActive));
        Set<String> processInstancesTenantC = new HashSet<>(Arrays.asList(processInstanceIdCCompleted, processInstanceIdCActive));

        // Prior to changing the Tenant Id, all elements are associated to the original tenant
        checkTenantIdForAllInstances(processInstancesTenantAAll, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First, we simulate the change
        ChangeTenantIdResult simulationResult = managementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .definitionTenantId("")
                .simulate();

        // All the instances should stay in the original tenant after the simulation
        checkTenantIdForAllInstances(processInstancesTenantAAll, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = managementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .definitionTenantId("")
                .complete();

        // All the instances from the default tenant should now be assigned to the tenant B
        checkTenantIdForAllInstances(processInstancesTenantADTOnly, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // But the instances that were created with a definition from tenant A must stay in tenant A
        checkTenantIdForAllInstances(processInstancesTenantANonDT, TEST_TENANT_A, "after the change to " + TEST_TENANT_B);

        // The instances from Tenant B are still associated to tenant B
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched in tenant C
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = new HashMap<>();
        resultMap.put(BpmnChangeTenantIdEntityTypes.ACTIVITY_INSTANCES, 7L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.EXECUTIONS, 2L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS, 0L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.TASKS, 1L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS, 0L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_ACTIVITY_INSTANCES, 16L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_PROCESS_INSTANCES, 2L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_TASK_LOG_ENTRIES, 7L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.HISTORIC_TASK_INSTANCES, 4L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.JOBS, 0L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.SUSPENDED_JOBS, 0L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.TIMER_JOBS, 0L);
        resultMap.put(BpmnChangeTenantIdEntityTypes.DEADLETTER_JOBS, 0L);

        //Check that all the entities are returned
        assertThat(simulationResult.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());
        assertThat(result.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());

        resultMap.forEach((key, value) -> {
            //Check simulation result content
            assertThat(simulationResult.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);

            //Check result content
            assertThat(result.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);
        });

        //Check that we can complete the active instances that we have changed
        completeTask(processInstanceIdAActive);
        assertProcessEnded(processInstanceIdAActive);
        completeTask(processInstanceIdADTActive);
        assertProcessEnded(processInstanceIdADTActive);
        completeTask(processInstanceIdBActive);
        assertProcessEnded(processInstanceIdBActive);
        completeTask(processInstanceIdCActive);
        assertProcessEnded(processInstanceIdCActive);

        assertThat(eventListener.events).hasSize(1);
        FlowableChangeTenantIdEvent event = eventListener.events.get(0);
        assertThat(event.getEngineScopeType()).isEqualTo(ScopeTypes.BPMN);
        assertThat(event.getSourceTenantId()).isEqualTo(TEST_TENANT_A);
        assertThat(event.getTargetTenantId()).isEqualTo(TEST_TENANT_B);
        assertThat(event.getDefinitionTenantId()).isEqualTo("");

    }

    @Test
    void changeTenantIdWhenTenantsAreInvalid() {
        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_A).simulate())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The source and the target tenant ids must be different.");
        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_A).complete())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The source and the target tenant ids must be different.");

        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(null, TEST_TENANT_A).simulate())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The source tenant id must not be null.");
        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(null, TEST_TENANT_A).complete())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The source tenant id must not be null.");

        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(TEST_TENANT_A, null).simulate())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The target tenant id must not be null.");
        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(TEST_TENANT_A, null).complete())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The target tenant id must not be null.");

        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).definitionTenantId(null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("definitionTenantId must not be null");
    }

    private String startProcess(String tenantId, String processDefinitionKey, String processInstanceName, int completeTaskLoops) {
        return startProcess(tenantId, processDefinitionKey, processInstanceName, completeTaskLoops, null);
    }

    private String startProcess(String tenantId, String processDefinitionKey, String processInstanceName, int completeTaskLoops, String overrideTenantId) {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder().processDefinitionKey(processDefinitionKey)
                .name(processInstanceName).tenantId(tenantId).fallbackToDefaultTenant();
        if (overrideTenantId != null) {
            processInstanceBuilder.overrideProcessDefinitionTenantId(overrideTenantId);
        }
        ProcessInstance processInstance = processInstanceBuilder.start();
        for (int i = 0; i < completeTaskLoops; i++) {
            completeTask(processInstance);
        }
        return processInstance.getId();
    }

    private void completeTask(ProcessInstance processInstance) {
        completeTask(processInstance.getId());
    }

    private void completeTask(String processInstanceId) {
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        taskService.complete(task.getId());
    }

    protected static class TestEventListener extends AbstractFlowableEventListener {

        protected final List<FlowableChangeTenantIdEvent> events = new ArrayList<>();

        @Override
        public void onEvent(FlowableEvent event) {
            if (event instanceof FlowableChangeTenantIdEvent) {
                events.add((FlowableChangeTenantIdEvent) event);
            }
        }

        @Override
        public boolean isFailOnException() {
            return true;
        }

        @Override
        public Collection<? extends FlowableEventType> getTypes() {
            return Collections.singleton(FlowableEngineEventType.CHANGE_TENANT_ID);
        }
    }
}
