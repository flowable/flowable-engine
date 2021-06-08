package org.flowable.dmn.engine.test.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.engine.test.AbstractFlowableDmnTest;
import org.junit.Before;
import org.junit.Test;

public class ChangeTenantIdDecisionsTest extends AbstractFlowableDmnTest {

    private static final String TEST_TENANT_A = "test-tenant-a";
    private static final String TEST_TENANT_B = "test-tenant-b";
    private static final String TEST_TENANT_C = "test-tenant-c";
        
    private String deploymentIdWithTenantA;
    private String deploymentIdWithTenantB;
    private String deploymentIdWithTenantC;
    private String deploymentIdWithoutTenant;

    @Before
    public void setUp() {
        this.deploymentIdWithTenantA = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/tenant/testDecision.dmn").tenantId(TEST_TENANT_A)
                .deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithTenantA);
        this.deploymentIdWithTenantB = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/tenant/testDecision.dmn").tenantId(TEST_TENANT_B)
                .deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithTenantB);
        this.deploymentIdWithTenantC = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/tenant/testDecision.dmn").tenantId(TEST_TENANT_C)
                .deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithTenantC);
        this.deploymentIdWithoutTenant = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/tenant/testDecisionDup.dmn").deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithoutTenant);
    }

    @Test
    public void testDeployments() {
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(4);
        assertThat(repositoryService.createDeploymentQuery().deploymentWithoutTenantId().count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_A).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_B).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_C).count()).isEqualTo(1);
    }

    @Test
    public void testChangeTenantIdDecision() {
        // Executing a Decision for every context
        executeDecision(TEST_TENANT_A, "testDecision");
        executeDecision(TEST_TENANT_B, "testDecision");
        executeDecision(TEST_TENANT_C, "testDecision");

        // Prior to changing the Tenant Id, all elements are associate to the original
        // tenant
        checkNumberOfDecisions(TEST_TENANT_A, 1, "prior to changing to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 1, "prior to changing to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_C, 1, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = managementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkNumberOfDecisions(TEST_TENANT_A, 1, "after simulating the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 1, "after simulating the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_C, 1, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = managementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).complete();

        // All the instances should now be assigned to the tenant B
        checkNumberOfDecisions(TEST_TENANT_A, 0, "after the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 2, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkNumberOfDecisions(TEST_TENANT_C, 1, "after the change to " + TEST_TENANT_B);

        // The simulation result must match the actual result
        assertThat(simulationResult).isEqualTo(result).as("The simulation result must match the actual result.");
    }

    @Test
    public void testChangeTenantIdDecision_OnlyDefaultTenantDefinitionInstances() {
        // Executing a Decision for every context
        executeDecision(TEST_TENANT_A, "testDecision");
        executeDecision(TEST_TENANT_A, "testDecisionDup"); //This definitionKey is present only in the default tenant
        executeDecision(TEST_TENANT_B, "testDecision");
        executeDecision(TEST_TENANT_C, "testDecision");

        String defaultTenant = dmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(TEST_TENANT_A, ScopeTypes.DMN, null);
        
        // Prior to changing the Tenant Id, all elements are associate to the original
        // tenant
        checkNumberOfDecisions(TEST_TENANT_A, 2, "prior to changing to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 1, "prior to changing to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_C, 1, "prior to changing to " + TEST_TENANT_B);
        checkNumberOfDecisions(defaultTenant, 0, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = managementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .onlyInstancesFromDefaultTenantDefinitions(true)
                .simulate();

        // All the instances should stay in the original tenant after the simulation
        checkNumberOfDecisions(TEST_TENANT_A, 2, "after simulating the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 1, "after simulating the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_C, 1, "after simulating the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(defaultTenant, 0, "after simulating the change to " + TEST_TENANT_B);


        // We now proceed with the changeTenantId operation
        ChangeTenantIdResult result = managementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .onlyInstancesFromDefaultTenantDefinitions(true)
                .complete();

        // The instance created with the definition from the default tenant should now be assigned to the tenant B
        checkNumberOfDecisions(TEST_TENANT_A, 1, "after the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 2, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkNumberOfDecisions(TEST_TENANT_C, 1, "after the change to " + TEST_TENANT_B);

        // And there is still zero instances associated to the default tenant
        checkNumberOfDecisions(defaultTenant, 0, "after the change to " + TEST_TENANT_B);


        // The simulation result must match the actual result
        assertThat(simulationResult).isEqualTo(result).as("The simulation result must match the actual result.");
    }

    private void executeDecision(String tenantId, String decisionKey) {
        ruleService.createExecuteDecisionBuilder().tenantId(tenantId).fallbackToDefaultTenant()
        .decisionKey(decisionKey).variable("inputVar", "a").executeWithSingleResult();
    }

    private void checkNumberOfDecisions(String tenantId, int expectedNumberOfDecisions, String moment) {
        List<DmnHistoricDecisionExecution> decisions = dmnHistoryService.createHistoricDecisionExecutionQuery()
        .tenantId(tenantId).list();
        assertThat(decisions.size())
                .as("The expected number of executions that  belong to tenant {} is {} but we found {} {}.",
                expectedNumberOfDecisions, tenantId, decisions.size(), moment)
                .isEqualTo(expectedNumberOfDecisions);
    }

}
