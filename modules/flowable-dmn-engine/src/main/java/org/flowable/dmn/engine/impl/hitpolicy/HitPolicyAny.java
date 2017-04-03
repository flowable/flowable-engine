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

import org.flowable.dmn.api.ExpressionExecution;
import org.flowable.dmn.api.RuleExecutionAuditContainer;
import org.flowable.dmn.engine.impl.context.Context;
import org.flowable.dmn.engine.impl.mvel.MvelExecutionContext;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.engine.common.api.FlowableException;

import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class HitPolicyAny extends AbstractHitPolicy {

    @Override
    public String getHitPolicyName() {
        return HitPolicy.ANY.getValue();
    }

    @Override
    public void evaluateRuleConclusionValidity(Object resultValue, int ruleNumber, int ruleConclusionNumber, MvelExecutionContext executionContext) {
        for (Map.Entry<Integer, RuleExecutionAuditContainer> entry : executionContext.getAuditContainer().getRuleExecutions().entrySet()) {
            if (entry.getKey().equals(ruleNumber) == false &&
                !entry.getValue().getConclusionResults().isEmpty() &&
                entry.getValue().getConclusionResults().size() >= ruleConclusionNumber) {

                ExpressionExecution expressionExecution = entry.getValue().getConclusionResults().get(ruleConclusionNumber);

                // conclusion value must be the same as for other valid rules
                if (expressionExecution != null && expressionExecution.getResult() != null && !expressionExecution.getResult().equals(resultValue)) {
                    String hitPolicyViolatedMessage = String.format("HitPolicy ANY violated: conclusion %d of rule %d is not the same as for rule %d", ruleConclusionNumber, ruleNumber, entry.getKey());

                    if (Context.getDmnEngineConfiguration().isStrictMode()) {
                        throw new FlowableException("HitPolicy ANY violated");
                    }
                }
            }
        }
    }
}
