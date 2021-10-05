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
package org.flowable.form.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.form.api.FormChangeTenantIdEntityTypes.FORM_INSTANCES;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.flowable.form.api.FormManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeTenantIdFormInstanceTest extends AbstractFlowableFormTest {

    private static final String TEST_TENANT_A = "test-tenant-a";
    private static final String TEST_TENANT_B = "test-tenant-b";
    private static final String TEST_TENANT_C = "test-tenant-c";

    protected String deploymentIdWithTenantA;
    protected String deploymentIdWithTenantB;
    protected String deploymentIdWithTenantC;
    protected String deploymentIdWithoutTenant;

    @BeforeEach
    void setUp() {
        this.deploymentIdWithTenantA = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/form/engine/test/deployment/simple.form").tenantId(TEST_TENANT_A)
                .deploy().getId();
        this.deploymentIdWithTenantB = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/form/engine/test/deployment/simple.form").tenantId(TEST_TENANT_B)
                .deploy().getId();
        this.deploymentIdWithTenantC = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/form/engine/test/deployment/simple.form").tenantId(TEST_TENANT_C)
                .deploy().getId();
        this.deploymentIdWithoutTenant = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/form/engine/test/example.form")
                .addClasspathResource("org/flowable/form/engine/test/deployment/simple.form")
                .deploy().getId();

    }

    @AfterEach
    void tearDown() {
        repositoryService.deleteDeployment(deploymentIdWithTenantA, true);
        repositoryService.deleteDeployment(deploymentIdWithTenantB, true);
        repositoryService.deleteDeployment(deploymentIdWithTenantC, true);
        repositoryService.deleteDeployment(deploymentIdWithoutTenant, true);
    }

    @Test
    void changeTenantIdFormInstance() {
        // Starting Form instances
        String formInstanceIdABpmn = createFormInstance(TEST_TENANT_A, "form1", false, ScopeTypes.BPMN);
        String formInstanceIdBBpmn = createFormInstance(TEST_TENANT_B, "form1", false, ScopeTypes.BPMN);
        String formInstanceIdCBpmn = createFormInstance(TEST_TENANT_C, "form1", false, ScopeTypes.BPMN);
        String formInstanceIdACmmn = createFormInstance(TEST_TENANT_A, "form1", false, ScopeTypes.CMMN);
        String formInstanceIdBCmmn = createFormInstance(TEST_TENANT_B, "form1", false, ScopeTypes.CMMN);
        String formInstanceIdCCmmn = createFormInstance(TEST_TENANT_C, "form1", false, ScopeTypes.CMMN);

        // Prior to changing the Tenant Id, all elements are associated to the original tenant
        checkFormInstanceTenant(formInstanceIdABpmn, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBBpmn, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCBpmn, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdACmmn, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBCmmn, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCCmmn, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = formEngine.getFormManagementService()
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkFormInstanceTenant(formInstanceIdABpmn, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBBpmn, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCBpmn, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdACmmn, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBCmmn, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCCmmn, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = formEngine.getFormManagementService()
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).complete();

        // All the instances should now be assigned to the tenant B
        checkFormInstanceTenant(formInstanceIdABpmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBBpmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdACmmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBCmmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkFormInstanceTenant(formInstanceIdCBpmn, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCCmmn, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = Collections.singletonMap(FORM_INSTANCES, 2L);

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
    void changeTenantIdFormInstanceFromEmptyTenant() {
        // Starting Form instances
        String formInstanceIdNoTenantBpmn = createFormInstance("", "form1", false, ScopeTypes.BPMN);
        String formInstanceIdBBpmn = createFormInstance(TEST_TENANT_B, "form1", false, ScopeTypes.BPMN);
        String formInstanceIdCBpmn = createFormInstance(TEST_TENANT_C, "form1", false, ScopeTypes.BPMN);
        String formInstanceIdNoTenantCmmn = createFormInstance("", "form1", false, ScopeTypes.CMMN);
        String formInstanceIdBCmmn = createFormInstance(TEST_TENANT_B, "form1", false, ScopeTypes.CMMN);
        String formInstanceIdCCmmn = createFormInstance(TEST_TENANT_C, "form1", false, ScopeTypes.CMMN);

        // Prior to changing the Tenant Id, all elements are associated to the original tenant
        checkFormInstanceTenant(formInstanceIdNoTenantBpmn, "", "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBBpmn, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCBpmn, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdNoTenantCmmn, "", "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBCmmn, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCCmmn, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = formEngine.getFormManagementService()
                .createChangeTenantIdBuilder("", TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkFormInstanceTenant(formInstanceIdNoTenantBpmn, "", "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBBpmn, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCBpmn, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdNoTenantCmmn, "", "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBCmmn, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCCmmn, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = formEngine.getFormManagementService()
                .createChangeTenantIdBuilder("", TEST_TENANT_B).complete();

        // All the instances should now be assigned to the tenant B
        checkFormInstanceTenant(formInstanceIdNoTenantBpmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBBpmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdNoTenantCmmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBCmmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkFormInstanceTenant(formInstanceIdCBpmn, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCCmmn, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = Collections.singletonMap(FORM_INSTANCES, 2L);

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

    private void checkFormInstanceTenant(String formInstanceId, String expectedTenantId, String moment) {
        FormInstance formInstance = formService.createFormInstanceQuery().id(formInstanceId).singleResult();
        assertThat(formInstance).isNotNull();
        assertThat(StringUtils.defaultIfEmpty(formInstance.getTenantId(), ""))
                .as("Form instance %s belongs to tenant %s instead of %s %s", formInstance.getId(),
                        formInstance.getTenantId(), expectedTenantId, moment)
                .isEqualTo(expectedTenantId);
    }

    @Test
    void changeTenantIdFormInstanceWithDefinedDefinitionTenant() {
        // Starting Form instances
        String formInstanceIdABpmn = createFormInstance(TEST_TENANT_A, "form1", false, ScopeTypes.BPMN);
        String formInstanceIdADTBpmn = createFormInstance(TEST_TENANT_A, "simpleExample", true, ScopeTypes.BPMN);
        String formInstanceIdBBpmn = createFormInstance(TEST_TENANT_B, "form1", false, ScopeTypes.BPMN);
        String formInstanceIdCBpmn = createFormInstance(TEST_TENANT_C, "form1", false, ScopeTypes.BPMN);
        String formInstanceIdACmmn = createFormInstance(TEST_TENANT_A, "form1", false, ScopeTypes.CMMN);
        String formInstanceIdADTCmmn = createFormInstance(TEST_TENANT_A, "simpleExample", true, ScopeTypes.CMMN);
        String formInstanceIdBCmmn = createFormInstance(TEST_TENANT_B, "form1", false, ScopeTypes.CMMN);
        String formInstanceIdCCmmn = createFormInstance(TEST_TENANT_C, "form1", false, ScopeTypes.CMMN);

        // Prior to changing the Tenant Id, all elements are associated to the original tenant
        checkFormInstanceTenant(formInstanceIdABpmn, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdADTBpmn, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBBpmn, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCBpmn, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdACmmn, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdADTCmmn, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBCmmn, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCCmmn, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = formEngine.getFormManagementService()
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .definitionTenantId("")
                .simulate();

        // All the instances should stay in the original tenant after the simulation
        checkFormInstanceTenant(formInstanceIdABpmn, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdADTBpmn, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBBpmn, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCBpmn, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdACmmn, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdADTCmmn, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBCmmn, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCCmmn, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = formEngine.getFormManagementService()
                .createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .definitionTenantId("")
                .complete();

        // All the instances from the default tenant should now be assigned to the tenant B
        checkFormInstanceTenant(formInstanceIdADTBpmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdADTCmmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The rest of the instances remain in the initial tenants
        checkFormInstanceTenant(formInstanceIdABpmn, TEST_TENANT_A, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBBpmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCBpmn, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdACmmn, TEST_TENANT_A, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdBCmmn, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkFormInstanceTenant(formInstanceIdCCmmn, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = Collections.singletonMap(FORM_INSTANCES, 2L);

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
    void changeTenantIdWhenSourceAndTargetAreEqual() {
        FormManagementService formManagementService = formEngine.getFormManagementService();
        assertThatThrownBy(() -> formManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_A).simulate())
                .isInstanceOf(FlowableIllegalArgumentException.class);
        assertThatThrownBy(() -> formManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_A).complete())
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    private String createFormInstance(String tenantId, String formDefinitionKey, boolean fallbackToDefaultTenant, String scope) {
        FormInfo formInfo = repositoryService.getFormModelByKey(formDefinitionKey, tenantId, fallbackToDefaultTenant);
        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        FormInstance formInstance = (ScopeTypes.BPMN.equals(scope)
                ? formService.createFormInstance(valuesMap, formInfo, null, null, null, tenantId, null)
                : formService.createFormInstanceWithScopeId(valuesMap, formInfo, null, null, ScopeTypes.CMMN, null, tenantId, null));
        assertThat(formInstance).isNotNull();
        return formInstance.getId();
    }

}
