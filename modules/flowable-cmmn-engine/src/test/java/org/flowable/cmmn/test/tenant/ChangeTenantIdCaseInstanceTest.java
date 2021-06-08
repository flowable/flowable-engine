package org.flowable.cmmn.test.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

public class ChangeTenantIdCaseInstanceTest extends FlowableCmmnTestCase {

    private static final String TEST_TENANT_A = "test-tenant-a";
    private static final String TEST_TENANT_B = "test-tenant-b";
    private static final String TEST_TENANT_C = "test-tenant-c";
    
    private String deploymentIdWithTenantA;
    private String deploymentIdWithTenantB;
    private String deploymentIdWithTenantC;
    private String deploymentIdWithoutTenant;

    @Before
    public void setUp() {
        this.deploymentIdWithTenantA = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/tenant/caseWithMilestone.cmmn").tenantId(TEST_TENANT_A)
                .deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithTenantA);
        this.deploymentIdWithTenantB = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/tenant/caseWithMilestone.cmmn").tenantId(TEST_TENANT_B)
                .deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithTenantB);
        this.deploymentIdWithTenantC = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/tenant/caseWithMilestone.cmmn").tenantId(TEST_TENANT_C)
                .deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithTenantC);
        this.deploymentIdWithoutTenant = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/tenant/caseWithMilestoneDup.cmmn").deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithoutTenant);
    }

    @Test
    public void testDeployments() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().count()).isEqualTo(4);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentWithoutTenantId().count()).isEqualTo(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_A).count())
                .isEqualTo(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_B).count())
                .isEqualTo(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_C).count())
                .isEqualTo(1);
    }

    @Test
    public void testChangeTenantIdCaseInstance() {
        // Starting case instances that will be sent to the history
        String caseInstanceIdACompleted = startCase(TEST_TENANT_A, "caseWithMilestone", "caseInstanceACompleted", true);
        String caseInstanceIdBCompleted = startCase(TEST_TENANT_B, "caseWithMilestone", "caseInstanceBCompleted", true);
        String caseInstanceIdCCompleted = startCase(TEST_TENANT_C, "caseWithMilestone", "caseInstanceCCompleted", true);
        
        // Starting case instances that will be kept active
        String caseInstanceIdAActive = startCase(TEST_TENANT_A, "caseWithMilestone", "caseInstanceAActive", false);
        String caseInstanceIdBActive = startCase(TEST_TENANT_B, "caseWithMilestone", "caseInstanceBActive", false);
        String caseInstanceIdCActive = startCase(TEST_TENANT_C, "caseWithMilestone", "caseInstanceCActive", false);
        
        Set<String> caseInstanceIdsTenantA = Sets.newSet(caseInstanceIdACompleted, caseInstanceIdAActive);
        Set<String> caseInstanceIdsTenantB = Sets.newSet(caseInstanceIdBCompleted, caseInstanceIdBActive);
        Set<String> caseInstanceIdsTenantC = Sets.newSet(caseInstanceIdCCompleted, caseInstanceIdCActive);

        // Prior to changing the Tenant Id, all elements are associated to the original tenant
        checkTenantIdForAllInstances(caseInstanceIdsTenantA, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = cmmnManagementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkTenantIdForAllInstances(caseInstanceIdsTenantA, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = cmmnManagementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).complete();

        // All the instances should now be assigned to the tenant B
        checkTenantIdForAllInstances(caseInstanceIdsTenantA, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        // The simulation result must match the actual result
        assertThat(simulationResult).isEqualTo(result).as("The simulation result must match the actual result.");

        //Check that we can complete the active instances that we have changed
        completeTask(caseInstanceIdAActive);
        assertCaseInstanceEnded(caseInstanceIdAActive);
        completeTask(caseInstanceIdBActive);
        assertCaseInstanceEnded(caseInstanceIdBActive);
        completeTask(caseInstanceIdCActive);
        assertCaseInstanceEnded(caseInstanceIdCActive);

    }

    private String startCase(String tenantId, String caseDefinitionKey, String name, boolean completeCase) {
        return startCase(tenantId, caseDefinitionKey, name, completeCase, false);
    }

    private String startCase(String tenantId, String caseDefinitionKey, String name, boolean completeCase, boolean overrideCaseDefinitionTenantIdEnabled) {
        CaseInstanceBuilder caseInstanceBuilder = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(caseDefinitionKey)
                .name(name).tenantId(tenantId).fallbackToDefaultTenant();
        if (overrideCaseDefinitionTenantIdEnabled) {
            caseInstanceBuilder.overrideCaseDefinitionTenantId(tenantId);
        }
        CaseInstance caseInstance = caseInstanceBuilder.start();
        // Completing A will reach milestone M1, which sets a variable that activates the second stage
        completeTask(caseInstance.getId());
        if (completeCase) {
            // Completing task B to send the caseInstance and all the plan items to the history
            completeTask(caseInstance.getId());
        }
        return caseInstance.getId();
    }

     private void completeTask(String caseInstanceId) {
             List<Task> tasks;
             tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).list();
             cmmnTaskService.complete(tasks.get(0).getId());
     }

    private void checkTenantIdForAllInstances(Set<String> caseInstanceIds, String expectedTenantId, String moment) {
        cmmnRuntimeService.createCaseInstanceQuery().caseInstanceIds(caseInstanceIds).list().forEach(ci -> {
                assertThat(ci.getTenantId())
                        .as("Active case instance '%s' %s must belong to %s but belongs to %s.", ci.getName(), moment,
                            expectedTenantId, ci.getTenantId())
                        .isEqualTo(expectedTenantId);

                cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(ci.getId()).list()
                .forEach(pii -> assertThat(pii.getTenantId())
                        .as("Active plan item instance %s from %s %s must belong to %s but belongs to %s.", 
                        pii.getName(), ci.getName(), moment, expectedTenantId, pii.getTenantId())
                        .isEqualTo(expectedTenantId));

                cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(ci.getId()).list()
                .forEach(am -> assertThat(am.getTenantId())
                        .as("Active milestone instance %s from %s %s must belong to %s but belongs to %s.", 
                        am.getName(), ci.getName(), moment,expectedTenantId, am.getTenantId())
                        .isEqualTo(expectedTenantId));
        });
        cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceIds(caseInstanceIds).list().forEach(hci -> {
                assertThat(hci.getTenantId())
                        .as("Historic case instances '%s' %s must belong to %s but belongs to %s.", 
                        hci.getName(), moment, expectedTenantId, hci.getTenantId())
                        .isEqualTo(expectedTenantId);

                cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(hci.getId()).list()
                .forEach(hpii -> assertThat(hpii.getTenantId())
                        .as("Historic plan item instance %s from %s %s must belong to %s but belongs to %s.", 
                        hpii.getName(), hci.getName(), moment, expectedTenantId, hpii.getTenantId())
                        .isEqualTo(expectedTenantId));

                cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(hci.getId()).list()
                .forEach(hm -> assertThat(hm.getTenantId())
                        .as("Historic milestone instance %s from %s %s must belong to %s but belongs to %s.", 
                        hm.getName(), hci.getName(), moment, expectedTenantId, hm.getTenantId())
                        .isEqualTo(expectedTenantId));
        });
    }


    @Test
    public void testChangeTenantIdCaseInstance_onlyDefaultTenantDefinitionInstances() {
        // In this test we will mark the instances created with a definition from the
        // default tenant with DT

        // Starting case instances that will be sent to the history
        String caseInstanceIdACompleted = startCase(TEST_TENANT_A, "caseWithMilestone", "caseInstanceACompleted", true);
        String caseInstanceIdADTCompleted = startCase(TEST_TENANT_A, "caseWithMilestoneDup", "caseInstanceADTCompleted", true, true); // For this instance we want to override the tenant Id.
        String caseInstanceIdBCompleted = startCase(TEST_TENANT_B, "caseWithMilestone", "caseInstanceBCompleted", true);
        String caseInstanceIdCCompleted = startCase(TEST_TENANT_C, "caseWithMilestone", "caseInstanceCCompleted", true);
        
        // Starting case instances that will be kept active
        String caseInstanceIdAActive = startCase(TEST_TENANT_A, "caseWithMilestone", "caseInstanceAActive", false);
        String caseInstanceIdADTActive = startCase(TEST_TENANT_A, "caseWithMilestoneDup", "caseInstanceADTActive", false, true); // For this instance we want to override the tenant Id.
        String caseInstanceIdBActive = startCase(TEST_TENANT_B, "caseWithMilestone", "caseInstanceBActive", false);
        String caseInstanceIdCActive = startCase(TEST_TENANT_C, "caseWithMilestone", "caseInstanceCActive", false);
        
        Set<String> caseInstanceIdsTenantADTOnly = Sets.newSet(caseInstanceIdADTCompleted, caseInstanceIdADTActive);
        Set<String> caseInstanceIdsTenantANotDT = Sets.newSet(caseInstanceIdACompleted, caseInstanceIdAActive);
        Set<String> caseInstanceIdsTenantAAll = Sets.newSet(caseInstanceIdADTCompleted, caseInstanceIdADTActive, caseInstanceIdACompleted, caseInstanceIdAActive);
        Set<String> caseInstanceIdsTenantB = Sets.newSet(caseInstanceIdBCompleted, caseInstanceIdBActive);
        Set<String> caseInstanceIdsTenantC = Sets.newSet(caseInstanceIdCCompleted, caseInstanceIdCActive);

        // Prior to changing the Tenant Id, all elements are associate to the original
        // tenant
        checkTenantIdForAllInstances(caseInstanceIdsTenantAAll, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = cmmnManagementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .onlyInstancesFromDefaultTenantDefinitions(true).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkTenantIdForAllInstances(caseInstanceIdsTenantAAll, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B,    "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C,    "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = cmmnManagementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .onlyInstancesFromDefaultTenantDefinitions(true).complete();

        // All the instances from the default tenant should now be assigned to the tenant B
        checkTenantIdForAllInstances(caseInstanceIdsTenantADTOnly, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // But the instances that were created with a definition from tenant A must stay in tenant A
        checkTenantIdForAllInstances(caseInstanceIdsTenantANotDT, TEST_TENANT_A, "after the change to " + TEST_TENANT_B);

        // The instances from Tenant B are still associated to tenant B
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched in tenant C
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        // The simulation result must match the actual result
        assertThat(simulationResult).isEqualTo(result).as("The simulation result must match the actual result.");

        //Check that we can complete the active instances that we have changed
        completeTask(caseInstanceIdAActive);
        assertCaseInstanceEnded(caseInstanceIdAActive);
        completeTask(caseInstanceIdADTActive);
        assertCaseInstanceEnded(caseInstanceIdADTActive);
        completeTask(caseInstanceIdBActive);
        assertCaseInstanceEnded(caseInstanceIdBActive);
        completeTask(caseInstanceIdCActive);
        assertCaseInstanceEnded(caseInstanceIdCActive);
        

    }

}