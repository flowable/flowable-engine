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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.dmn.engine.impl.el.ELExecutionContext;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.HitPolicy;

/**
 * @author Yvo Swillens
 */
public class HitPolicyAny extends AbstractHitPolicy implements ComposeDecisionResultBehavior {

    @Override
    public String getHitPolicyName() {
        return HitPolicy.ANY.getValue();
    }

    @Override
    public void composeDecisionResults(final ELExecutionContext executionContext) {

        boolean validationFailed = false;

        for (Map.Entry<Integer, Map<String, Object>> ruleResults : executionContext.getRuleResults().entrySet()) {

            for (Map.Entry<Integer, Map<String, Object>> otherRuleResults : executionContext.getRuleResults().entrySet()) {

                if (!otherRuleResults.getKey().equals(ruleResults.getKey())) {

                    for (Map.Entry<String, Object> outputValues : otherRuleResults.getValue().entrySet()) {
                        if (!ruleResults.getValue().containsKey(outputValues.getKey()) ||
                            (ruleResults.getValue().containsKey(outputValues.getKey()) && !outputValues.getValue().equals(ruleResults.getValue().get(outputValues.getKey())))) {

                            String hitPolicyViolatedMessage = String.format("HitPolicy %s violated; both rule %d and %d are valid but output %s has different values.",
                                getHitPolicyName(), otherRuleResults.getKey(), ruleResults.getKey(), outputValues.getKey());

                            if (CommandContextUtil.getDmnEngineConfiguration().isStrictMode()) {
                                executionContext.getAuditContainer().getRuleExecutions().get(otherRuleResults.getKey()).setExceptionMessage(hitPolicyViolatedMessage);
                                executionContext.getAuditContainer().getRuleExecutions().get(ruleResults.getKey()).setExceptionMessage(hitPolicyViolatedMessage);

                                throw new FlowableException(String.format("HitPolicy %s violated.", getHitPolicyName()));
                            } else {
                                validationFailed = true;

                                executionContext.getAuditContainer().getRuleExecutions().get(otherRuleResults.getKey()).setValidationMessage(hitPolicyViolatedMessage);
                                executionContext.getAuditContainer().getRuleExecutions().get(ruleResults.getKey()).setValidationMessage(hitPolicyViolatedMessage);

                                break;
                            }
                        }
                    }
                }
            }
        }

        List<Map<String, Object>> ruleResults = new ArrayList<>(executionContext.getRuleResults().values());
        if (!ruleResults.isEmpty()) {
            if (CommandContextUtil.getDmnEngineConfiguration().isStrictMode() == false && validationFailed) {
                executionContext.getAuditContainer().setValidationMessage(String.format("HitPolicy %s violated; multiple valid rules with different outcomes. Setting last valid rule result as final result.", getHitPolicyName()));
            }
            executionContext.getAuditContainer().addDecisionResultObject(ruleResults.get(ruleResults.size() - 1));
        }
    }

}
