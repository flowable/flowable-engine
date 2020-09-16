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

import java.util.Map;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
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
    protected ProcessEngineConfiguration processEngineConfiguration;

    @BeforeEach
    void setUp(ProcessEngineConfiguration processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
        runtimeService = processEngineConfiguration.getRuntimeService();
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

}
