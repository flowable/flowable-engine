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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yvo Swillens
 */
public class HitPolicyRuleOrderTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeployment
    public void ruleOrderHitPolicy() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 13)
                .execute();
        
        assertEquals(2, result.size());
        assertEquals("result2", result.get(0).get("outputVariable1"));
        assertEquals("result4", result.get(1).get("outputVariable1"));
    }
}
