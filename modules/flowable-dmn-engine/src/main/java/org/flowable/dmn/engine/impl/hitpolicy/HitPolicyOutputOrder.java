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

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.flowable.dmn.engine.impl.context.Context;
import org.flowable.dmn.engine.impl.mvel.MvelExecutionContext;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.flowable.engine.common.api.FlowableException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
            executionContext.addOutputResult(outputNumber, outputVariableId, resultVariable);
        } else {
            throw new FlowableException(String.format("HitPolicy: %s has wrong output variable type", getHitPolicyName()));
        }
    }

    @Override
    public void composeDecisionTableOutput(Map<Integer, List<RuleOutputClauseContainer>> validRuleOutputEntries, final MvelExecutionContext executionContext) {
        if (executionContext.getResultValues() != null && !executionContext.getResultValues().isEmpty()) {

            // create comparable list of compound output values per rule
            List<Map> ruleOutputValues = new ArrayList<>();
            boolean outputValuesPresent = false;
            for (Map.Entry<Integer, Object> entry : executionContext.getResultValues().entrySet()) {
                if (executionContext.getOutputValues().containsKey(entry.getKey()) && !executionContext.getOutputValues().get(entry.getKey()).isEmpty()) {
                    outputValuesPresent = true;
                }

                if (entry.getValue() instanceof List) {
                    int ruleCounter = 0;
                    for (Object outputVariable : (List) entry.getValue()) {
                        Map<Integer, Comparable> ruleOutputNumberVariableValue;
                        try {
                            ruleOutputNumberVariableValue = ruleOutputValues.get(ruleCounter);
                            ruleOutputNumberVariableValue.put(entry.getKey(), (Comparable) outputVariable);
                        } catch (IndexOutOfBoundsException iobe) {
                            ruleOutputNumberVariableValue = new HashMap<>();
                            ruleOutputNumberVariableValue.put(entry.getKey(), (Comparable) outputVariable);
                            ruleOutputValues.add(ruleCounter, ruleOutputNumberVariableValue);
                        }
                        ruleCounter++;
                    }
                } else {
                    throw new FlowableException(String.format("HitPolicy: %s has wrong output variable type", getHitPolicyName()));
                }
            }

            if (Context.getDmnEngineConfiguration().isStrictMode() && !outputValuesPresent) {
                throw new FlowableException(String.format("HitPolicy: %s; no output values present", getHitPolicyName()));
            }

            // sort on predefined list(s) of output values
            Collections.sort(ruleOutputValues, new Comparator() {
                public int compare(Object o1, Object o2) {
                    CompareToBuilder compareToBuilder = new CompareToBuilder();
                    for (Map.Entry<Integer, List<Object>> entry : executionContext.getOutputValues().entrySet()) {
                        List<Object> outputValues = entry.getValue();
                        compareToBuilder.append(((Map) o1).get(entry.getKey()), ((Map) o2).get(entry.getKey()), new OutputOrderComparator<>(outputValues.toArray(new Comparable[outputValues.size()])));
                    }
                    return compareToBuilder.toComparison();
                }
            });

            // reset result variables with sorted values per variable
            executionContext.getResultVariables().clear();
            for (Map<Integer, Object> ruleOutputValue : ruleOutputValues) {
                for (Map.Entry<Integer, Object> entry : ruleOutputValue.entrySet()) {
                    List<Object> outputValues;
                    String variableId = executionContext.getVariableId(entry.getKey());

                    if (!executionContext.getResultVariables().containsKey(variableId) || executionContext.getResultVariables().get(variableId) instanceof List == false) {
                        outputValues = new ArrayList<>();
                        executionContext.getResultVariables().put(variableId, outputValues);
                    } else {
                        outputValues = (List) executionContext.getResultVariables().get(variableId);
                    }
                    outputValues.add(entry.getValue());
                }
            }
        }
    }
}
