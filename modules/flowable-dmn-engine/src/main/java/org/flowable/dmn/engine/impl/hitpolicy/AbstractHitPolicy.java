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

import org.flowable.dmn.engine.impl.mvel.MvelExecutionContext;

/**
 * @author Yvo Swillens
 */
public abstract class AbstractHitPolicy implements HitPolicyBehavior {

    @Override
    public boolean shouldContinueEvaluating(boolean ruleResult) {
        return true;
    }

    @Override
    public void evaluateRuleValidity(int ruleNumber, MvelExecutionContext executionContext) {
        // default: do nothing
    }

    @Override
    public void evaluateRuleConclusionValidity(Object resultValue, int ruleNumber, int ruleConclusionNumber, MvelExecutionContext executionContext) {
        // default: do nothing
    }

    @Override
    public void composeOutput(String outputVariableId, Object executionVariable, MvelExecutionContext executionContext) {
        executionContext.getResultVariables().put(outputVariableId, executionVariable);
    }
}
