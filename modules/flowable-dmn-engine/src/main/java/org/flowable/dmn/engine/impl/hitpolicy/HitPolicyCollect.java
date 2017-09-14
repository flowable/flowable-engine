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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.dmn.engine.impl.el.ELExecutionContext;
import org.flowable.dmn.model.BuiltinAggregator;
import org.flowable.dmn.model.HitPolicy;

/**
 * @author Yvo Swillens
 */
public class HitPolicyCollect extends AbstractHitPolicy implements ComposeDecisionResultBehavior {

    @Override
    public String getHitPolicyName() {
        return HitPolicy.COLLECT.getValue();
    }

    @Override
    public void composeDecisionResults(ELExecutionContext executionContext) {
        List<Map<String, Object>> decisionResults = new ArrayList<>();
        if (executionContext.getRuleResults() != null && !executionContext.getRuleResults().isEmpty()) {
            if (executionContext.getAggregator() == null) {
                decisionResults = new ArrayList<>(executionContext.getRuleResults().values());
                
            } else if (executionContext.getAggregator() == BuiltinAggregator.SUM) {
                Map.Entry<String, List<Double>> distinctOutputValuesEntry = createDistinctOutputDoubleValues(executionContext);
                if (distinctOutputValuesEntry != null) {
                    Double sumResult = aggregateSum(distinctOutputValuesEntry.getValue());
                    decisionResults.add(createDecisionResults(distinctOutputValuesEntry.getKey(), sumResult));
                }
                
            } else if (executionContext.getAggregator() == BuiltinAggregator.MIN) {
                Map.Entry<String, List<Double>> distinctOutputValuesEntry = createDistinctOutputDoubleValues(executionContext);
                if (distinctOutputValuesEntry != null) {
                    Double minResult = aggregateMin(distinctOutputValuesEntry.getValue());
                    decisionResults.add(createDecisionResults(distinctOutputValuesEntry.getKey(), minResult));
                }
                
            } else if (executionContext.getAggregator() == BuiltinAggregator.MAX) {
                Map.Entry<String, List<Double>> distinctOutputValuesEntry = createDistinctOutputDoubleValues(executionContext);
                if (distinctOutputValuesEntry != null) {
                    Double maxResult = aggregateMax(distinctOutputValuesEntry.getValue());
                    decisionResults.add(createDecisionResults(distinctOutputValuesEntry.getKey(), maxResult));
                }
                
            } else if (executionContext.getAggregator() == BuiltinAggregator.COUNT) {
                Map.Entry<String, List<Double>> distinctOutputValuesEntry = createDistinctOutputDoubleValues(executionContext);
                if (distinctOutputValuesEntry != null) {
                    Double countResult = aggregateCount(distinctOutputValuesEntry.getValue());
                    decisionResults.add(createDecisionResults(distinctOutputValuesEntry.getKey(), countResult));
                }
            }
        }
        executionContext.getAuditContainer().setDecisionResult(decisionResults);
    }

    protected Map.Entry<String, List<Double>> createDistinctOutputDoubleValues(ELExecutionContext executionContext) {
        List<Map<String, Object>> ruleResults = new ArrayList<>(executionContext.getRuleResults().values());
        Set<Map<String, Object>> distinctRuleResults = new HashSet<>(ruleResults);
        Map<String, List<Double>> distinctOutputDoubleValues = new HashMap<>();

        for (Map<String, Object> ruleResult : distinctRuleResults) {
           for (Map.Entry<String, Object> entry : ruleResult.entrySet()) {
               if (distinctOutputDoubleValues.containsKey(entry.getKey()) && distinctOutputDoubleValues.get(entry.getKey()) instanceof List) {
                   distinctOutputDoubleValues.get(entry.getKey()).add((Double) entry.getValue());
               } else {
                   List<Double> valuesList = new ArrayList<>();
                   valuesList.add((Double) entry.getValue());
                   distinctOutputDoubleValues.put(entry.getKey(), valuesList);
               }
           }
        }

        // get first entry
        Map.Entry<String, List<Double>> firstEntry = null;
        if (!distinctOutputDoubleValues.isEmpty()) {
            firstEntry = distinctOutputDoubleValues.entrySet().iterator().next();
        }

        return firstEntry;
    }

    protected Double aggregateSum(List<Double> values) {
        double aggregate = 0;
        for (Object value : values) {
            aggregate += (Double) value;
        }
        return aggregate;
    }

    protected Double aggregateMin(List<Double> values) {
        return Collections.min(values);
    }

    protected Double aggregateMax(List<Double> values) {
        return Collections.max(values);
    }

    protected Double aggregateCount(List<Double> values) {
        return (double) values.size();
    }

    protected Map<String, Object> createDecisionResults(String outputName, Double outputValue) {
        Map<String, Object> ruleResult = new HashMap<>();
        ruleResult.put(outputName, outputValue);
        return ruleResult;
    }
}
