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
package org.flowable.dmn.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.dmn.engine.test.BaseFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnConfigurationResource;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
@DmnConfigurationResource(value = "historynone.flowable.dmn.cfg.xml")
class HistoryNoneTest extends BaseFlowableDmnTest {

    @Test
    @DmnDeployment
    public void testFirstHitPolicy() throws Exception {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .executeWithSingleResult();

        assertThat(historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").list()).isEmpty();
    }
}
