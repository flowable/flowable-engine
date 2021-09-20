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
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.*;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ChangeTenantIdProcessTest  extends PluggableFlowableTestCase {

    private static final String TEST_TENANT_A = "test-tenant-a";
    private static final String TEST_TENANT_B = "test-tenant-b";
    private static final String TEST_TENANT_C = "test-tenant-c";
        
    private String deploymentIdWithTenantA;
    private String deploymentIdWithTenantB;
    private String deploymentIdWithTenantC;
    private String deploymentIdWithoutTenant;
    private String deploymentIdWithTenantAForJobs;

    @BeforeEach
    public void setUp() {
        this.deploymentIdWithTenantA = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/tenant/testProcess.bpmn20.xml").tenantId(TEST_TENANT_A)
                .deploy().getId();
        this.deploymentIdWithTenantB = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/tenant/testProcess.bpmn20.xml").tenantId(TEST_TENANT_B)
                .deploy().getId();
        this.deploymentIdWithTenantC = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/tenant/testProcess.bpmn20.xml").tenantId(TEST_TENANT_C)
                .deploy().getId();
        this.deploymentIdWithoutTenant = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/tenant/testProcessDup.bpmn20.xml").deploy().getId();
        this.deploymentIdWithTenantAForJobs = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/tenant/testProcessForJobsAndEventSubscriptions.bpmn20.xml").tenantId(TEST_TENANT_A)
                .deploy().getId();
    }

    @AfterEach
    public void cleanUp() {
        repositoryService.deleteDeployment(deploymentIdWithTenantA,true);
        repositoryService.deleteDeployment(deploymentIdWithTenantB,true);
        repositoryService.deleteDeployment(deploymentIdWithTenantC,true);
        repositoryService.deleteDeployment(deploymentIdWithoutTenant,true);
        repositoryService.deleteDeployment(deploymentIdWithTenantAForJobs, true);
    }

    @Test
    public void testChangeTenantIdProcessInstance() {
        //testDeployments() {
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(5);
        assertThat(repositoryService.createDeploymentQuery().deploymentWithoutTenantId().count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_A).count()).isEqualTo(2);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_B).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_C).count()).isEqualTo(1);
        

        //Starting process instances that will be completed
        String processInstanceIdACompleted = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdACompleted", 2, false);
        String processInstanceIdBCompleted = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBCompleted", 2, false);
        String processInstanceIdCCompleted = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCCompleted", 2, false);
        
        
        //Starting process instances that will remain active and moving jobs to different states
        String processInstanceIdAActive = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdAActive", 1, false);
        String processInstanceIdBActive = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBActive", 1, false);
        String processInstanceIdCActive = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCActive", 1, false);
        String processInstanceIdAAForJobs = startProcess(TEST_TENANT_A, "testProcessForJobsAndEventSubscriptions", "processInstanceIdAAForJobs", 0, false);
        Job jobToBeSentToDeadLetter = managementService.createTimerJobQuery().processInstanceId(processInstanceIdAAForJobs).elementName("Timer to create a deadletter job").singleResult();
        Job jobInTheDeadLetterQueue = managementService.moveJobToDeadLetterJob(jobToBeSentToDeadLetter.getId());
        assertThat(jobInTheDeadLetterQueue).as("We have a job in the deadletter queue.").isNotNull();
        String processInstanceIdAForSuspendedJobs = startProcess(TEST_TENANT_A, "testProcessForJobsAndEventSubscriptions", "processInstanceIdAAForJobsActive", 0, false);
        runtimeService.suspendProcessInstanceById(processInstanceIdAForSuspendedJobs);
        Job aSuspendedJob = managementService.createSuspendedJobQuery().processInstanceId(processInstanceIdAForSuspendedJobs).elementName("Timer to create a suspended job").singleResult();
        assertThat(aSuspendedJob).as("We have a suspended job.").isNotNull();

        Set<String> processInstancesTenantA = new HashSet<>(Arrays.asList(processInstanceIdACompleted, processInstanceIdAActive, processInstanceIdAAForJobs, processInstanceIdAForSuspendedJobs));
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

        // The simulation result must match the actual result
        assertThat(simulationResult).as("The simulation result must match the actual result.").isEqualTo(result);
        
        //Expected results map
        Map<String, Long> resultMap = new HashMap<>();
        resultMap.put(ACTIVITY_INSTANCES, 35L);
        resultMap.put(EXECUTIONS, 16L);
        resultMap.put(EVENT_SUBSCRIPTIONS, 2L);
        resultMap.put(TASKS, 1L);
        resultMap.put(EXTERNAL_WORKER_JOBS, 1L);
        resultMap.put(HISTORIC_ACTIVITY_INSTANCES, 44L);
        resultMap.put(HISTORIC_PROCESS_INSTANCES, 4L);
        resultMap.put(HISTORIC_TASK_LOG_ENTRIES, 7L);
        resultMap.put(HISTORIC_TASK_INSTANCES, 4L);
        resultMap.put(HISTORY_JOBS, 0L);
        resultMap.put(JOBS, 1L);
        resultMap.put(SUSPENDED_JOBS, 5L);
        resultMap.put(TIMER_JOBS, 2L);
        resultMap.put(DEADLETTER_JOBS, 1L);

        //Check that all the entities are returned
        simulationResult.getChangedEntityTypes().containsAll(resultMap.keySet());
        result.getChangedEntityTypes().containsAll(resultMap.keySet());
        
        //Check simulation result content
        resultMap.entrySet().forEach(e -> assertThat(simulationResult.getChangedInstances(e.getKey())).isEqualTo(e.getValue()));
        
        //Check result content
        resultMap.entrySet().forEach(e -> assertThat(result.getChangedInstances(e.getKey())).isEqualTo(e.getValue()));

        //Check that we can complete the active instances that we have changed
        completeTask(processInstanceIdAActive);
        assertProcessEnded(processInstanceIdAActive);
        completeTask(processInstanceIdBActive);
        assertProcessEnded(processInstanceIdBActive);
        completeTask(processInstanceIdCActive);
        assertProcessEnded(processInstanceIdCActive);
    }

    private void checkTenantIdForAllInstances(Set<String> processInstanceIds, String expectedTenantId, String moment) {
        runtimeService.createProcessInstanceQuery().processInstanceIds(processInstanceIds).list().forEach(pi -> {
            assertThat(pi.getTenantId())
                .as("Active process instance '%s' %s must belong to %s but belongs to %s.", 
                pi.getName(), moment, expectedTenantId, pi.getTenantId())
                .isEqualTo(expectedTenantId);

            runtimeService.createActivityInstanceQuery().processInstanceId(pi.getId()).list()
            .forEach(ai -> assertThat(ai.getTenantId())
                .as("Active activity instance %s from %s %s must belong to %s but belongs to %s.", 
                ai.getActivityName(), pi.getName(), moment, expectedTenantId, ai.getTenantId())
                .isEqualTo(expectedTenantId));
            
            runtimeService.createExecutionQuery().processInstanceId(pi.getId()).list()
            .forEach(ex -> assertThat(ex.getTenantId())
                .as("Execution %s from %s %s must belong to %s but belongs to %s.", 
                ex.getName(), pi.getName(), moment, expectedTenantId, ex.getTenantId())
                .isEqualTo(expectedTenantId));
        });

        historyService.createHistoricProcessInstanceQuery().processInstanceIds(processInstanceIds).list().forEach(hpi -> {
            assertThat(hpi.getTenantId())
            .as("Historic process instance '%s' %s must belong to %s but belongs to %s.", 
            hpi.getName(), moment, expectedTenantId, hpi.getTenantId())
            .isEqualTo(expectedTenantId);

            historyService.createHistoricActivityInstanceQuery().processInstanceId(hpi.getId()).list()
            .forEach(hai -> assertThat(hai.getTenantId())
            .as("Historic activity instance %s from %s %s must belong to %s but belongs to %s.", 
            hai.getActivityName(), hpi.getName(), moment, expectedTenantId, hai.getTenantId())
            .isEqualTo(expectedTenantId));
        });

    }

    @Test
    public void testChangeTenantIdProcessInstance_onlyDefaultTenantDefinitionInstances() {

        //Starting process instances that will be completed
        String processInstanceIdACompleted = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdACompleted", 2, false);
        String processInstanceIdADTCompleted = startProcess(TEST_TENANT_A, "testProcessDup", "processInstanceIdADTCompleted", 2, true); // For this instance we want to override the tenant Id.
        String processInstanceIdBCompleted = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBCompleted", 2, false);
        String processInstanceIdCCompleted = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCCompleted", 2, false);
        
        //Starting process instances that will remain active
        String processInstanceIdAActive = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdAActive", 1, false);
        String processInstanceIdADTActive = startProcess(TEST_TENANT_A, "testProcessDup", "processInstanceIdADTActive", 1, true); // For this instance we want to override the tenant Id.
        String processInstanceIdBActive = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBActive", 1, false);
        String processInstanceIdCActive = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCActive", 1, false);

        Set<String> processInstancesTenantADTOnly = new HashSet<>(Arrays.asList(processInstanceIdADTCompleted, processInstanceIdADTActive));
        Set<String> processInstancesTenantANonDT = new HashSet<>(Arrays.asList(processInstanceIdACompleted, processInstanceIdAActive));
        Set<String> processInstancesTenantAAll = new HashSet<>(Arrays.asList(processInstanceIdACompleted, processInstanceIdAActive, processInstanceIdADTCompleted, processInstanceIdADTActive));
        Set<String> processInstancesTenantB = new HashSet<>(Arrays.asList(processInstanceIdBCompleted, processInstanceIdBActive));
        Set<String> processInstancesTenantC = new HashSet<>(Arrays.asList(processInstanceIdCCompleted, processInstanceIdCActive));

        // Prior to changing the Tenant Id, all elements are associated to the original tenant
        checkTenantIdForAllInstances(processInstancesTenantAAll, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);
        
        // First, we simulate the change
        ChangeTenantIdResult simulationResult = managementService
        .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
        .onlyInstancesFromDefaultTenantDefinitions()
        .simulate();
        
        // All the instances should stay in the original tenant after the simulation
        checkTenantIdForAllInstances(processInstancesTenantAAll, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = managementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .onlyInstancesFromDefaultTenantDefinitions()
                .complete();

        // All the instances from the default tenant should now be assigned to the tenant B
        checkTenantIdForAllInstances(processInstancesTenantADTOnly, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // But the instances that were created with a definition from tenant A must stay in tenant A
        checkTenantIdForAllInstances(processInstancesTenantANonDT, TEST_TENANT_A, "after the change to " + TEST_TENANT_B);

        // The instances from Tenant B are still associated to tenant B
        checkTenantIdForAllInstances(processInstancesTenantB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched in tenant C
        checkTenantIdForAllInstances(processInstancesTenantC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        // The simulation result must match the actual result
        assertThat(simulationResult).isEqualTo(result).as("The simulation result must match the actual result.");

        //Check that we can complete the active instances that we have changed
        completeTask(processInstanceIdAActive);
        assertProcessEnded(processInstanceIdAActive);
        completeTask(processInstanceIdADTActive);
        assertProcessEnded(processInstanceIdADTActive);
        completeTask(processInstanceIdBActive);
        assertProcessEnded(processInstanceIdBActive);
        completeTask(processInstanceIdCActive);
        assertProcessEnded(processInstanceIdCActive);
        
    }

    @Test
    public void testChangeTenantId_whenSourceAndTargetAreEqual_AFlowableExceptionIsThrown() {
        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_A).simulate()).isInstanceOf(FlowableException.class);
        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_A).complete()).isInstanceOf(FlowableException.class);
    }

    private String startProcess(String tenantId, String processDefinitionKey, String processInstanceName, int completeTaskLoops, boolean overrideProcessDefinitionTenantIdEnabled) {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder().processDefinitionKey(processDefinitionKey).name(processInstanceName).tenantId(tenantId).fallbackToDefaultTenant();
        if (overrideProcessDefinitionTenantIdEnabled) {
            processInstanceBuilder.overrideProcessDefinitionTenantId(tenantId);
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

}