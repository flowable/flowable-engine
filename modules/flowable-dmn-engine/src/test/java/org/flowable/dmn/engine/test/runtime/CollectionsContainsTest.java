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
package org.flowable.dmn.engine.test.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class CollectionsContainsTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    public ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/contains_IN.dmn")
    public void testContainsTrue() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        List inputVariable1 = Arrays.asList("test1", "test2", "test3");
        List inputVariable2 = Arrays.asList("test1", "test2", "test3");
        List inputVariable3 = Arrays.asList("test1", "test2");
        List inputVariable4 = Arrays.asList(5L, 10L, 20L, 50L);
        List inputVariable5 = Arrays.asList("test3", "test5");

        ArrayNode arrayNode1 = objectMapper.createArrayNode().add("test1").add("test2").add("test3");
        ArrayNode arrayNode2 = objectMapper.createArrayNode().add(5L).add(10L).add(20L).add(50L);
        ArrayNode arrayNode3 = objectMapper.createArrayNode().add(5.5D).add(10.5D).add(20.5D).add(50.5D);
        ArrayNode arrayNode4 = objectMapper.createArrayNode().add(5.5555F).add(10.5555F).add(20.5555F).add(50.5555F);

        processVariablesInput.put("collection1", inputVariable1);
        processVariablesInput.put("collection2", inputVariable2);
        processVariablesInput.put("collection3", inputVariable3);
        processVariablesInput.put("collection4", inputVariable4);
        processVariablesInput.put("collection5", inputVariable5);
        processVariablesInput.put("arrayNode1", arrayNode1);
        processVariablesInput.put("arrayNode2", arrayNode2);
        processVariablesInput.put("arrayNode3", arrayNode3);
        processVariablesInput.put("arrayNode4", arrayNode4);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decision")
            .variables(processVariablesInput)
            .executeWithAuditTrail();

        Assert.assertFalse(result.isFailed());
        Assert.assertTrue(result.getRuleExecutions().get(1).isValid());
        Assert.assertTrue(result.getRuleExecutions().get(2).isValid());
        Assert.assertTrue(result.getRuleExecutions().get(3).isValid());
        Assert.assertTrue(result.getRuleExecutions().get(4).isValid());
        Assert.assertTrue(result.getRuleExecutions().get(7).isValid());
        Assert.assertTrue(result.getRuleExecutions().get(8).isValid());
        Assert.assertTrue(result.getRuleExecutions().get(11).isValid());
        Assert.assertTrue(result.getRuleExecutions().get(13).isValid());
        Assert.assertTrue(result.getRuleExecutions().get(14).isValid());
        Assert.assertTrue(result.getRuleExecutions().get(15).isValid());
        Assert.assertTrue(result.getRuleExecutions().get(16).isValid());
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/contains_IN.dmn")
    public void testContainsFalse() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        List inputVariable1 = Arrays.asList("test1", "test2", "test3");
        List inputVariable2 = Arrays.asList("test1", "test2", "test3");
        List inputVariable3 = Arrays.asList("test1", "test2");
        List inputVariable4 = Arrays.asList(5L, 10L, 20L, 50L);
        List inputVariable5 = Arrays.asList("test3", "test5");

        ArrayNode arrayNode1 = objectMapper.createArrayNode().add("test1").add("test2").add("test3");
        ArrayNode arrayNode2 = objectMapper.createArrayNode().add(5L).add(10L).add(20L).add(50L);
        ArrayNode arrayNode3 = objectMapper.createArrayNode().add(5.5D).add(10.5D).add(20.5D).add(50.5D);
        ArrayNode arrayNode4 = objectMapper.createArrayNode().add(5.5555F).add(10.5555F).add(20.5555F).add(50.5555F);

        processVariablesInput.put("collection1", inputVariable1);
        processVariablesInput.put("collection2", inputVariable2);
        processVariablesInput.put("collection3", inputVariable3);
        processVariablesInput.put("collection4", inputVariable4);
        processVariablesInput.put("collection5", inputVariable5);
        processVariablesInput.put("arrayNode1", arrayNode1);
        processVariablesInput.put("arrayNode2", arrayNode2);
        processVariablesInput.put("arrayNode3", arrayNode3);
        processVariablesInput.put("arrayNode4", arrayNode4);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decision")
            .variables(processVariablesInput)
            .executeWithAuditTrail();

        Assert.assertFalse(result.isFailed());
        Assert.assertFalse(result.getRuleExecutions().get(5).isValid());
        Assert.assertFalse(result.getRuleExecutions().get(6).isValid());
        Assert.assertFalse(result.getRuleExecutions().get(9).isValid());
        Assert.assertFalse(result.getRuleExecutions().get(10).isValid());
        Assert.assertFalse(result.getRuleExecutions().get(12).isValid());
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/contains_IN.dmn")
    public void testContainsNotACollection() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("collection1", "not a collection");

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decision")
            .variables(processVariablesInput)
            .executeWithAuditTrail();

        Assert.assertTrue(result.isFailed());
    }


}
