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

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class CollectionsContainsReversedTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/contains_IN_reversed.dmn")
    public void testContainsTrue() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        Person customerOne = new Person();
        customerOne.setName("test1");
        customerOne.setAge(10L);

        processVariablesInput.put("customerOne", customerOne);

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
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/contains_IN_reversed.dmn")
    public void testContainsFalse() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        Person customerOne = new Person();
        customerOne.setName("test3");
        customerOne.setAge(11L);

        processVariablesInput.put("customerOne", customerOne);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decision")
            .variables(processVariablesInput)
            .executeWithAuditTrail();

        Assert.assertFalse(result.isFailed());
        Assert.assertFalse(result.getRuleExecutions().get(1).isValid());
        Assert.assertFalse(result.getRuleExecutions().get(2).isValid());
        Assert.assertFalse(result.getRuleExecutions().get(3).isValid());
        Assert.assertFalse(result.getRuleExecutions().get(4).isValid());
    }

    class Person {
        private String name;
        private Long age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getAge() {
            return age;
        }

        public void setAge(Long age) {
            this.age = age;
        }
    }
}
