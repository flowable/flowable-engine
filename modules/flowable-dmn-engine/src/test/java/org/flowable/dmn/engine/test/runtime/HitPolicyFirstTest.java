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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.engine.test.BaseFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.junit.jupiter.api.Test;

/**
 * @author Yvo Swillens
 */
class HitPolicyFirstTest extends BaseFlowableDmnTest {

    @Test
    @DmnDeployment
    public void firstHitPolicy() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .executeWithSingleResult();

        assertThat(result)
                .contains(
                        entry("outputVariable1", "gt 10"),
                        entry("outputVariable2", "result2")
                );
    }

    @Test
    @DmnDeployment
    public void firstHitPolicyNoMatch() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 50)
                .executeWithSingleResult();

        assertThat(result).isNull();
    }
}
