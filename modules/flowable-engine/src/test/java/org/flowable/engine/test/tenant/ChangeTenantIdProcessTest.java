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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
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
    }

    @AfterEach
    public void cleanUp() {
        repositoryService.deleteDeployment(deploymentIdWithTenantA,true);
        repositoryService.deleteDeployment(deploymentIdWithTenantB,true);
        repositoryService.deleteDeployment(deploymentIdWithTenantC,true);
        repositoryService.deleteDeployment(deploymentIdWithoutTenant,true);
    }

    @Test
    public void testChangeTenantIdProcessInstance() {
        //testDeployments() {
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(4);
        assertThat(repositoryService.createDeploymentQuery().deploymentWithoutTenantId().count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_A).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_B).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_C).count()).isEqualTo(1);

        //Starting process instances that will be completed
        String processInstanceIdACompleted = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdACompleted", true, false);
        String processInstanceIdBCompleted = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBCompleted", true, false);
        String processInstanceIdCCompleted = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCCompleted", true, false);
        
        //Starting process instances that will remain active
        String processInstanceIdAActive = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdAActive", false, false);
        String processInstanceIdBActive = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBActive", false, false);
        String processInstanceIdCActive = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCActive", false, false);

        Set<String> processInstancesTenantA = new HashSet<>(Arrays.asList(processInstanceIdACompleted, processInstanceIdAActive));
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
        assertThat(simulationResult).isEqualTo(result).as("The simulation result must match the actual result.");

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
        String processInstanceIdACompleted = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdACompleted", true, false);
        String processInstanceIdADTCompleted = startProcess(TEST_TENANT_A, "testProcessDup", "processInstanceIdADTCompleted", true, true); // For this instance we want to override the tenant Id.
        String processInstanceIdBCompleted = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBCompleted", true, false);
        String processInstanceIdCCompleted = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCCompleted", true, false);
        
        //Starting process instances that will remain active
        String processInstanceIdAActive = startProcess(TEST_TENANT_A, "testProcess", "processInstanceIdAActive", false, false);
        String processInstanceIdADTActive = startProcess(TEST_TENANT_A, "testProcessDup", "processInstanceIdADTActive", false, true); // For this instance we want to override the tenant Id.
        String processInstanceIdBActive = startProcess(TEST_TENANT_B, "testProcess", "processInstanceIdBActive", false, false);
        String processInstanceIdCActive = startProcess(TEST_TENANT_C, "testProcess", "processInstanceIdCActive", false, false);

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

    private String startProcess(String tenantId, String processDefinitionKey, String processInstanceName, boolean completeProcess, boolean overrideProcessDefinitionTenantIdEnabled) {
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder().processDefinitionKey(processDefinitionKey).name(processInstanceName).tenantId(tenantId).fallbackToDefaultTenant();
        if (overrideProcessDefinitionTenantIdEnabled) {
            processInstanceBuilder.overrideProcessDefinitionTenantId(tenantId);
        }
        ProcessInstance processInstance = processInstanceBuilder.start();
        completeTask(processInstance);
        if (completeProcess) {
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