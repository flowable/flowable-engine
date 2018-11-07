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

import org.flowable.dmn.engine.impl.test.ResourceFlowableDmnTestCase;
import org.flowable.dmn.engine.test.DmnDeployment;

/**
 * @author Tijs Rademakers
 */
public class HistoryNoneTest extends ResourceFlowableDmnTestCase {
    
    public HistoryNoneTest() {
        super("historynone.flowable.dmn.cfg.xml");
    }

    @DmnDeployment
    public void testFirstHitPolicy() throws Exception {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .executeWithSingleResult();
        
        assertEquals(0, historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").list().size());
    }
}
