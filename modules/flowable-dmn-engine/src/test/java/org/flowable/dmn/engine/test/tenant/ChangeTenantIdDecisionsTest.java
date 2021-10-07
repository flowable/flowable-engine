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
package org.flowable.dmn.engine.test.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.dmn.api.DmnChangeTenantIdEntityTypes.HISTORIC_DECISION_EXECUTIONS;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.api.DmnHistoricDecisionExecutionQuery;
import org.flowable.dmn.engine.test.AbstractFlowableDmnTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChangeTenantIdDecisionsTest extends AbstractFlowableDmnTest {

    private static final String TEST_TENANT_A = "test-tenant-a";
    private static final String TEST_TENANT_B = "test-tenant-b";
    private static final String TEST_TENANT_C = "test-tenant-c";

    protected String deploymentIdWithTenantA;
    protected String deploymentIdWithTenantB;
    protected String deploymentIdWithTenantC;
    protected String deploymentIdWithoutTenant;

    @Before
    public void setUp() {
        this.deploymentIdWithTenantA = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/tenant/testDecision.dmn").tenantId(TEST_TENANT_A)
                .deploy().getId();
        this.deploymentIdWithTenantB = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/tenant/testDecision.dmn").tenantId(TEST_TENANT_B)
                .deploy().getId();
        this.deploymentIdWithTenantC = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/tenant/testDecision.dmn").tenantId(TEST_TENANT_C)
                .deploy().getId();
        this.deploymentIdWithoutTenant = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/tenant/testDecisionDup.dmn")
                .addClasspathResource("org/flowable/dmn/engine/test/tenant/testDecision.dmn")
                .deploy().getId();
    }

    @After
    public void tearDown() {
        repositoryService.deleteDeployment(deploymentIdWithTenantA);
        repositoryService.deleteDeployment(deploymentIdWithTenantB);
        repositoryService.deleteDeployment(deploymentIdWithTenantC);
        repositoryService.deleteDeployment(deploymentIdWithoutTenant);
    }

    @Test
    public void changeTenantIdDecision() {
        //testDeployments
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(4);
        assertThat(repositoryService.createDeploymentQuery().deploymentWithoutTenantId().count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_A).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_B).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_C).count()).isEqualTo(1);

        // Executing a Decision for every context
        executeDecision(TEST_TENANT_A, "testDecision");
        executeDecision(TEST_TENANT_B, "testDecision");
        executeDecision(TEST_TENANT_C, "testDecision");

        // Prior to changing the Tenant Id, all elements are associate to the original tenant
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

        //Expected results map
        Map<String, Long> resultMap = Collections.singletonMap(HISTORIC_DECISION_EXECUTIONS, 1L);

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

    }

    @Test
    public void changeTenantIdDecisionFromEmptyTenant() {
        //testDeployments
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(4);
        assertThat(repositoryService.createDeploymentQuery().deploymentWithoutTenantId().count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_A).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_B).count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_C).count()).isEqualTo(1);

        // Executing a Decision for every context
        executeDecision("", "testDecision");
        executeDecision(TEST_TENANT_B, "testDecision");
        executeDecision(TEST_TENANT_C, "testDecision");

        // Prior to changing the Tenant Id, all elements are associate to the original tenant
        checkNumberOfDecisions("", 1, "prior to changing to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 1, "prior to changing to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_C, 1, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = managementService
                .createChangeTenantIdBuilder("", TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkNumberOfDecisions("", 1, "after simulating the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 1, "after simulating the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_C, 1, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = managementService
                .createChangeTenantIdBuilder("", TEST_TENANT_B).complete();

        // All the instances should now be assigned to the tenant B
        checkNumberOfDecisions("", 0, "after the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 2, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkNumberOfDecisions(TEST_TENANT_C, 1, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = Collections.singletonMap(HISTORIC_DECISION_EXECUTIONS, 1L);

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

    }

    @Test
    public void changeTenantIdDecisionWithDefinedDefinitionTenant() {
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
                .definitionTenantId("")
                .simulate();

        // All the instances should stay in the original tenant after the simulation
        checkNumberOfDecisions(TEST_TENANT_A, 2, "after simulating the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 1, "after simulating the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_C, 1, "after simulating the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(defaultTenant, 0, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation
        ChangeTenantIdResult result = managementService
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .definitionTenantId("")
                .complete();

        // The instance created with the definition from the default tenant should now be assigned to the tenant B
        checkNumberOfDecisions(TEST_TENANT_A, 1, "after the change to " + TEST_TENANT_B);
        checkNumberOfDecisions(TEST_TENANT_B, 2, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkNumberOfDecisions(TEST_TENANT_C, 1, "after the change to " + TEST_TENANT_B);

        // And there is still zero instances associated to the default tenant
        checkNumberOfDecisions(defaultTenant, 0, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = Collections.singletonMap(HISTORIC_DECISION_EXECUTIONS, 1L);

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
    }

    @Test
    public void changeTenantIdWhenSourceAndTargetAreEqual() {
        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_A).simulate())
                .isInstanceOf(FlowableIllegalArgumentException.class);
        assertThatThrownBy(() -> managementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_A).complete())
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    protected void executeDecision(String tenantId, String decisionKey) {
        ruleService.createExecuteDecisionBuilder().tenantId(tenantId).fallbackToDefaultTenant()
                .decisionKey(decisionKey).variable("inputVar", "a").executeWithSingleResult();
    }

    protected void checkNumberOfDecisions(String tenantId, int expectedNumberOfDecisions, String moment) {
        DmnHistoricDecisionExecutionQuery query = dmnHistoryService.createHistoricDecisionExecutionQuery();
        if (StringUtils.isNotEmpty(tenantId)) {
            query.tenantId(tenantId);
        } else {
            query.withoutTenantId();
        }
        List<DmnHistoricDecisionExecution> decisions = query.list();
        assertThat(decisions)
                .as("The expected number of executions that  belong to tenant {} is {} but we found {} {}.",
                        expectedNumberOfDecisions, tenantId, decisions.size(), moment)
                .hasSize(expectedNumberOfDecisions);
    }

}
