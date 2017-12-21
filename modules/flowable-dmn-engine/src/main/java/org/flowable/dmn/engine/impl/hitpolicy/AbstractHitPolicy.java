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
package org.flowable.dmn.engine.impl.hitpolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.engine.impl.el.ELExecutionContext;

/**
 * @author Yvo Swillens
 *
 * (Abstact) base class for all Hit Policy behaviors
 */
public abstract class AbstractHitPolicy implements ContinueEvaluatingBehavior, ComposeRuleResultBehavior, ComposeDecisionResultBehavior {

    /**
     * Returns the name for the specific Hit Policy behavior
     */
    public abstract String getHitPolicyName();

    /**
     * Default behavior for ContinueEvaluating behavior
     */

    @Override
    public boolean shouldContinueEvaluating(boolean ruleResult) {
        return true;
    }

    /**
     * Default behavior for ComposeRuleOutput behavior
     */
    @Override
    public void composeRuleResult(int ruleNumber, String outputName, Object outputValue, ELExecutionContext executionContext) {
        executionContext.addRuleResult(ruleNumber, outputName, outputValue);
    }

    /**
     * Default behavior for ComposeRuleOutput behavior
     */
    @Override
    public void composeDecisionResults(ELExecutionContext executionContext) {
        List<Map<String, Object>> decisionResults = new ArrayList<>(executionContext.getRuleResults().values());
        executionContext.getAuditContainer().setDecisionResult(decisionResults);
    }
}
