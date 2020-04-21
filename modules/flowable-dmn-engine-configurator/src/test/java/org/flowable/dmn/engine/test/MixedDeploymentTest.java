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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.DefaultTenantProvider;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnHistoricDecisionExecution;
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
 * @author Filip Hrisafov
 */
public class MixedDeploymentTest extends AbstractFlowableDmnEngineConfiguratorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void deploySingleProcessAndDecisionTable() {
        try {
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
        } finally {
            deleteAllDmnDeployments();
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void testDecisionTaskExecution() {
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess", Collections.singletonMap("inputVariable1", (Object) 1));
            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                            .processInstanceId(processInstance.getId()).orderByVariableName().asc().list();
            
            assertEquals("inputVariable1", variables.get(0).getVariableName());
            assertEquals(1, variables.get(0).getValue());
            assertEquals("outputVariable1", variables.get(1).getVariableName());
            assertEquals("result1", variables.get(1).getValue());
        } finally {
            deleteAllDmnDeployments();
        }
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
        } finally {
            deleteAllDmnDeployments();
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskNoHitsErrorProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void testNoHitsDecisionTask() {
        try {
            runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess", Collections.singletonMap("inputVariable1", (Object) 2));
            fail("Expected Exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("did not hit any rules for the provided input"));
        } finally {
            deleteAllDmnDeployments();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskNoHitsErrorProcess.bpmn20.xml"})
    public void testDecisionNotFound() {
        try {
            assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess", Collections.singletonMap("inputVariable1", (Object) 2)))
                    .isInstanceOf(FlowableException.class)
                    .hasMessageContaining("No decision found for key: decision1")
                    .hasMessageNotContaining("tenant");
        } finally {
            deleteAllDmnDeployments();
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
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessFallBackToDefaultTenant.bpmn20.xml" },
        tenantId = "someTenant"
    )
    public void testDecisionTaskExecutionWithGlobalTenantFallback() {
        deployDecisionWithGlobalTenantFallback();
    }

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml" }
    )
    public void testDecisionTaskExecutionInAnotherDeploymentAndTenantDefaultBehavior() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("Process definition with key 'oneDecisionTaskProcess' and tenantId 'flowable' was not found");

        deployDecisionAndAssertProcessExecuted();
    }

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessFallBackToDefaultTenantFalse.bpmn20.xml" },
        tenantId = "flowable"
    )
    public void testDecisionTaskExecutionInAnotherDeploymentAndTenantFalse() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("No decision found for key: decision1");
        this.expectedException.expectMessage("and tenant id: flowable");

        deployDecisionAndAssertProcessExecuted();
    }

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessFallBackToDefaultTenantFalse.bpmn20.xml" },
        tenantId = "flowable"
    )
    public void testDecisionTaskExecutionInAnotherDeploymentAndTenantFallbackFalseWithoutDeployment() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("No decision found for key: decision1");
        this.expectedException.expectMessage("and tenant id: flowable");

