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
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.flowable.engine.common.api.FlowableException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class HitPolicyOutputOrder extends AbstractHitPolicy implements ComposeRuleOutputBehavior, ComposeDecisionTableOutputBehavior {

    @Override
    public String getHitPolicyName() {
        return HitPolicy.OUTPUT_ORDER.getValue();
    }

    @Override
    public void composeRuleOutput(int outputNumber, String outputVariableId, Object executionVariable, MvelExecutionContext executionContext) {
        Object resultVariable = executionContext.getResultVariables().get(outputVariableId);
        if (resultVariable == null) {
            resultVariable = new ArrayList<>();
        }
        if (resultVariable instanceof List) {
            ((List) resultVariable).add(executionVariable);

            // add result variable
            executionContext.addOutputResultVariable(outputNumber, outputVariableId, resultVariable);
        } else {
            throw new FlowableException(String.format("HitPolicy: %s has wrong output variable type", getHitPolicyName()));
        }
    }


    @Override
    public void composeDecisionTableOutput(Map<Integer, List<RuleOutputClauseContainer>> validRuleOutputEntries, MvelExecutionContext executionContext) {
        if (executionContext.getResultVariables() != null) {
            for (Map.Entry<Integer, Object> entry : executionContext.getOutputVariables().entrySet()) {
                if (entry.getValue() == null) {
                    break;
                }

                if (!executionContext.getOutputValues().containsKey(entry.getKey()) || executionContext.getOutputValues().get(entry.getKey()).isEmpty()) {
                    throw new FlowableException(String.format("HitPolicy: %s; no output values present for output: %d", getHitPolicyName(), entry.getKey()));
                }

                if (entry.getValue() instanceof List) {
                    List<Comparable> outputValues = (List<Comparable>) entry.getValue();
                    List<Object> outputValueList = executionContext.getOutputValues().get(entry.getKey());
                    Collections.sort(outputValues, new OutputOrderComparator<>(outputValueList.toArray(new Comparable[outputValueList.size()])));
                } else {
                    throw new FlowableException(String.format("HitPolicy: %s has wrong output variable type", getHitPolicyName()));
                }
            }
        }
    }
}
