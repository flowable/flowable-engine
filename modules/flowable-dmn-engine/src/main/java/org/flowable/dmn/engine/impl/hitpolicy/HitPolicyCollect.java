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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flowable.dmn.engine.impl.el.ELExecutionContext;
import org.flowable.dmn.model.BuiltinAggregator;
import org.flowable.dmn.model.HitPolicy;

/**
 * @author Yvo Swillens
 * @author martin.grofcik
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
            } else {
                Entry<String, List<Double>> outputValuesEntry = composeOutputValues(executionContext);
                if (executionContext.getAggregator() == BuiltinAggregator.SUM) {
                    if (outputValuesEntry != null) {
                        Double sumResult = aggregateSum(outputValuesEntry.getValue());
                        decisionResults.add(createDecisionResults(outputValuesEntry.getKey(), sumResult));
                    }
                } else if (executionContext.getAggregator() == BuiltinAggregator.MIN) {
                    if (outputValuesEntry != null) {
                        Double minResult = aggregateMin(outputValuesEntry.getValue());
                        decisionResults.add(createDecisionResults(outputValuesEntry.getKey(), minResult));
                    }
                } else if (executionContext.getAggregator() == BuiltinAggregator.MAX) {
                    if (outputValuesEntry != null) {
                        Double maxResult = aggregateMax(outputValuesEntry.getValue());
                        decisionResults.add(createDecisionResults(outputValuesEntry.getKey(), maxResult));
                    }
                } else if (executionContext.getAggregator() == BuiltinAggregator.COUNT) {
                    if (outputValuesEntry != null) {
                        Double countResult = aggregateCount(outputValuesEntry.getValue());
                        decisionResults.add(createDecisionResults(outputValuesEntry.getKey(), countResult));
                    }
                }
            }
        }
        executionContext.getAuditContainer().setDecisionResult(decisionResults);
        // the `multipleResults` flag depends on the aggregator. If there is no aggregation there are more results.
        executionContext.getAuditContainer().setMultipleResults(isMultipleResults(executionContext.getAggregator()));
    }

    protected boolean isMultipleResults(BuiltinAggregator aggregator) {
        return aggregator == null;
    }

    protected Entry<String, List<Double>> composeOutputValues(ELExecutionContext executionContext) {
        Collection<Map<String, Object>> ruleResults = new ArrayList<>(executionContext.getRuleResults().values());

        if (executionContext.isForceDMN11()) {
            // create distinct rule results
            ruleResults = new HashSet<>(ruleResults);
        }

        return createOutputDoubleValues(ruleResults);
    }

    protected Entry<String, List<Double>> createOutputDoubleValues(Collection<Map<String, Object>> ruleResults) {
        Map<String, List<Double>> distinctOutputDoubleValues = new HashMap<>();

        for (Map<String, Object> ruleResult : ruleResults) {
            for (Entry<String, Object> entry : ruleResult.entrySet()) {
                if (distinctOutputDoubleValues.containsKey(entry.getKey()) && distinctOutputDoubleValues.get(entry.getKey()) != null) {
                    distinctOutputDoubleValues.get(entry.getKey()).add((Double) entry.getValue());
                } else {
                    List<Double> valuesList = new ArrayList<>();
                    valuesList.add((Double) entry.getValue());
                    distinctOutputDoubleValues.put(entry.getKey(), valuesList);
                }
            }
        }

        // get first entry
        Entry<String, List<Double>> firstEntry = null;
        if (!distinctOutputDoubleValues.isEmpty()) {
            firstEntry = distinctOutputDoubleValues.entrySet().iterator().next();
        }

        return firstEntry;
    }

    protected Double aggregateSum(List<Double> values) {
        double aggregate = 0;
        for (Double value : values) {
            aggregate += value;
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
