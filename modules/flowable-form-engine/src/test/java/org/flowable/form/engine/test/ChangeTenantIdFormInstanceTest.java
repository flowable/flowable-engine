package org.flowable.form.engine.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ChangeTenantIdFormInstanceTest extends AbstractFlowableFormTest {

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
                .deploy().getId();

    }

    @AfterEach
    public void tearDown() {
        repositoryService.deleteDeployment(deploymentIdWithTenantA, true);
        repositoryService.deleteDeployment(deploymentIdWithTenantB, true);
        repositoryService.deleteDeployment(deploymentIdWithTenantC, true);
        repositoryService.deleteDeployment(deploymentIdWithoutTenant, true);
    }

    @Test
    public void testChangeTenantIdFormInstance() {
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

        // The simulation result must match the actual result
        assertThat(simulationResult).as("The simulation result must match the actual result.").isEqualTo(result);
    }

    private void checkFormInstanceTenant(String formInstanceId, String expectedTenantId, String moment) {
        FormInstance formInstance = formService.createFormInstanceQuery().id(formInstanceId).singleResult();
        assertThat(formInstance).isNotNull();
        assertThat(formInstance.getTenantId())
            .as("Form instance %s belongs to tenant %s instead of %s %s", formInstance.getId(), 
            formInstance.getTenantId(), expectedTenantId,  moment)
            .isEqualTo(expectedTenantId);
    }

    @Test
    public void testChangeTenantIdFormInstance_onlyDefaultTenantDefinitionInstances() {
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
                .onlyInstancesFromDefaultTenantDefinitions(true)
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
                .onlyInstancesFromDefaultTenantDefinitions(true)
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

        // The simulation result must match the actual result
        assertThat(simulationResult).isEqualTo(result).as("The simulation result must match the actual result.");
    }

    private String createFormInstance(String tenantId, String formDefinitionKey, boolean fallbackToDefaultTenant, String scope) {
        FormInfo formInfo = repositoryService.getFormModelByKey(formDefinitionKey, tenantId, fallbackToDefaultTenant);
        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("input1", "test");
        FormInstance formInstance = (ScopeTypes.BPMN.equals(scope)
            ?formService.createFormInstance(valuesMap, formInfo, null, null, null, tenantId, null)
            :formService.createFormInstanceWithScopeId(valuesMap, formInfo, null, null, ScopeTypes.CMMN, null, tenantId, null));
        assertThat(formInstance).isNotNull();
        return formInstance.getId();
    }
    
}