        deleteAllDmnDeployments();
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().
            addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn").
            tenantId("anotherTenant").
            deploy();
        try {
            assertDmnProcessExecuted();
        } finally {
            this.repositoryService.deleteDeployment(deployment.getId(), true);
            deleteAllDmnDeployments();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessFallBackToDefaultTenant.bpmn20.xml" },
        tenantId = "flowable"
    )
    public void testDecisionTaskExecutionInAnotherDeploymentAndTenantFallbackTrueWithoutDeployment() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("No decision found for key: decision1. There was also no fall back decision table found without tenant.");

        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().
            addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn").
            tenantId("anotherTenant").
            deploy();
        try {
            assertDmnProcessExecuted();
        } finally {
            this.repositoryService.deleteDeployment(deployment.getId(), true);
            deleteAllDmnDeployments();
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessSameDeployment.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn"
    })
    public void testDecisionTaskExecutionWithSameDeployment() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("inputVariable1", 1)
                .start();

        assertThat(historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .variableName("outputVariable1")
                .singleResult())
                .extracting(HistoricVariableInstance::getValue)
                .isEqualTo("result1");

        try {
            // Same deployment decision should be used from parent deployment
            DmnEngines.getDefaultDmnEngine()
                    .getDmnRepositoryService()
                    .createDeployment()
                    .addClasspathResource("org/flowable/dmn/engine/test/deployment/simpleV2.dmn")
                    .deploy();

            processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneDecisionTaskProcess")
                    .variable("inputVariable1", 1)
                    .start();

            assertThat(historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("outputVariable1")
                    .singleResult())
                    .extracting(HistoricVariableInstance::getValue)
                    .isEqualTo("result1");

        } finally {
            deleteAllDmnDeployments();
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessSameDeployment.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn"
    }, tenantId = "flowable")
    public void testDecisionTaskExecutionWithSameDeploymentInTenant() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("inputVariable1", 1)
                .tenantId("flowable")
                .start();

        assertThat(historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .variableName("outputVariable1")
                .singleResult())
                .extracting(HistoricVariableInstance::getValue)
                .isEqualTo("result1");

        try {
            // Same deployment decision should be used from parent deployment
            DmnEngines.getDefaultDmnEngine()
                    .getDmnRepositoryService()
                    .createDeployment()
                    .addClasspathResource("org/flowable/dmn/engine/test/deployment/simpleV2.dmn")
                    .tenantId("flowable")
                    .deploy();

            processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneDecisionTaskProcess")
                    .variable("inputVariable1", 1)
                    .tenantId("flowable")
                    .start();

            assertThat(historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("outputVariable1")
                    .singleResult())
                    .extracting(HistoricVariableInstance::getValue)
                    .isEqualTo("result1");

        } finally {
            deleteAllDmnDeployments();
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn"
    })
    public void testDecisionTaskExecutionWithSameDeploymentGlobal() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("inputVariable1", 1)
                .start();

        assertThat(historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .variableName("outputVariable1")
                .singleResult())
                .extracting(HistoricVariableInstance::getValue)
                .isEqualTo("result1");

        try {
            // Same deployment decision should be used from parent deployment
            DmnEngines.getDefaultDmnEngine()
                    .getDmnRepositoryService()
                    .createDeployment()
                    .addClasspathResource("org/flowable/dmn/engine/test/deployment/simpleV2.dmn")
                    .deploy();

            processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneDecisionTaskProcess")
                    .variable("inputVariable1", 1)
                    .start();

            assertThat(historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("outputVariable1")
                    .singleResult())
                    .extracting(HistoricVariableInstance::getValue)
                    .isEqualTo("result1");

        } finally {
            deleteAllDmnDeployments();
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessSameDeploymentFalse.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn"
    })
    public void testDecisionTaskExecutionWithSameDeploymentFalse() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("inputVariable1", 1)
                .start();

        assertThat(historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .variableName("outputVariable1")
                .singleResult())
                .extracting(HistoricVariableInstance::getValue)
                .isEqualTo("result1");

        try {
            // Latest decision should be used
            DmnEngines.getDefaultDmnEngine()
                    .getDmnRepositoryService()
                    .createDeployment()
                    .addClasspathResource("org/flowable/dmn/engine/test/deployment/simpleV2.dmn")
                    .deploy();

            processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneDecisionTaskProcess")
                    .variable("inputVariable1", 1)
                    .start();

            assertThat(historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("outputVariable1")
                    .singleResult())
                    .extracting(HistoricVariableInstance::getValue)
                    .isEqualTo("result1V2");

        } finally {
            deleteAllDmnDeployments();
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessSameDeploymentFalse.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn"
    }, tenantId = "flowable")
    public void testDecisionTaskExecutionWithSameDeploymentFalseInTenant() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("inputVariable1", 1)
                .tenantId("flowable")
                .start();

        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("outputVariable1")
                .singleResult())
                .extracting(HistoricVariableInstance::getValue)
                .isEqualTo("result1");

        try {
            // Latest decision should be used
            DmnEngines.getDefaultDmnEngine()
                    .getDmnRepositoryService()
                    .createDeployment()
                    .addClasspathResource("org/flowable/dmn/engine/test/deployment/simpleV2.dmn")
                    .tenantId("flowable")
                    .deploy();

            processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneDecisionTaskProcess")
                    .variable("inputVariable1", 1)
                    .tenantId("flowable")
                    .start();

            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("outputVariable1")
                    .singleResult())
                    .extracting(HistoricVariableInstance::getValue)
                    .isEqualTo("result1V2");

        } finally {
            deleteAllDmnDeployments();
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
            deleteAllDmnDeployments();
        }
    }
    
    protected void deployDecisionWithGlobalTenantFallback() {
        DmnEngineConfiguration dmnEngineConfiguration = (DmnEngineConfiguration) processEngineConfiguration.getEngineConfigurations().get(
                        EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
        
        DefaultTenantProvider originalDefaultTenantProvider = dmnEngineConfiguration.getDefaultTenantProvider();
        dmnEngineConfiguration.setFallbackToDefaultTenant(true);
        dmnEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().
            addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn").
            tenantId("defaultFlowable").
            deploy();
        
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId(
                "oneDecisionTaskProcess", Collections.singletonMap("inputVariable1", (Object) 1), "someTenant");
            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId()).orderByVariableName().asc().list();

            assertEquals("inputVariable1", variables.get(0).getVariableName());
            assertEquals(1, variables.get(0).getValue());
            assertEquals("outputVariable1", variables.get(1).getVariableName());
            assertEquals("result1", variables.get(1).getValue());
            
            DmnHistoricDecisionExecution decisionExecution = dmnEngineConfiguration.getDmnHistoryService()
                            .createHistoricDecisionExecutionQuery()
                            .instanceId(processInstance.getId())
                            .singleResult();
            
            assertNotNull(decisionExecution);
            assertEquals("someTenant", decisionExecution.getTenantId());
            
        } finally {
            dmnEngineConfiguration.setFallbackToDefaultTenant(false);
            dmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
            this.repositoryService.deleteDeployment(deployment.getId(), true);
            deleteAllDmnDeployments();
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
        
        DmnEngineConfiguration dmnEngineConfiguration = (DmnEngineConfiguration) processEngineConfiguration.getEngineConfigurations().get(
                        EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
        DmnHistoricDecisionExecution decisionExecution = dmnEngineConfiguration.getDmnHistoryService()
                        .createHistoricDecisionExecutionQuery()
                        .instanceId(processInstance.getId())
                        .singleResult();
        
        assertNotNull(decisionExecution);
        assertEquals("flowable", decisionExecution.getTenantId());
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
