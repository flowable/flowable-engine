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
package org.flowable.dmn.engine.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.DmnEngines;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Yvo Swillens
 */
public class MixedDeploymentTest extends AbstractFlowableDmnEngineConfiguratorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void deploySingleProcessAndDecisionTable() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .processDefinitionKey("oneDecisionTaskProcess")
                .singleResult();

        assertNotNull(processDefinition);
        assertEquals("oneDecisionTaskProcess", processDefinition.getKey());

        DmnRepositoryService dmnRepositoryService = DmnEngines.getDefaultDmnEngine().getDmnRepositoryService();
        DmnDecisionTable decisionTable = dmnRepositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision1")
                .singleResult();
        assertNotNull(decisionTable);
        assertEquals("decision1", decisionTable.getKey());

        List<DmnDecisionTable> decisionTableList = repositoryService.getDecisionTablesForProcessDefinition(processDefinition.getId());
        assertEquals(1l, decisionTableList.size());
        assertEquals("decision1", decisionTableList.get(0).getKey());
    }
    
    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void testDecisionTaskExecution() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess", Collections.singletonMap("inputVariable1", (Object) 1));
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId()).orderByVariableName().asc().list();
        
        assertEquals("inputVariable1", variables.get(0).getVariableName());
        assertEquals(1, variables.get(0).getValue());
        assertEquals("outputVariable1", variables.get(1).getVariableName());
        assertEquals("result1", variables.get(1).getValue());
    }
    
    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void testFailedDecisionTask() {
        try {
            runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess");
            fail("Expected DMN failure due to missing variable");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unknown property used in expression: #{inputVariable1"));
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskNoHitsErrorProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void testNoHitsDecisionTask() {
        try {
            runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess", Collections.singletonMap("inputVariable1", (Object) 2));
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("did not hit any rules for the provided input"));
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessFallBackToDefaultTenant.bpmn20.xml" },
        tenantId = "flowable"
    )
    public void testDecisionTaskExecutionInAnotherDeploymentAndTenant() {
        deployDecisionAndAssertProcessExecuted();
    }

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml" }
    )
    public void testDecisionTaskExecutionInAnotherDeploymentAndTenantDefaultBehavior() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("no processes deployed with key 'oneDecisionTaskProcess' for tenant identifier 'flowable'");

        deployDecisionAndAssertProcessExecuted();
    }

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessFallBackToDefaultTenantFalse.bpmn20.xml" },
        tenantId = "flowable"
    )
    public void testDecisionTaskExecutionInAnotherDeploymentAndTenantFalse() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("No decision found for key: decision1 and tenant flowable");

        deployDecisionAndAssertProcessExecuted();
    }

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessFallBackToDefaultTenantFalse.bpmn20.xml" },
        tenantId = "flowable"
    )
    public void testDecisionTaskExecutionInAnotherDeploymentAndTenantFallbackFalseWithoutDeployment() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("No decision found for key: decision1 and tenant flowable");

        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().
            addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn").
            tenantId("anotherTenant").
            deploy();
        try {
            assertDmnProcessExecuted();
        } finally {
            this.repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessFallBackToDefaultTenant.bpmn20.xml" },
        tenantId = "flowable"
    )
    public void testDecisionTaskExecutionInAnotherDeploymentAndTenantFallbackTrueWithoutDeployment() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("No decision found for key: decision1. There was also no fall back decision table found without tenant.");

        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().
            addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn").
            tenantId("anotherTenant").
            deploy();
        try {
            assertDmnProcessExecuted();
        } finally {
            this.repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }


    protected void deployDecisionAndAssertProcessExecuted() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().
            addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn").
            tenantId("").
            deploy();
        try {
            assertDmnProcessExecuted();
        } finally {
            this.repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    protected void assertDmnProcessExecuted() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId(
            "oneDecisionTaskProcess",
            Collections.singletonMap("inputVariable1", (Object) 1),
            "flowable");
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
            .processInstanceId(processInstance.getId()).orderByVariableName().asc().list();

        assertEquals("inputVariable1", variables.get(0).getVariableName());
        assertEquals(1, variables.get(0).getValue());
        assertEquals("outputVariable1", variables.get(1).getVariableName());
        assertEquals("result1", variables.get(1).getValue());
    }

    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskNoHitsErrorProcess.bpmn20.xml"})
    public void testDecisionNotFound() {
        deleteAllDmnDeployments();
        try {
            runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess", Collections.singletonMap("inputVariable1", (Object) 2));
            fail("Expected Exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Decision table for key [decision1] was not found"));
        }
    }

    protected void deleteAllDmnDeployments() {
        DmnEngineConfiguration dmnEngineConfiguration = (DmnEngineConfiguration) flowableRule.getProcessEngine().getProcessEngineConfiguration().getEngineConfigurations()
            .get(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
        dmnEngineConfiguration.getDmnRepositoryService().createDeploymentQuery().list().stream()
        .forEach(
            deployment -> dmnEngineConfiguration.getDmnRepositoryService().deleteDeployment(deployment.getId())
        );
    }

}
