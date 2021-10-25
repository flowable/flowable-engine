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
package org.flowable.dmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.engine.DecisionTableVariableManager;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Valentin Zickner
 */
@FlowableTest
@ConfigurationResource("org/flowable/bpmn/test/runtime/DmnTaskTest.cfg.xml")
public class DmnTaskTest {

    protected RuntimeService runtimeService;
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    @BeforeEach
    void setUp(ProcessEngineConfiguration processEngineConfiguration) {
        this.processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngineConfiguration;
        runtimeService = processEngineConfiguration.getRuntimeService();
    }

    @AfterEach
    protected void deleteAllDmnDeployments() {
        DmnEngineConfiguration dmnEngineConfiguration = (DmnEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
        dmnEngineConfiguration.getDmnRepositoryService().createDeploymentQuery().list()
                .forEach(
                        deployment -> dmnEngineConfiguration.getDmnRepositoryService().deleteDeployment(deployment.getId())
                );
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.firstHit.dmn"})
    void withFirstHitPolicy_ensureItemIsReturned() {
        ProcessInstance processInstance = this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("testInput", "second")
                .start();
        Map<String, Object> processVariables = processInstance.getProcessVariables();
        assertThat(processVariables).containsEntry("testOutput", 2d);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.outputOrder.dmn"})
    void withOutputOrder_ensureListOfItemIsReturnedEvenIfOnlyOneRowIsHit() {
        ProcessInstance processInstance = this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("testInput", "second")
                .start();
        Map<String, Object> processVariables = processInstance.getProcessVariables();
        Object resultObject = processVariables.get("DecisionTable");
        assertThat(resultObject).isInstanceOf(ArrayNode.class);
        ArrayNode result = (ArrayNode) resultObject;
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(ObjectNode.class);
        assertThat(result.get(0).get("testOutput")).isInstanceOf(DoubleNode.class);
        assertThat(result.get(0).get("testOutput").asDouble()).isEqualTo(2.0);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.ruleOrder.dmn"})
    void withRuleOrder_ensureListOfItemIsReturnedEvenIfOnlyOneRowIsHit() {
        ProcessInstance processInstance = this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("testInput", "second")
                .start();
        Map<String, Object> processVariables = processInstance.getProcessVariables();
        Object resultObject = processVariables.get("DecisionTable");
        assertThat(resultObject).isInstanceOf(ArrayNode.class);
        ArrayNode result = (ArrayNode) resultObject;
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(ObjectNode.class);
        assertThat(result.get(0).get("testOutput")).isInstanceOf(DoubleNode.class);
        assertThat(result.get(0).get("testOutput").asDouble()).isEqualTo(2.0);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.ruleOrder.dmn"})
    void withRuleOrderAndBackwardsCompatibilityFlag_ensureListOfItemIsReturnedEvenIfOnlyOneRowIsHit() {
        processEngineConfiguration.setAlwaysUseArraysForDmnMultiHitPolicies(false);
        ProcessInstance processInstance = this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("testInput", "second")
                .start();
        Map<String, Object> processVariables = processInstance.getProcessVariables();
        Object resultObject = processVariables.get("DecisionTable");
        assertThat(resultObject).isNull();
        assertThat(processVariables).containsEntry("testOutput", 2.0);

        processEngineConfiguration.setAlwaysUseArraysForDmnMultiHitPolicies(true);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.ruleOrder.dmn"})
    void withRuleOrder_ensureListOfItemIsReturnedEvenIfNoRowIsHit() {
        ProcessInstance processInstance = this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("testInput", "fourth")
                .start();
        Map<String, Object> processVariables = processInstance.getProcessVariables();
        Object resultObject = processVariables.get("DecisionTable");
        assertThat(resultObject).isInstanceOf(ArrayNode.class);
        ArrayNode result = (ArrayNode) resultObject;
        assertThat(result).isEmpty();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.ruleOrder.dmn"})
    public void withCustomDecisionTableManager_ensureDecisionTableManagerIsCalled() {
        DecisionTableVariableManager originalDecisionTableVariableManager = processEngineConfiguration.getDecisionTableVariableManager();
        final boolean[] setVariableOnPlanItemCalled = {false};
        processEngineConfiguration.setDecisionTableVariableManager(new DecisionTableVariableManager() {
            @Override
            public void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
            }

            @Override
            public void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper, boolean multipleResults) {
                setVariableOnPlanItemCalled[0] = true;
            }

            @Override
            public void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
            }
        });
        this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("testInput", "second")
                .start();
        assertThat(setVariableOnPlanItemCalled[0]).isTrue();
        processEngineConfiguration.setDecisionTableVariableManager(originalDecisionTableVariableManager);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.ruleOrder.dmn"})
    public void withCustomDecisionTableManagerAndWithOldMethodMethod_ensureDecisionTableManagerIsCalled() {
        DecisionTableVariableManager originalDecisionTableVariableManager = processEngineConfiguration.getDecisionTableVariableManager();
        final boolean[] setVariableOnPlanItemCalled = {false};
        processEngineConfiguration.setDecisionTableVariableManager(new DecisionTableVariableManager() {
            @Override
            public void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
                setVariableOnPlanItemCalled[0] = true;
            }

            @Override
            public void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
            }
        });
        this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionTaskProcess")
                .variable("testInput", "second")
                .start();
        assertThat(setVariableOnPlanItemCalled[0]).isTrue();
        processEngineConfiguration.setDecisionTableVariableManager(originalDecisionTableVariableManager);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionServiceTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.multipleResults.dmn"
    })
    public void withCustomDecisionTableManagerAndOldDecisionService_ensureDecisionTableManagerIsCalled() {
        DecisionTableVariableManager originalDecisionTableVariableManager = processEngineConfiguration.getDecisionTableVariableManager();
        final boolean[] setCalled = {false};
        processEngineConfiguration.setDecisionTableVariableManager(new DecisionTableVariableManager() {
            @Override
            public void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
            }

            @Override
            public void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
                setCalled[0] = true;
            }
        });
        this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionServiceTaskProcess")
                .variable("a", "1")
                .variable("b", "1")
                .variable("c", "1")
                .start();
        assertThat(setCalled[0]).isTrue();
        processEngineConfiguration.setDecisionTableVariableManager(originalDecisionTableVariableManager);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionServiceTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.multipleResults.dmn"
    })
    public void withCustomDecisionTableManagerAndNewDecisionService_ensureDecisionTableManagerIsCalledAndMultipleResultsIsTrue() {
        DecisionTableVariableManager originalDecisionTableVariableManager = processEngineConfiguration.getDecisionTableVariableManager();
        final boolean[] setCalled = {false};
        processEngineConfiguration.setDecisionTableVariableManager(new DecisionTableVariableManager() {
            @Override
            public void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
            }

            @Override
            public void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
            }

            @Override
            public void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey,
                    DelegateExecution execution, ObjectMapper objectMapper, boolean multipleResults) {
                setCalled[0] = true;
                assertThat(multipleResults).isTrue();
            }
        });
        this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionServiceTaskProcess")
                .variable("a", "1")
                .variable("b", "1")
                .variable("c", "1")
                .start();
        assertThat(setCalled[0]).isTrue();
        processEngineConfiguration.setDecisionTableVariableManager(originalDecisionTableVariableManager);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionServiceTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.singleResult.dmn"
    })
    public void withCustomDecisionTableManagerAndNewDecisionServiceOneResult_ensureDecisionTableManagerIsCalledAndMultipleResultsIsFalse() {
        DecisionTableVariableManager originalDecisionTableVariableManager = processEngineConfiguration.getDecisionTableVariableManager();
        final boolean[] setCalled = {false};
        processEngineConfiguration.setDecisionTableVariableManager(new DecisionTableVariableManager() {
            @Override
            public void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
            }

            @Override
            public void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
            }

            @Override
            public void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey,
                    DelegateExecution execution, ObjectMapper objectMapper, boolean multipleResults) {
                setCalled[0] = true;
                assertThat(multipleResults).isFalse();
            }
        });
        this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionServiceTaskProcess")
                .variable("a", "1")
                .variable("b", "1")
                .variable("c", "1")
                .start();
        assertThat(setCalled[0]).isTrue();
        processEngineConfiguration.setDecisionTableVariableManager(originalDecisionTableVariableManager);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionServiceTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.multipleTablesAsResult.dmn"
    })
    public void withCustomDecisionTableManagerAndNewDecisionServiceMultipleResultTables_ensureDecisionTableManagerIsCalledAndMultipleResultsIsTrue() {
        DecisionTableVariableManager originalDecisionTableVariableManager = processEngineConfiguration.getDecisionTableVariableManager();
        final boolean[] setCalled = {false};
        processEngineConfiguration.setDecisionTableVariableManager(new DecisionTableVariableManager() {
            @Override
            public void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
            }

            @Override
            public void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
            }

            @Override
            public void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey,
                    DelegateExecution execution, ObjectMapper objectMapper, boolean multipleResults) {
                setCalled[0] = true;
                assertThat(multipleResults).isTrue();
            }
        });
        this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionServiceTaskProcess")
                .variable("a", "1")
                .variable("b", "1")
                .variable("c", "1")
                .start();
        assertThat(setCalled[0]).isTrue();
        processEngineConfiguration.setDecisionTableVariableManager(originalDecisionTableVariableManager);
    }


    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionServiceTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.multipleResults.dmn"
    })
    public void withMultipleResultsAndOneTable_ensureDecisionTableIsAList() {
        Map<String, Object> processVariables = this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionServiceTaskProcess")
                .variable("a", "1")
                .variable("b", "1")
                .variable("c", "1")
                .start()
                .getProcessVariables();
        Object result = processVariables.get("decisionServiceTest");

        assertThat(result).isInstanceOf(ObjectNode.class);
        ObjectNode resultNode = (ObjectNode) result;
        JsonNode decisionResult = resultNode.get("decision3");
        assertThat(decisionResult).isInstanceOf(ArrayNode.class);
        assertThat(decisionResult.size()).isEqualTo(4);
        assertThat(decisionResult.get(0).get("g").asText()).isEqualTo("1000");
        assertThat(decisionResult.get(1).get("g").asText()).isEqualTo("100");
        assertThat(decisionResult.get(2).get("g").asText()).isEqualTo("10");
        assertThat(decisionResult.get(3).get("g").asText()).isEqualTo("1");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionServiceTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.multipleResults.dmn"
    })
    public void withMultipleResultButOnlyOneMatchesAndOneTable_ensureDecisionTableIsAList() {
        Map<String, Object> processVariables = this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionServiceTaskProcess")
                .variable("a", "100")
                .variable("b", "100")
                .variable("c", "100")
                .start()
                .getProcessVariables();
        Object result = processVariables.get("decisionServiceTest");

        assertThat(result).isInstanceOf(ObjectNode.class);
        ObjectNode resultNode = (ObjectNode) result;
        JsonNode decisionResult = resultNode.get("decision3");
        assertThat(decisionResult).isInstanceOf(ArrayNode.class);
        assertThat(decisionResult.size()).isEqualTo(1);
        assertThat(decisionResult.get(0).get("g").asText()).isEqualTo("1");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/bpmn/test/runtime/DmnTaskTest.oneDecisionServiceTaskProcess.bpmn20.xml",
            "org/flowable/bpmn/test/runtime/DmnTaskTest.singleResult.dmn"
    })
    public void withSingleResult_ensureDecisionTableIsAString() {
        Map<String, Object> processVariables = this.runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneDecisionServiceTaskProcess")
                .variable("a", "1")
                .variable("b", "1")
                .variable("c", "1")
                .start()
                .getProcessVariables();
        Object result = processVariables.get("g");

        assertThat(result).isEqualTo("1000");
    }

}
